package com.redbull.lab.domain.model

/** Product model exposed by the SDK, independent from any HTTP payload. */
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val thumbnail: String,
)
