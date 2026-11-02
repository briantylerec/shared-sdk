package com.redbull.lab.data.remote.dto

/** Envelope returned by the DummyJSON products endpoint. */
data class ProductResponseDto(val products: List<ProductDto> = emptyList())
