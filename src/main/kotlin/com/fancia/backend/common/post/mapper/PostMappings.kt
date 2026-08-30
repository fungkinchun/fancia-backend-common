package com.fancia.backend.common.post.mapper

import com.fancia.backend.common.post.core.entity.Post
import com.fancia.backend.common.post.core.entity.PostMedia
import com.fancia.backend.common.post.core.entity.PostPoll
import com.fancia.backend.shared.common.post.core.dto.CreatePostRequest
import com.fancia.backend.shared.common.post.core.dto.PollOptionResponse
import com.fancia.backend.shared.common.post.core.dto.PollResponse
import com.fancia.backend.shared.common.post.core.dto.PostMediaResponse
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import java.time.LocalDateTime
import java.util.*

fun Post.toDto(currentUserId: UUID?): PostResponse =
    PostResponse(
        id = id!!,
        targetId = targetId,
        authorUserId = authorUserId,
        body = body,
        media = media.sortedBy { it.sortOrder }.map { it.toDto() },
        status = status,
        expiredAt = expiredAt,
        likeCount = likes.size.toLong(),
        likedByCurrentUser = currentUserId != null && likes.any { it.id.userId == currentUserId },
        createdAt = createdAt,
        kind = kind,
        poll = poll?.toDto(currentUserId),
    )

fun CreatePostRequest.toEntity(): Post =
    Post(
        targetId = targetId!!,
        authorUserId = authorUserId!!,
        body = body,
        status = status,
        expiredAt = expiredAt,
        kind = kind,
    )

private fun PostMedia.toDto(): PostMediaResponse =
    PostMediaResponse(
        objectKey = objectKey,
        mediaType = mediaType,
        sortOrder = sortOrder,
    )

private fun PostPoll.toDto(currentUserId: UUID?): PollResponse {
    val now = LocalDateTime.now()
    val optionResponses = options.sortedBy { it.sortOrder }.map { option ->
        val optionId = option.id!!
        PollOptionResponse(
            id = optionId,
            label = option.label,
            sortOrder = option.sortOrder,
            voteCount = votes.count { it.id.optionId == optionId }.toLong(),
            selectedByCurrentUser = currentUserId != null &&
                votes.any { it.id.optionId == optionId && it.id.userId == currentUserId },
        )
    }
    return PollResponse(
        allowMultiple = allowMultiple,
        closesAt = closesAt,
        closed = isClosed(now),
        totalVotes = votes.map { it.id.userId }.toSet().size.toLong(),
        options = optionResponses,
    )
}
