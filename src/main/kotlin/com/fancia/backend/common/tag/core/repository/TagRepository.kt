package com.fancia.backend.common.tag.core.repository

import com.fancia.backend.shared.common.tag.core.entity.Tag
import com.fancia.backend.shared.common.tag.core.enums.TagType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TagRepository : JpaRepository<Tag, UUID> {
    fun existsByNameAndType(name: String, type: TagType): Boolean

    @Query(
        """
        SELECT t FROM Tag t
        WHERE trgm_word_similarity(:name, t.name)
        AND t.type = :type
    """,
    )
    fun findSimilarByNameAndType(
        @Param("name") name: String,
        @Param("type") type: TagType,
    ): List<Tag>

    fun findByNameAndType(name: String, type: TagType): Tag?

    fun findByIdIn(ids: Collection<UUID>): List<Tag>
}
