package com.seufintech.conversor2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.seufintech.conversor2.data.CurrencyData
import com.seufintech.conversor2.databinding.ActivityConverterBinding
import com.seufintech.conversor2.network.RetrofitClient
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding

    private var saldoReal: Double = 0.0
    private var saldoDolar: Double = 0.0
    private var saldoBitcoin: Double = 0.0

    private val realDolarFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val bitcoinFormat = DecimalFormat("0.0000", DecimalFormatSymbols(Locale("pt", "BR")))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar saldos da MainActivity
        saldoReal = intent.getDoubleExtra("SALDO_REAL", 0.0)
        saldoDolar = intent.getDoubleExtra("SALDO_DOLAR", 0.0)
        saldoBitcoin = intent.getDoubleExtra("SALDO_BITCOIN", 0.0)

        setupSpinners()
        setupConvertButton()
    }

    private fun setupSpinners() {
        val currencies = arrayOf("R$", "$", "BTC")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerOrigem.adapter = adapter
        binding.spinnerDestino.adapter = adapter
    }

    private fun setupConvertButton() {
        binding.btnRealizarConversao.setOnClickListener {
            performConversion()
        }
    }

    private fun performConversion() {
        // Obter valores da UI
        val moedaOrigem = binding.spinnerOrigem.selectedItem.toString()
        val moedaDestino = binding.spinnerDestino.selectedItem.toString()
        val valorString = binding.etValor.text.toString().replace(',', '.') // Aceita vírgula como separador

        // --- Início das Validações ---
        if (valorString.isBlank()) {
            showToast("Por favor, insira o valor a ser convertido.")
            return
        }

        val valorAConverter = valorString.toDoubleOrNull()
        if (valorAConverter == null || valorAConverter <= 0) {
            showToast("Valor inválido. Insira um número positivo.")
            return
        }

        val saldoDisponivel = when (moedaOrigem) {
            "R$" -> saldoReal
            "$" -> saldoDolar
            "BTC" -> saldoBitcoin
            else -> 0.0
        }

        if (valorAConverter > saldoDisponivel) {
            showToast("Saldo insuficiente na moeda de origem.")
            return
        }

        if (moedaOrigem == moedaDestino) {
            showToast("Selecione moedas de origem e destino diferentes.")
            return
        }

        val apiCurrencyPair = getApiCurrencyPair(moedaOrigem, moedaDestino)
        if (apiCurrencyPair == null) {
            showToast("Combinação de moedas não suportada.")
            return
        }
        // --- Fim das Validações ---

        // Iniciar Coroutine para chamada de rede
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getLatestExchangeRates(apiCurrencyPair)
                if (response.isSuccessful) {
                    val currencyData = extractCurrencyData(response.body(), apiCurrencyPair)
                    if (currencyData != null) {
                        processConversion(currencyData, valorAConverter, moedaOrigem, moedaDestino)
                    } else {
                        showToastOnMainThread("Erro ao processar dados da API.")
                    }
                } else {
                    showToastOnMainThread("Erro na API: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                showToastOnMainThread("Erro de conexão: ${e.message}")
                e.printStackTrace()
            } finally {
                showLoadingOnMainThread(false)
            }
        }
    }

    private fun extractCurrencyData(responseBody: com.seufintech.conversor2.data.CurrencyResponse?, pair: String): CurrencyData? {
        return when (pair) {
            "USD-BRL" -> responseBody?.usdbrl
            "BRL-USD" -> responseBody?.brlusd
            "BTC-BRL" -> responseBody?.btcbrl
            "BRL-BTC" -> responseBody?.brlbtc
            "BTC-USD" -> responseBody?.btcusd
            "USD-BTC" -> responseBody?.usdbtc
            else -> null
        }
    }

    private suspend fun processConversion(currencyData: CurrencyData, valorAConverter: Double, moedaOrigem: String, moedaDestino: String) {
        val taxaDeConversao = currencyData.bid.toDoubleOrNull()
        if (taxaDeConversao == null) {
            showToastOnMainThread("Cotação inválida recebida da API.")
            return
        }

        val valorConvertido = valorAConverter * taxaDeConversao

        // Atualiza os saldos locais
        when (moedaOrigem) {
            "R$" -> saldoReal -= valorAConverter
            "$" -> saldoDolar -= valorAConverter
            "BTC" -> saldoBitcoin -= valorAConverter
        }
        when (moedaDestino) {
            "R$" -> saldoReal += valorConvertido
            "$" -> saldoDolar += valorConvertido
            "BTC" -> saldoBitcoin += valorConvertido
        }

        // Prepara para exibir o resultado na UI Thread
        withContext(Dispatchers.Main) {
            val formattedValue = when (moedaDestino) {
                "R$", "$" -> realDolarFormat.format(valorConvertido)
                "BTC" -> bitcoinFormat.format(valorConvertido)
                else -> "N/A"
            }
            binding.tvValorConvertido.text = formattedValue
            binding.tvResultadoLabel.visibility = View.VISIBLE
            binding.tvValorConvertido.visibility = View.VISIBLE
            showToast("Conversão realizada com sucesso!")

            // Prepara o Intent para retornar os dados atualizados
            val resultIntent = Intent().apply {
                putExtra("NOVO_SALDO_REAL", saldoReal)
                putExtra("NOVO_SALDO_DOLAR", saldoDolar)
                putExtra("NOVO_SALDO_BITCOIN", saldoBitcoin)
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
    }

    private fun getApiCurrencyPair(origem: String, destino: String): String? {
        return when {
            origem == "R$" && destino == "$" -> "BRL-USD"
            origem == "$" && destino == "R$" -> "USD-BRL"
            origem == "R$" && destino == "BTC" -> "BRL-BTC"
            origem == "BTC" && destino == "R$" -> "BTC-BRL"
            origem == "$" && destino == "BTC" -> "USD-BTC"
            origem == "BTC" && destino == "$" -> "BTC-USD"
            else -> null
        }
    }

    // --- Funções Auxiliares para UI ---
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRealizarConversao.isEnabled = !isLoading
    }

    private suspend fun showLoadingOnMainThread(isLoading: Boolean) {
        withContext(Dispatchers.Main) {
            showLoading(isLoading)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private suspend fun showToastOnMainThread(message: String) {
        withContext(Dispatchers.Main) {
            showToast(message)
        }
    }

    override fun onBackPressed() {
        // Garante que, ao voltar, os saldos (mesmo que não alterados) sejam retornados
        val resultIntent = Intent().apply {
            putExtra("NOVO_SALDO_REAL", saldoReal)
            putExtra("NOVO_SALDO_DOLAR", saldoDolar)
            putExtra("NOVO_SALDO_BITCOIN", saldoBitcoin)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        super.onBackPressed()
    }
}