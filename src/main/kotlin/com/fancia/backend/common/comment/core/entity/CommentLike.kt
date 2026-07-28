package com.fancia.backend.common.comment.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Embeddable
data class CommentLikeId(
    @Column(name = "comment_id")
    var commentId: UUID,
    @Column(name = "user_id")
    var userId: UUID,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is CommentLikeId &&
                other.commentId == commentId &&
                other.userId == userId

    override fun hashCode(): Int = Objects.hash(commentId, userId)
}

@Entity
@Table(name = "comment_likes")
class CommentLike(
    @EmbeddedId
    var id: CommentLikeId,
) {
    @MapsId("commentId")
    @ManyToOne
    @JoinColumn(name = "comment_id", insertable = false, updatable = false)
    var comment: Comment? = null

    @CreationTimestamp
    var likedAt: LocalDateTime? = null
}
