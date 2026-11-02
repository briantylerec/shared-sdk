package com.redbull.lab.data.remote.dto

/** Wire-format product received from DummyJSON. */
data class ProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val thumbnail: String,
)
