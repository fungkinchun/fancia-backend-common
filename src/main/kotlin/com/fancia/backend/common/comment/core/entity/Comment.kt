package com.fancia.backend.common.comment.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "comments")
class Comment(
    @Column(name = "target_id", nullable = false)
    var targetId: UUID,
    @Column(name = "resource_id")
    var resourceId: UUID? = null,
    @Column(name = "author_user_id", nullable = false)
    var authorUserId: UUID,
    @Column(nullable = false, length = 4000)
    var body: String,
) : AbstractEntity() {
    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], orphanRemoval = true)
    var likes: MutableSet<CommentLike> = mutableSetOf()
}
