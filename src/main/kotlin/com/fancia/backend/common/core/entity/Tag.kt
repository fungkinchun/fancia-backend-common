package com.fancia.backend.common.core.entity

import com.fancia.backend.shared.common.core.entity.AbstractEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "tags")
class Tag(
    @Column(nullable = false, unique = true)
    var name: String = ""
) : AbstractEntity()