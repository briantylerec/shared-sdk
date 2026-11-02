package com.redbull.lab.sample.presentation

import com.redbull.lab.domain.model.Product

/** Immutable screen state rendered by the sample Activity. */
data class SampleUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
