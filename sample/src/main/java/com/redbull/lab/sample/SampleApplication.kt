package com.redbull.lab.sample

import android.app.Application
import com.redbull.lab.sample.data.SdkSampleProductRepository
import com.redbull.lab.sample.domain.usecase.LoadProductsUseCase
import com.redbull.lab.sample.presentation.SampleViewModelFactory
import com.redbull.lab.sdk.RedBullSdk

/** Composition root for the sample app's manual dependency graph. */
class SampleApplication : Application() {
    val viewModelFactory: SampleViewModelFactory by lazy {
        val sdk = RedBullSdk.create()
        val repository = SdkSampleProductRepository(sdk)
        SampleViewModelFactory(LoadProductsUseCase(repository))
    }
}
