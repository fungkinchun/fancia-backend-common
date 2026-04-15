package com.fancia.backend.common.mapper

import com.fancia.backend.common.core.entity.Tag
import com.fancia.backend.shared.common.core.dto.SingleTagCreation
import com.fancia.backend.shared.common.core.dto.TagResponse
import org.mapstruct.Mapper
import org.mapstruct.NullValueMappingStrategy
import org.mapstruct.ReportingPolicy

@Mapper(
    componentModel = "spring",
    nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
    nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
interface TagMapper {
    fun toDto(tag: Tag): TagResponse
    fun toBean(request: SingleTagCreation): Tag
    fun toBean(response: TagResponse): Tag
}