package com.fancia.backend.common.post.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Embeddable
data class PostLikeId(
    @Column(name = "post_id")
    var postId: UUID,
    @Column(name = "user_id")
    var userId: UUID,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is PostLikeId &&
                other.postId == postId &&
                other.userId == userId

    override fun hashCode(): Int = Objects.hash(postId, userId)
}

@Entity
@Table(name = "post_likes")
class PostLike(
    @EmbeddedId
    var id: PostLikeId,
) {
    @MapsId("postId")
    @ManyToOne
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    var post: Post? = null

    @CreationTimestamp
    var likedAt: LocalDateTime? = null
}
