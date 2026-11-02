package com.redbull.lab.domain.usecase

import com.redbull.lab.domain.model.Product
import com.redbull.lab.domain.repository.ProductRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProductsUseCaseTest {
    @Test
    fun returnsProductsFromRepository() = runBlocking {
        val expected = listOf(Product(1, "Sample", "Demo", 4.5, "image"))
        val repository = object : ProductRepository {
            override suspend fun getProducts(limit: Int) = expected
        }

        assertEquals(expected, GetProductsUseCase(repository)(10))
    }
}
