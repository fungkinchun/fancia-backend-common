package com.fancia.backend.common.post.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import com.fancia.backend.shared.common.post.core.enums.PostMediaType
import jakarta.persistence.*

@Entity
@Table(name = "post_media")
class PostMedia(
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,
    @Column(name = "object_key", nullable = false, length = 1024)
    var objectKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    var mediaType: PostMediaType,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,
) : AbstractEntity()
