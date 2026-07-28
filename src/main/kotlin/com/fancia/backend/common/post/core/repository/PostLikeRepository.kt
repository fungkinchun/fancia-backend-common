package com.fancia.backend.common.post.core.repository

import com.fancia.backend.common.post.core.entity.PostLike
import com.fancia.backend.common.post.core.entity.PostLikeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PostLikeRepository : JpaRepository<PostLike, PostLikeId> {
    fun existsByIdPostIdAndIdUserId(postId: UUID, userId: UUID): Boolean
}
