package com.fancia.backend.common.post.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import jakarta.persistence.*
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
    @Column(name = "is_featured", nullable = false)
    var featured: Boolean = false,
    @Column(name = "is_pinned", nullable = false)
    var pinned: Boolean = false,
) : AbstractEntity() {
    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    var media: MutableList<PostMedia> = mutableListOf()

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var likes: MutableSet<PostLike> = mutableSetOf()
}
