package com.fancia.backend.common.post.core.service

import com.fancia.backend.common.post.core.entity.Post
import com.fancia.backend.common.post.core.entity.PostLike
import com.fancia.backend.common.post.core.entity.PostLikeId
import com.fancia.backend.common.post.core.entity.PostMedia
import com.fancia.backend.common.post.core.entity.PostPoll
import com.fancia.backend.common.post.core.entity.PostPollOption
import com.fancia.backend.common.post.core.entity.PostPollVote
import com.fancia.backend.common.post.core.entity.PostPollVoteId
import com.fancia.backend.common.post.core.repository.PostLikeRepository
import com.fancia.backend.common.post.core.repository.PostRepository
import com.fancia.backend.common.post.mapper.toDto
import com.fancia.backend.common.post.mapper.toEntity
import com.fancia.backend.shared.common.core.exception.InvalidAuthenticationException
import com.fancia.backend.shared.common.post.core.dto.CastPollVoteRequest
import com.fancia.backend.shared.common.post.core.dto.CreatePollRequest
import com.fancia.backend.shared.common.post.core.dto.CreatePostRequest
import com.fancia.backend.shared.common.post.core.dto.PostMediaItem
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import com.fancia.backend.shared.common.post.core.dto.UpdatePostRequest
import com.fancia.backend.shared.common.post.core.enums.PostKind
import com.fancia.backend.shared.common.post.core.enums.PostStatus
import com.fancia.backend.shared.common.post.core.exception.FeaturedPostRequiresMediaException
import com.fancia.backend.shared.common.post.core.exception.InvalidPollVoteException
import com.fancia.backend.shared.common.post.core.exception.PollClosedException
import com.fancia.backend.shared.common.post.core.exception.PollRequiredException
import com.fancia.backend.shared.common.post.core.exception.PostContentRequiredException
import com.fancia.backend.shared.common.post.core.exception.PostExpiredException
import com.fancia.backend.shared.common.post.core.exception.PostMediaLimitExceededException
import com.fancia.backend.shared.common.post.core.exception.PostNotFoundException
import com.fancia.backend.shared.common.post.core.exception.PostNotPollException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
) {
    companion object {
        const val MAX_MEDIA = 10
        const val MIN_POLL_OPTIONS = 2
        const val MAX_POLL_OPTIONS = 10
    }

    @Transactional
    fun create(@Valid request: CreatePostRequest, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val authorUserId = request.authorUserId
        if (authorUserId != currentUserId) {
            throw InvalidAuthenticationException()
        }
        validateContent(request.kind, request.body, request.media, request.poll)
        val post = request.toEntity()
        attachMedia(post, request.media)
        if (request.kind == PostKind.POLL) {
            attachPoll(post, request.poll!!)
        }
        applyStatus(post, request.status)
        return postRepository.save(post).toDto(currentUserId)
    }

    @Transactional
    fun update(postId: UUID, @Valid request: UpdatePostRequest, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        if (post.authorUserId != currentUserId) {
            throw InvalidAuthenticationException()
        }
        if (post.isExpired()) {
            if (request.status == null) {
                throw PostExpiredException()
            }
            applyStatus(post, request.status)
            return postRepository.save(post).toDto(currentUserId)
        }
        if (post.kind == PostKind.POLL) {
            val hasBody = !request.body.isNullOrBlank()
            if (!hasBody && request.media.isEmpty() && post.poll?.options.isNullOrEmpty()) {
                throw PostContentRequiredException()
            }
        } else {
            validateContent(PostKind.TEXT, request.body, request.media, null)
        }
        if (request.media.size > MAX_MEDIA) {
            throw PostMediaLimitExceededException(MAX_MEDIA)
        }
        post.body = request.body
        post.media.clear()
        attachMedia(post, request.media)
        if (request.expiredAt != null) {
            post.expiredAt = request.expiredAt
        }
        if (request.status != null) {
            applyStatus(post, request.status)
        }
        return postRepository.save(post).toDto(currentUserId)
    }

    @Transactional
    fun like(postId: UUID, jwt: Jwt) {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        requireMutable(post)
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
        requireMutable(post)
        post.likes.removeIf { it.id.userId == currentUserId }
        postRepository.save(post)
    }

    @Transactional
    fun vote(postId: UUID, @Valid request: CastPollVoteRequest, jwt: Jwt): PostResponse {
        val currentUserId = requireUserId(jwt)
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        if (post.kind != PostKind.POLL) {
            throw PostNotPollException()
        }
        requireMutable(post)
        if (post.status == PostStatus.READ_ONLY || post.status == PostStatus.HIDDEN) {
            throw PollClosedException()
        }
        val poll = post.poll ?: throw PostNotPollException()
        if (poll.isClosed()) {
            throw PollClosedException()
        }
        val optionIds = request.optionIds.distinct()
        if (optionIds.isEmpty()) {
            throw InvalidPollVoteException()
        }
        if (!poll.allowMultiple && optionIds.size != 1) {
            throw InvalidPollVoteException(
                message = "This poll allows only one option",
            )
        }
        if (poll.allowMultiple && optionIds.size > poll.options.size) {
            throw InvalidPollVoteException()
        }
        val validOptionIds = poll.options.mapNotNull { it.id }.toSet()
        if (!validOptionIds.containsAll(optionIds)) {
            throw InvalidPollVoteException()
        }
        poll.votes.removeIf { it.id.userId == currentUserId }
        optionIds.forEach { optionId ->
            val option = poll.options.first { it.id == optionId }
            poll.votes.add(
                PostPollVote(PostPollVoteId(postId, optionId, currentUserId)).apply {
                    this.poll = poll
                    this.option = option
                },
            )
        }
        return postRepository.save(post).toDto(currentUserId)
    }

    @Transactional(readOnly = true)
    fun listByTargetId(
        targetId: UUID,
        kind: PostKind?,
        openOnly: Boolean,
        pageable: Pageable,
        jwt: Jwt?,
    ): Page<PostResponse> {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        return postRepository.findByTargetIdFiltered(
            targetId = targetId,
            kind = kind,
            openOnly = openOnly,
            now = LocalDateTime.now(),
            pageable = pageable,
        ).map { it.toDto(currentUserId) }
    }

    @Transactional(readOnly = true)
    fun getById(postId: UUID, jwt: Jwt?): PostResponse {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        return post.toDto(currentUserId)
    }

    @Transactional(readOnly = true)
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

    private fun requireMutable(post: Post) {
        if (post.isExpired()) {
            throw PostExpiredException()
        }
    }

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

    private fun attachPoll(post: Post, request: CreatePollRequest) {
        val labels = request.options.map { it.trim() }.filter { it.isNotEmpty() }
        if (labels.size < MIN_POLL_OPTIONS || labels.size > MAX_POLL_OPTIONS) {
            throw PollRequiredException()
        }
        val poll = PostPoll(
            post = post,
            allowMultiple = request.allowMultiple,
            closesAt = request.closesAt,
        )
        labels.forEachIndexed { index, label ->
            poll.options.add(
                PostPollOption(
                    poll = poll,
                    label = label,
                    sortOrder = index,
                ),
            )
        }
        post.poll = poll
    }

    private fun applyStatus(post: Post, status: PostStatus) {
        if (status == PostStatus.FEATURED) {
            if (post.media.isEmpty()) {
                throw FeaturedPostRequiresMediaException()
            }
            postRepository.clearFeaturedByTargetId(post.targetId)
        }
        post.status = status
    }

    private fun validateContent(
        kind: PostKind,
        body: String?,
        media: List<*>,
        poll: CreatePollRequest?,
    ) {
        if (media.size > MAX_MEDIA) {
            throw PostMediaLimitExceededException(MAX_MEDIA)
        }
        when (kind) {
            PostKind.TEXT -> {
                val hasBody = !body.isNullOrBlank()
                val hasMedia = media.isNotEmpty()
                if (!hasBody && !hasMedia) {
                    throw PostContentRequiredException()
                }
            }
            PostKind.POLL -> {
                val labels = poll?.options?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                if (labels.size < MIN_POLL_OPTIONS || labels.size > MAX_POLL_OPTIONS) {
                    throw PollRequiredException()
                }
            }
        }
    }
}
