package com.fancia.backend.common.comment.core.repository

import com.fancia.backend.common.comment.core.entity.CommentLike
import com.fancia.backend.common.comment.core.entity.CommentLikeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentLikeRepository : JpaRepository<CommentLike, CommentLikeId> {
    fun existsByIdCommentIdAndIdUserId(commentId: UUID, userId: UUID): Boolean
}
