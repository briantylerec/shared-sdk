package com.redbull.lab.domain.repository

import com.redbull.lab.domain.model.Product

/** Contract for loading products without exposing the network implementation. */
interface ProductRepository {
    suspend fun getProducts(limit: Int = 20): List<Product>
}
