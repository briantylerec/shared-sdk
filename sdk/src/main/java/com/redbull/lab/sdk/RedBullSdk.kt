package com.redbull.lab.sdk

import com.redbull.lab.data.di.RemoteDataFactory
import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.usecase.GetProductsUseCase

/** Public entry point for applications consuming the shared Android platform SDK. */
class RedBullSdk private constructor(private val getProductsUseCase: GetProductsUseCase) {
    /** Returns normalized products from the configured API. */
    suspend fun getProducts(limit: Int = 20): List<Product> = getProductsUseCase(limit)

    companion object {
        /** Creates an SDK instance. The base URL must end with a slash. */
        @JvmStatic
        fun create(baseUrl: String = "https://dummyjson.com/"): RedBullSdk {
            require(baseUrl.startsWith("https://")) { "The SDK requires HTTPS" }
            return RedBullSdk(GetProductsUseCase(RemoteDataFactory.createProductRepository(baseUrl)))
        }
    }
}
