package com.fancia.backend.common.tag.core.service

import tools.jackson.core.type.TypeReference
import com.fancia.backend.shared.common.redis.CacheKeys
import com.fancia.backend.shared.common.redis.CachedPage
import com.fancia.backend.shared.common.redis.RedisQueryCache
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
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.*

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val redisQueryCache: ObjectProvider<RedisQueryCache>,
) {
    @Transactional
    fun create(@Valid request: CreateTagsRequest, pageable: Pageable): Page<TagResponse> {
        request.tags.forEach { tagItem ->
            if (!tagRepository.existsByNameAndType(tagItem.name, tagItem.type)) {
                tagRepository.save(tagItem.toEntity())
            }
        }
        invalidateTagCaches()
        val tags = resolveExactTags(request.tags)
        return PageImpl(tags.map { it.toDto() }, pageable, tags.size.toLong())
    }

    fun searchSimilar(
        search: List<String>,
        type: TagType,
        pageable: Pageable,
    ): Page<TagResponse> {
        val normalized = search.map { it.trim() }.filter { it.isNotEmpty() }.sorted()
        val cache = redisQueryCache.ifAvailable
        if (cache == null || normalized.isEmpty()) {
            return loadSimilar(normalized, type, pageable)
        }
        val key = "$TAG_SIMILAR_PREFIX${type.name}:${CacheKeys.hash(normalized)}:${pageable.pageNumber}:${pageable.pageSize}"
        val cached = cache.getOrLoad(
            key,
            SIMILAR_TTL,
            object : TypeReference<CachedPage<TagResponse>>() {},
        ) {
            CachedPage.from(loadSimilar(normalized, type, pageable))
        }
        return cached.toPage(pageable)
    }

    fun findByIds(ids: Set<UUID>): List<TagResponse> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val cache = redisQueryCache.ifAvailable ?: return tagRepository.findByIdIn(ids).map { it.toDto() }
        val key = "$TAG_BY_ID_PREFIX${CacheKeys.hash(ids)}"
        return cache.getOrLoad(
            key,
            BY_ID_TTL,
            object : TypeReference<List<TagResponse>>() {},
        ) {
            tagRepository.findByIdIn(ids).map { it.toDto() }
        }
    }

    fun deleteTagById(id: UUID): Tag {
        val tag = tagRepository.findById(id).orElseThrow { TagNotFoundException(id.toString()) }
        tagRepository.delete(tag)
        invalidateTagCaches()
        return tag
    }

    private fun loadSimilar(
        normalized: List<String>,
        type: TagType,
        pageable: Pageable,
    ): Page<TagResponse> {
        val tags = normalized
            .flatMap { name -> tagRepository.findSimilarByNameAndType(name, type) }
            .distinctBy { it.id }
            .map { it.toDto() }
        return PageImpl(tags, pageable, tags.size.toLong())
    }

    private fun resolveExactTags(requests: List<TagItemRequest>): List<Tag> =
        requests
            .mapNotNull { request ->
                if (request.name.isBlank()) null
                else tagRepository.findByNameAndType(request.name, request.type)
            }
            .distinctBy { it.id }

    private fun invalidateTagCaches() {
        redisQueryCache.ifAvailable?.evictByPrefix(TAG_PREFIX)
    }

    companion object {
        private const val TAG_PREFIX = "common:tag:"
        private const val TAG_BY_ID_PREFIX = "${TAG_PREFIX}ids:"
        private const val TAG_SIMILAR_PREFIX = "${TAG_PREFIX}similar:"
        private val BY_ID_TTL = Duration.ofHours(3)
        private val SIMILAR_TTL = Duration.ofMinutes(30)
    }
}
