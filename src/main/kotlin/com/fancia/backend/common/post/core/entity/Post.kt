package com.fancia.backend.common.post.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import com.fancia.backend.shared.common.post.core.enums.PostKind
import com.fancia.backend.shared.common.post.core.enums.PostStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "posts")
class Post(
    @Column(name = "target_id", nullable = false)
    var targetId: UUID,
    @Column(name = "author_user_id", nullable = false)
    var authorUserId: UUID,
    @Column(length = 4000)
    var body: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: PostStatus = PostStatus.VISIBLE,
    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var kind: PostKind = PostKind.TEXT,
) : AbstractEntity() {
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    var media: MutableSet<PostMedia> = mutableSetOf()

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var likes: MutableSet<PostLike> = mutableSetOf()

    @OneToOne(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var poll: PostPoll? = null

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        expiredAt != null && !expiredAt!!.isAfter(now)
}
