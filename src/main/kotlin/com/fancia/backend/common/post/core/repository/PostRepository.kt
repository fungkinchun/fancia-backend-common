package com.fancia.backend.common.post.core.repository

import com.fancia.backend.common.post.core.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PostRepository : JpaRepository<Post, UUID> {
    @EntityGraph(attributePaths = ["media", "likes"])
    @Query(
        """
        SELECT p FROM Post p
        WHERE p.targetId = :targetId
        ORDER BY p.pinned DESC, p.createdAt DESC
        """
    )
    fun findByTargetIdOrderByPinnedFirst(targetId: UUID, pageable: Pageable): Page<Post>

    @EntityGraph(attributePaths = ["media", "likes"])
    override fun findById(id: UUID): Optional<Post>

    @Modifying
    @Query(
        """
        UPDATE Post p
        SET p.featured = false
        WHERE p.targetId = :targetId AND p.featured = true
        """
    )
    fun clearFeaturedByTargetId(targetId: UUID)
}
