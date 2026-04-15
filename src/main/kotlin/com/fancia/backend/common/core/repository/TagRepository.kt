package com.fancia.backend.common.core.repository

import com.fancia.backend.common.core.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    fun existsByName(@Param("name") name: String): Boolean

    @Query(
        """
        SELECT t FROM Tag t
        WHERE trgm_word_similarity(:name, t.name)
    """
    )
    fun findAll(
        @Param("name") name: String,
    ): List<Tag>

    fun findByName(@Param("name") name: String): Tag?
}