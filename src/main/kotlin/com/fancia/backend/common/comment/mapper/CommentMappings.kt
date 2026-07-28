package com.fancia.backend.common.comment.mapper

import com.fancia.backend.common.comment.core.entity.Comment
import com.fancia.backend.shared.common.comment.core.dto.CommentResponse
import com.fancia.backend.shared.common.comment.core.dto.CreateCommentRequest
import java.util.*

fun Comment.toDto(currentUserId: UUID?): CommentResponse =
    CommentResponse(
        id = id!!,
        targetId = targetId,
        resourceId = resourceId,
        authorUserId = authorUserId,
        body = body,
        createdAt = createdAt,
        likeCount = likes.size.toLong(),
        likedByCurrentUser = currentUserId != null && likes.any { it.id.userId == currentUserId },
    )

fun CreateCommentRequest.toEntity(): Comment =
    Comment(
        targetId = targetId!!,
        resourceId = resourceId,
        authorUserId = UUID(0, 0),
        body = body,
    )
