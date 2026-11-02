package com.redbull.lab.data.mapper

import com.redbull.lab.data.remote.dto.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMapperTest {
    @Test
    fun mapsRemoteDtoToDomainModel() {
        val product = ProductDto(7, "Demo", "Description", 12.0, "thumb").toDomain()

        assertEquals(7, product.id)
        assertEquals("Demo", product.title)
        assertEquals(12.0, product.price, 0.0)
    }
}
