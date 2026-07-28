package com.fancia.backend.common.tag.mapper

import com.fancia.backend.shared.common.tag.core.dto.TagItemRequest
import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.entity.Tag

fun Tag.toDto(): TagResponse =
    TagResponse(
        id = id,
        name = name,
        type = type,
        createdBy = createdBy,
        createdAt = createdAt,
    )

fun TagItemRequest.toEntity(): Tag =
    Tag(name = name, type = type)

fun TagResponse.toEntity(): Tag =
    Tag(name = name, type = type).apply {
        id = this@toEntity.id
    }
