package com.fancia.backend.common.post.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import jakarta.persistence.*

@Entity
@Table(name = "post_poll_options")
class PostPollOption(
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    var poll: PostPoll,
    @Column(nullable = false, length = 255)
    var label: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
) : AbstractEntity()
