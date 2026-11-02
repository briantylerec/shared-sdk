package com.redbull.lab.data.remote.api

import com.redbull.lab.data.remote.dto.ProductResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit contract for the external product endpoint. */
internal interface ProductApi {
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int): ProductResponseDto
}
