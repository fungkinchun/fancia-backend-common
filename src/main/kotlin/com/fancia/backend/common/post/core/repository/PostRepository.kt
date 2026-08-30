package com.fancia.backend.common.post.core.repository

import com.fancia.backend.common.post.core.entity.Post
import com.fancia.backend.shared.common.post.core.enums.PostKind
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface PostRepository : JpaRepository<Post, UUID> {
    @EntityGraph(attributePaths = ["media", "likes", "poll", "poll.options", "poll.votes"])
    @Query(
        """
        SELECT DISTINCT p FROM Post p
        LEFT JOIN p.poll poll
        WHERE p.targetId = :targetId
          AND p.status <> com.fancia.backend.shared.common.post.core.enums.PostStatus.HIDDEN
          AND (:kind IS NULL OR p.kind = :kind)
          AND (
            :openOnly = false
            OR (
              p.kind = com.fancia.backend.shared.common.post.core.enums.PostKind.POLL
              AND poll IS NOT NULL
              AND (poll.closesAt IS NULL OR poll.closesAt > :now)
              AND p.status <> com.fancia.backend.shared.common.post.core.enums.PostStatus.READ_ONLY
            )
          )
        ORDER BY
          CASE p.status
            WHEN com.fancia.backend.shared.common.post.core.enums.PostStatus.PINNED THEN 2
            WHEN com.fancia.backend.shared.common.post.core.enums.PostStatus.FEATURED THEN 1
            ELSE 0
          END DESC,
          p.createdAt DESC
        """,
    )
    fun findByTargetIdFiltered(
        @Param("targetId") targetId: UUID,
        @Param("kind") kind: PostKind?,
        @Param("openOnly") openOnly: Boolean,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): Page<Post>

    @EntityGraph(attributePaths = ["media", "likes", "poll", "poll.options", "poll.votes"])
    override fun findById(id: UUID): Optional<Post>

    @Modifying
    @Query(
        """
        UPDATE Post p
        SET p.status = com.fancia.backend.shared.common.post.core.enums.PostStatus.VISIBLE
        WHERE p.targetId = :targetId
          AND p.status = com.fancia.backend.shared.common.post.core.enums.PostStatus.FEATURED
        """,
    )
    fun clearFeaturedByTargetId(targetId: UUID)
}
