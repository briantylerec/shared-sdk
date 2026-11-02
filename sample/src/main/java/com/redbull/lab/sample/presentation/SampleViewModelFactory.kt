package com.redbull.lab.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase

/** Creates the sample ViewModel from the app's explicit dependency graph. */
class SampleViewModelFactory(private val loadProducts: LoadProductsUseCase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SampleViewModel::class.java))
        return SampleViewModel(loadProducts) as T
    }
}
