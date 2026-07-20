package com.seufintech.conversor2.network

import com.seufintech.conversor2.data.CurrencyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApiService {

    @GET("json/last/{moedas}")
    suspend fun getLatestExchangeRates(@Path("moedas") currencies: String): Response<CurrencyResponse>
}