package com.seufintech.conversor2.data

import com.google.gson.annotations.SerializedName

data class CurrencyResponse(
    @SerializedName("USDBRL") val usdbrl: CurrencyData?,
    @SerializedName("BRLUSD") val brlusd: CurrencyData?,
    @SerializedName("BTCBRL") val btcbrl: CurrencyData?,
    @SerializedName("BRLBTC") val brlbtc: CurrencyData?,
    @SerializedName("BTCUSD") val btcusd: CurrencyData?,
    @SerializedName("USDBTC") val usdbtc: CurrencyData?
)

data class CurrencyData(
    val code: String,
    val codein: String,
    val name: String,
    val high: String,
    val low: String,
    val varBid: String,
    val pctChange: String,
    val bid: String, // Preço de compra que usaremos para a conversão
    val ask: String,
    val timestamp: String,
    @SerializedName("create_date") val createDate: String
)