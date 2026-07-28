package com.fancia.backend.common.comment.core.service

import com.fancia.backend.common.comment.core.entity.CommentLike
import com.fancia.backend.common.comment.core.entity.CommentLikeId
import com.fancia.backend.common.comment.core.repository.CommentLikeRepository
import com.fancia.backend.common.comment.core.repository.CommentRepository
import com.fancia.backend.common.comment.mapper.toDto
import com.fancia.backend.common.comment.mapper.toEntity
import com.fancia.backend.shared.common.comment.core.dto.CommentResponse
import com.fancia.backend.shared.common.comment.core.dto.CreateCommentRequest
import com.fancia.backend.shared.common.comment.core.exception.CommentNotFoundException
import com.fancia.backend.shared.common.core.exception.InvalidAuthenticationException
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
) {
    @Transactional
    fun create(@Valid request: CreateCommentRequest, jwt: Jwt): CommentResponse {
        val currentUserId = jwt.getClaimAsString("userId")?.let { UUID.fromString(it) }
            ?: throw InvalidAuthenticationException()
        val targetId = request.targetId ?: throw InvalidAuthenticationException()
        val comment = request.toEntity().apply {
            authorUserId = currentUserId
            this.targetId = targetId
        }
        return commentRepository.save(comment).toDto(currentUserId)
    }

    @Transactional
    fun like(commentId: UUID, jwt: Jwt) {
        val currentUserId = jwt.getClaimAsString("userId")?.let { UUID.fromString(it) }
            ?: throw InvalidAuthenticationException()
        val comment = commentRepository.findById(commentId).orElseThrow { CommentNotFoundException(commentId) }
        if (commentLikeRepository.existsByIdCommentIdAndIdUserId(commentId, currentUserId)) {
            return
        }
        comment.likes.add(
            CommentLike(CommentLikeId(commentId, currentUserId)).apply { this.comment = comment }
        )
        commentRepository.save(comment)
    }

    @Transactional
    fun unlike(commentId: UUID, jwt: Jwt) {
        val currentUserId = jwt.getClaimAsString("userId")?.let { UUID.fromString(it) }
            ?: throw InvalidAuthenticationException()
        val comment = commentRepository.findById(commentId).orElseThrow { CommentNotFoundException(commentId) }
        comment.likes.removeIf { it.id.userId == currentUserId }
        commentRepository.save(comment)
    }

    fun listByTargetId(targetId: UUID, resourceId: UUID?, pageable: Pageable, jwt: Jwt?): Page<CommentResponse> {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        val comments = if (resourceId != null) {
            commentRepository.findByTargetIdAndResourceId(targetId, resourceId, pageable)
        } else {
            commentRepository.findByTargetId(targetId, pageable)
        }
        return comments.map { it.toDto(currentUserId) }
    }
}
