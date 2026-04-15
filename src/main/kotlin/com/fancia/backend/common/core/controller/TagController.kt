package com.fancia.backend.common.core.controller

import com.fancia.backend.common.core.message.TagProducer
import com.fancia.backend.common.core.service.TagService
import com.fancia.backend.shared.common.core.dto.CreateTagsRequest
import com.fancia.backend.shared.common.core.dto.TagResponse
import com.fancia.backend.shared.common.core.message.TagDeletedEvent
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

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Endpoints for managing global tags")
@SecurityRequirement(name = "bearerAuth")
class TagController(
    private val tagService: TagService,
    private val tagProducer: TagProducer
) {
    @Operation(
        summary = "Create a new tag",
        description = "Creates a new unique tag. Name must be unique."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Tag created"),
            ApiResponse(responseCode = "409", description = "Tag name already exists")
        ]
    )
    @PostMapping
    fun createTags(
        @RequestBody @Valid request: CreateTagsRequest,
        @PageableDefault(size = 20)
        pageable: Pageable
    ): ResponseEntity<Page<TagResponse>> {
        val tag = tagService.create(request, pageable)
        return ResponseEntity.status(HttpStatus.CREATED).body(tag)
    }

    @Operation(
        summary = "List tags",
        description = "Paginated list of tags. Supports fuzzy search."
    )
    @GetMapping
    fun listTags(
        @RequestParam(required = false)
        @Parameter(description = "Fuzzy search by tag (case-insensitive)")
        search: List<String>,
        @PageableDefault(size = 20)
        pageable: Pageable
    ): ResponseEntity<Page<TagResponse>> {
        val tags = tagService.findAll(search, pageable)
        return ResponseEntity.ok(tags)
    }

    @DeleteMapping("/{name}")
    fun deleteTagByName(
        @PathVariable name: String
    ): ResponseEntity<Void> {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .build<Void>()
            .also {
                tagService.deleteTagByName(name)
                tagProducer.publishTagDeleted(TagDeletedEvent(name))
            }
    }
}