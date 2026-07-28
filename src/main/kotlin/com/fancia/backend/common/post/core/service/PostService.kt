package com.fancia.backend.common.post.core.service

import com.fancia.backend.common.post.core.entity.Post
import com.fancia.backend.common.post.core.entity.PostLike
import com.fancia.backend.common.post.core.entity.PostLikeId
import com.fancia.backend.common.post.core.entity.PostMedia
import com.fancia.backend.common.post.core.repository.PostLikeRepository
import com.fancia.backend.common.post.core.repository.PostRepository
import com.fancia.backend.common.post.mapper.toDto
import com.fancia.backend.common.post.mapper.toEntity
import com.fancia.backend.shared.common.core.exception.InvalidAuthenticationException
import com.fancia.backend.shared.common.post.core.dto.CreatePostRequest
import com.fancia.backend.shared.common.post.core.dto.PostMediaItem
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import com.fancia.backend.shared.common.post.core.dto.UpdatePostRequest
import com.fancia.backend.shared.common.post.core.exception.FeaturedPostRequiresMediaException
import com.fancia.backend.shared.common.post.core.exception.PostContentRequiredException
import com.fancia.backend.shared.common.post.core.exception.PostMediaLimitExceededException
import com.fancia.backend.shared.common.post.core.exception.PostNotFoundException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    companion object {
        const val MAX_MEDIA = 10
    }

    @Transactional
    fun create(@Valid request: CreatePostRequest, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val authorUserId = request.authorUserId
        if (authorUserId != currentUserId) {
            throw InvalidAuthenticationException()
        }
        validateContent(request.body, request.media)
        val post = request.toEntity()
        attachMedia(post, request.media)
        applyFeatured(post, request.featured)
        return postRepository.save(post).toDto(currentUserId)
    }

    @Transactional
    fun update(postId: UUID, @Valid request: UpdatePostRequest, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        if (post.authorUserId != currentUserId) {
            throw InvalidAuthenticationException()
        }
        validateContent(request.body, request.media)
        post.body = request.body
        post.media.clear()
        attachMedia(post, request.media)
        post.pinned = request.pinned
        applyFeatured(post, request.featured)
        return postRepository.save(post).toDto(currentUserId)
    }

    @Transactional
    fun like(postId: UUID, jwt: Jwt) {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        if (postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId)) {
            return
        }
        post.likes.add(
            PostLike(PostLikeId(postId, currentUserId)).apply { this.post = post }
        )
        postRepository.save(post)
    }

    @Transactional
    fun unlike(postId: UUID, jwt: Jwt) {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        post.likes.removeIf { it.id.userId == currentUserId }
        postRepository.save(post)
    }

    fun listByTargetId(targetId: UUID, pageable: Pageable, jwt: Jwt?): Page<PostResponse> {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        return postRepository.findByTargetIdOrderByPinnedFirst(targetId, pageable)
            .map { it.toDto(currentUserId) }
    }

    fun requirePostOnTarget(postId: UUID, targetId: UUID, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        if (post.targetId != targetId) {
            throw PostNotFoundException(postId)
        }
        return post.toDto(currentUserId)
    }

    private fun requireUserId(jwt: Jwt): UUID =
        jwt.getClaimAsString("userId")?.let { UUID.fromString(it) }
            ?: throw InvalidAuthenticationException()

    private fun attachMedia(post: Post, media: List<PostMediaItem>) {
        media.forEachIndexed { index, item ->
            post.media.add(
                PostMedia(
                    post = post,
                    objectKey = item.objectKey,
                    mediaType = item.mediaType,
                    sortOrder = index,
                )
            )
        }
    }

    private fun applyFeatured(post: Post, featured: Boolean) {
        if (featured) {
            if (post.media.isEmpty()) {
                throw FeaturedPostRequiresMediaException()
            }
            postRepository.clearFeaturedByTargetId(post.targetId)
            post.featured = true
        } else {
            post.featured = false
        }
    }

    private fun validateContent(body: String?, media: List<*>) {
        val hasBody = !body.isNullOrBlank()
        val hasMedia = media.isNotEmpty()
        if (!hasBody && !hasMedia) {
            throw PostContentRequiredException()
        }
        if (media.size > MAX_MEDIA) {
            throw PostMediaLimitExceededException(MAX_MEDIA)
        }
    }
}
