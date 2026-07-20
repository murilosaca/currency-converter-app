package com.seufintech.conversor2

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.seufintech.conversor2.ConverterActivity
import com.seufintech.conversor2.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Saldos iniciais (não persistentes)
    private var saldoReal: Double = 100000.00
    private var saldoDolar: Double = 50000.00
    private var saldoBitcoin: Double = 0.5000

    // Formatos para exibir os valores com a formatação correta
    private val realDolarFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val bitcoinFormat = DecimalFormat("0.0000", DecimalFormatSymbols(Locale("pt", "BR")))

    // API moderna para receber o resultado da ConverterActivity
    private val startConverterActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                // Atualiza os saldos com os valores retornados
                saldoReal = intent.getDoubleExtra("NOVO_SALDO_REAL", saldoReal)
                saldoDolar = intent.getDoubleExtra("NOVO_SALDO_DOLAR", saldoDolar)
                saldoBitcoin = intent.getDoubleExtra("NOVO_SALDO_BITCOIN", saldoBitcoin)
                // Exibe os novos saldos na tela
                updateBalancesOnScreen()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateBalancesOnScreen()

        binding.btnConvert.setOnClickListener {
            val intent = Intent(this, ConverterActivity::class.java).apply {
                // Passa os saldos atuais para a próxima tela
                putExtra("SALDO_REAL", saldoReal)
                putExtra("SALDO_DOLAR", saldoDolar)
                putExtra("SALDO_BITCOIN", saldoBitcoin)
            }
            startConverterActivity.launch(intent)
        }
    }

    private fun updateBalancesOnScreen() {
        binding.tvRealBalance.text = realDolarFormat.format(saldoReal)
        binding.tvDolarBalance.text = realDolarFormat.format(saldoDolar)
        binding.tvBitcoinBalance.text = bitcoinFormat.format(saldoBitcoin)
    }
}