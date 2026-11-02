package com.redbull.lab.data.repository

import com.redbull.lab.data.mapper.toDomain
import com.redbull.lab.data.remote.api.ProductApi
import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository

/** Implements the domain repository using the Retrofit API. */
internal class RetrofitProductRepository(private val api: ProductApi) : ProductRepository {
    override suspend fun getProducts(limit: Int): List<Product> =
        api.getProducts(limit.coerceIn(1, 100)).products.map { it.toDomain() }
}
