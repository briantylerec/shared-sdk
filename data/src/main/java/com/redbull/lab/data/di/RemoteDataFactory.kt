package com.redbull.lab.data.di

import com.redbull.lab.data.remote.api.ProductApi
import com.redbull.lab.data.repository.RetrofitProductRepository
import com.redbull.lab.domain.repository.ProductRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Constructs the remote data graph across Gradle modules without forcing consumers to depend on DI frameworks. This wiring API is not part of the supported SDK facade. */
object RemoteDataFactory {
    fun createProductRepository(baseUrl: String): ProductRepository {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ProductApi::class.java)
        return RetrofitProductRepository(api)
    }
}
