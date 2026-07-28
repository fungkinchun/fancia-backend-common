package com.fancia.backend.common.post.core.controller

import com.fancia.backend.common.post.core.repository.PostRepository
import com.fancia.backend.common.post.core.service.PostService
import com.fancia.backend.common.post.mapper.toDto
import com.fancia.backend.shared.common.post.core.dto.CreatePostRequest
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import com.fancia.backend.shared.common.post.core.dto.UpdatePostRequest
import com.fancia.backend.shared.common.post.core.exception.PostNotFoundException
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/internal/posts")
@Tag(name = "Posts", description = "Internal post storage used by user, event, and interest group services.")
@SecurityRequirement(name = "bearerAuth")
class PostController(
    private val postService: PostService,
    private val postRepository: PostRepository,
) {
    @Operation(
        summary = "Create post",
        description = "Persists a post for a target. authorUserId must match JWT userId."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Post created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
        ]
    )
    @PostMapping
    fun create(
        @RequestBody @Valid request: CreatePostRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(request, jwt))
    }

    @Operation(summary = "List posts for target", description = "Paginated posts for an event, group, or user profile.")
    @GetMapping
    fun list(
        @RequestParam @Parameter(description = "Event, interest group, or user id") targetId: UUID,
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<Page<PostResponse>> {
        return ResponseEntity.ok(postService.listByTargetId(targetId, pageable, jwt))
    }

    @Operation(summary = "Get post by id")
    @GetMapping("/{postId}")
    fun getById(
        @PathVariable postId: UUID,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<PostResponse> {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        val post = postRepository.findById(postId).orElseThrow { PostNotFoundException(postId) }
        return ResponseEntity.ok(post.toDto(currentUserId))
    }

    @Operation(summary = "Update post")
    @PutMapping("/{postId}")
    fun update(
        @PathVariable postId: UUID,
        @RequestBody @Valid request: UpdatePostRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.update(postId, request, jwt))
    }

    @Operation(summary = "Like post")
    @PostMapping("/{postId}/likes")
    fun like(
        @PathVariable postId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        postService.like(postId, jwt)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Unlike post")
    @DeleteMapping("/{postId}/likes")
    fun unlike(
        @PathVariable postId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        postService.unlike(postId, jwt)
        return ResponseEntity.noContent().build()
    }
}
