package com.redbull.lab.sample.domain.repository

import com.redbull.lab.domain.model.Product

/** Sample-app boundary for product loading, independent from the SDK implementation. */
interface SampleProductRepository {
    suspend fun loadProducts(): List<Product>
}
