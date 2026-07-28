package com.fancia.backend.common.post.mapper

import com.fancia.backend.common.post.core.entity.Post
import com.fancia.backend.common.post.core.entity.PostMedia
import com.fancia.backend.shared.common.post.core.dto.CreatePostRequest
import com.fancia.backend.shared.common.post.core.dto.PostMediaResponse
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import java.util.*

fun Post.toDto(currentUserId: UUID?): PostResponse =
    PostResponse(
        id = id!!,
        targetId = targetId,
        authorUserId = authorUserId,
        body = body,
        media = media.sortedBy { it.sortOrder }.map { it.toDto() },
        featured = featured,
        pinned = pinned,
        likeCount = likes.size.toLong(),
        likedByCurrentUser = currentUserId != null && likes.any { it.id.userId == currentUserId },
        createdAt = createdAt,
    )

fun CreatePostRequest.toEntity(): Post =
    Post(
        targetId = targetId!!,
        authorUserId = authorUserId!!,
        body = body,
        featured = featured,
        pinned = pinned,
    )

private fun PostMedia.toDto(): PostMediaResponse =
    PostMediaResponse(
        objectKey = objectKey,
        mediaType = mediaType,
        sortOrder = sortOrder,
    )
