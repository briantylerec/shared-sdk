package com.redbull.lab.sample.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.sample.domain.repository.SampleProductRepository

/** Requests the sample catalog through its own domain contract. */
class LoadProductsUseCase(private val repository: SampleProductRepository) {
    suspend operator fun invoke(): List<Product> = repository.loadProducts()
}
