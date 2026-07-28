package com.fancia.backend.common.tag.core.service

import com.fancia.backend.common.tag.core.repository.TagRepository
import com.fancia.backend.common.tag.mapper.toDto
import com.fancia.backend.common.tag.mapper.toEntity
import com.fancia.backend.shared.common.tag.core.dto.CreateTagsRequest
import com.fancia.backend.shared.common.tag.core.dto.TagItemRequest
import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.entity.Tag
import com.fancia.backend.shared.common.tag.core.enums.TagType
import com.fancia.backend.shared.common.tag.core.exception.TagNotFoundException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TagService(
    private val tagRepository: TagRepository,
) {
    @Transactional
    fun create(@Valid request: CreateTagsRequest, pageable: Pageable): Page<TagResponse> {
        request.tags.forEach { tagItem ->
            if (!tagRepository.existsByNameAndType(tagItem.name, tagItem.type)) {
                tagRepository.save(tagItem.toEntity())
            }
        }
        val tags = resolveExactTags(request.tags)
        return PageImpl(tags.map { it.toDto() }, pageable, tags.size.toLong())
    }

    fun searchSimilar(
        search: List<String>,
        type: TagType,
        pageable: Pageable,
    ): Page<TagResponse> {
        val tags = search
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { name -> tagRepository.findSimilarByNameAndType(name, type) }
            .distinctBy { it.id }
            .map { it.toDto() }
        return PageImpl(tags, pageable, tags.size.toLong())
    }

    fun findByIds(ids: Set<UUID>): List<TagResponse> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return tagRepository.findByIdIn(ids).map { it.toDto() }
    }

    fun deleteTagById(id: UUID): Tag {
        val tag = tagRepository.findById(id).orElseThrow { TagNotFoundException(id.toString()) }
        tagRepository.delete(tag)
        return tag
    }

    private fun resolveExactTags(requests: List<TagItemRequest>): List<Tag> =
        requests
            .mapNotNull { request ->
                if (request.name.isBlank()) null
                else tagRepository.findByNameAndType(request.name, request.type)
            }
            .distinctBy { it.id }
}
