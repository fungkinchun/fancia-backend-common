package com.fancia.backend.common.core.service

import com.fancia.backend.common.core.entity.Tag
import com.fancia.backend.common.core.repository.TagRepository
import com.fancia.backend.common.mapper.TagMapper
import com.fancia.backend.shared.common.core.dto.CreateTagsRequest
import com.fancia.backend.shared.common.core.dto.SingleTagCreation
import com.fancia.backend.shared.common.core.dto.TagResponse
import com.fancia.backend.shared.common.core.exception.TagNotFoundException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val tagMapper: TagMapper,
) {
    @Transactional
    fun create(@Valid request: CreateTagsRequest, pageable: Pageable): Page<TagResponse> {
        request.tags.forEach { it ->
            val existing = tagRepository.existsByName(it.name)
            if (!existing) {
                tagRepository.save(tagMapper.toBean(it))
            }
        }
        val tags = tags(request.tags.map { it.name })
        return PageImpl(
            tags
                .map(tagMapper::toDto), pageable, tags.size.toLong()
        )
    }

    private fun tags(search: List<String>): MutableList<Tag> {
        val tags: MutableList<Tag> = mutableListOf()
        search.forEach { it ->
            if (it.isNotBlank()) {
                tags.addAll(tagRepository.findAll(it))
            }
        }
        return tags
    }

    fun findAll(
        search: List<String>,
        pageable: Pageable
    ): Page<TagResponse> {
        val notFound = search.filter { s ->
            tags(listOf(s)).isEmpty()
        }
        create(
            CreateTagsRequest(
                tags = notFound.map { SingleTagCreation(name = it) }
            ), pageable
        )
        val tags = tags(search).map(tagMapper::toDto)
        return PageImpl(tags, pageable, tags.size.toLong())
    }

    fun deleteTagByName(name: String) {
        val tag = tagRepository.findByName(name) ?: throw TagNotFoundException(name)
        tagRepository.delete(tag)
    }
}