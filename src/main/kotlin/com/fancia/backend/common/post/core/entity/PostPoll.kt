package com.fancia.backend.common.post.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "post_polls")
class PostPoll(
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "post_id")
    var post: Post,
    @Column(name = "allow_multiple", nullable = false)
    var allowMultiple: Boolean = false,
    @Column(name = "closes_at")
    var closesAt: LocalDateTime? = null,
) {
    @Id
    @Column(name = "post_id")
    var postId: UUID? = null

    @OneToMany(mappedBy = "poll", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 32)
    var options: MutableSet<PostPollOption> = mutableSetOf()

    @OneToMany(mappedBy = "poll", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 32)
    var votes: MutableSet<PostPollVote> = mutableSetOf()

    fun isClosed(now: LocalDateTime = LocalDateTime.now()): Boolean =
        closesAt?.let { !it.isAfter(now) } == true
}
