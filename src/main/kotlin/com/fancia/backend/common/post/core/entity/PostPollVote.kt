package com.fancia.backend.common.post.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Embeddable
data class PostPollVoteId(
    @Column(name = "post_id")
    var postId: UUID = UUID(0, 0),
    @Column(name = "option_id")
    var optionId: UUID = UUID(0, 0),
    @Column(name = "user_id")
    var userId: UUID = UUID(0, 0),
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is PostPollVoteId &&
            other.postId == postId &&
            other.optionId == optionId &&
            other.userId == userId

    override fun hashCode(): Int = Objects.hash(postId, optionId, userId)
}

@Entity
@Table(name = "post_poll_votes")
class PostPollVote(
    @EmbeddedId
    var id: PostPollVoteId,
) {
    @MapsId("postId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    var poll: PostPoll? = null

    @MapsId("optionId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "option_id", insertable = false, updatable = false)
    var option: PostPollOption? = null

    @CreationTimestamp
    @Column(name = "voted_at")
    var votedAt: LocalDateTime? = null
}
