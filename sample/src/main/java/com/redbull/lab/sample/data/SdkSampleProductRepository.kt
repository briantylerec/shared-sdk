package com.redbull.lab.sample.data

import com.redbull.lab.domain.model.Product
import com.redbull.lab.sample.domain.repository.SampleProductRepository
import com.redbull.lab.sdk.RedBullSdk

/** Adapts the published SDK facade to the sample app's domain interface. */
class SdkSampleProductRepository(private val sdk: RedBullSdk) : SampleProductRepository {
    override suspend fun loadProducts(): List<Product> = sdk.getProducts()
}
