package com.example.healthandwellbeingapp.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response

interface FatSecretApiService {
    @GET("rest/server.api")
    suspend fun searchFood(
        @Query("method") method: String = "foods.search",
        @Query("format") format: String = "json",
        @Query("search_expression") query: String,
        @Query("oauth_consumer_key") apiKey: String,
        @Query("oauth_signature_method") signatureMethod: String = "HMAC-SHA1",
        @Query("oauth_timestamp") timestamp: String,
        @Query("oauth_nonce") nonce: String,
        @Query("oauth_version") oauthVersion: String = "1.0",
        @Query("oauth_signature") signature: String
    ): Response<FoodSearchResponse>
}
