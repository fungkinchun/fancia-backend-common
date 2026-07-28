package com.fancia.backend.common.tag.core.controller

import com.fancia.backend.common.tag.core.message.TagProducer
import com.fancia.backend.common.tag.core.service.TagService
import com.fancia.backend.shared.common.tag.core.dto.CreateTagsRequest
import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.enums.TagType
import com.fancia.backend.shared.common.tag.core.message.TagDeletedEvent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Endpoints for managing global tags")
@SecurityRequirement(name = "bearerAuth")
class TagController(
    private val tagService: TagService,
    private val tagProducer: TagProducer,
) {
    @Operation(
        summary = "Create new tags",
        description = "Creates tags with a type. Name must be unique per type.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Tags created"),
            ApiResponse(responseCode = "409", description = "Tag name already exists for type"),
        ],
    )
    @PostMapping
    fun createTags(
        @RequestBody @Valid request: CreateTagsRequest,
        @PageableDefault(size = 20)
        pageable: Pageable,
    ): ResponseEntity<Page<TagResponse>> {
        val tag = tagService.create(request, pageable)
        return ResponseEntity.status(HttpStatus.CREATED).body(tag)
    }

    @Operation(
        summary = "Search similar tags",
        description = "Read-only fuzzy search by tag name. Does not create tags.",
    )
    @GetMapping
    fun listTags(
        @RequestParam(required = false)
        @Parameter(description = "Fuzzy search by tag name (case-insensitive)")
        search: List<String>,
        @RequestParam(defaultValue = "INTEREST")
        @Parameter(description = "Tag type to filter by")
        type: TagType,
        @PageableDefault(size = 20)
        pageable: Pageable,
    ): ResponseEntity<Page<TagResponse>> {
        val tags = tagService.searchSimilar(search, type, pageable)
        return ResponseEntity.ok(tags)
    }

    @GetMapping("/ids")
    fun listTagsByIds(
        @RequestParam id: Set<UUID>,
    ): ResponseEntity<List<TagResponse>> {
        return ResponseEntity.ok(tagService.findByIds(id))
    }

    @DeleteMapping("/{id}")
    fun deleteTagById(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val deleted = tagService.deleteTagById(id)
        tagProducer.publishTagDeleted(
            TagDeletedEvent(
                id = deleted.id!!,
                name = deleted.name,
                type = deleted.type,
            ),
        )
        return ResponseEntity.noContent().build()
    }
}
