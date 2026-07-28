package com.fancia.backend.common.comment.core.repository

import com.fancia.backend.common.comment.core.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = ["likes"])
    fun findByTargetId(targetId: UUID, pageable: Pageable): Page<Comment>

    @EntityGraph(attributePaths = ["likes"])
    fun findByTargetIdAndResourceId(targetId: UUID, resourceId: UUID, pageable: Pageable): Page<Comment>

    @EntityGraph(attributePaths = ["likes"])
    override fun findById(id: UUID): Optional<Comment>
}
