package com.redbull.lab.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository

/** Retrieves products through the domain repository contract. */
class GetProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(limit: Int = 20): List<Product> = repository.getProducts(limit)
}
