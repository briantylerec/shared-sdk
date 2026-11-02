package com.redbull.lab.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads catalog data and exposes a lifecycle-safe UI state to the Activity. */
class SampleViewModel(private val loadProducts: LoadProductsUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = SampleUiState(isLoading = true)
            try {
                _uiState.value = SampleUiState(isLoading = false, products = loadProducts())
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = SampleUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load products.",
                )
            }
        }
    }
}
