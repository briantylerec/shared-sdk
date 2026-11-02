package com.redbull.lab.data.mapper

import com.redbull.lab.data.remote.dto.ProductDto
import com.redbull.lab.domain.model.Product

/** Converts the external wire model into the stable domain model. */
internal fun ProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price,
    thumbnail = thumbnail,
)
