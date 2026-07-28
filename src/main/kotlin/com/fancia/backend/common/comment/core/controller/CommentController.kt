package com.fancia.backend.common.comment.core.controller

import com.fancia.backend.common.comment.core.repository.CommentRepository
import com.fancia.backend.common.comment.core.service.CommentService
import com.fancia.backend.common.comment.mapper.toDto
import com.fancia.backend.shared.common.comment.core.dto.CommentResponse
import com.fancia.backend.shared.common.comment.core.dto.CreateCommentRequest
import com.fancia.backend.shared.common.comment.core.exception.CommentNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/internal/comments")
@Tag(
    name = "Comments",
    description = "Endpoints for managing comments on various targets (events, interest groups, etc). This is an internal API used by other services, not intended for public use."
)
@SecurityRequirement(name = "bearerAuth")
class CommentController(
    private val commentService: CommentService,
    private val commentRepository: CommentRepository,
) {
    @Operation(
        summary = "Create comment",
        description = "Persists a comment for the given target. Author is taken from the JWT userId.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Comment created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "404", description = "Parent comment not found"),
        ]
    )
    @PostMapping
    fun create(
        @RequestBody @Valid request: CreateCommentRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(request, jwt))
    }

    @Operation(
        summary = "List comments",
        description = "Paginated comments for a target. Use event/post/group id for top-level comments, or a comment id for replies.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Comments returned"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
        ]
    )
    @GetMapping
    fun list(
        @RequestParam
        @Parameter(description = "Target id (event, interest group, post, or parent comment id for replies)")
        targetId: UUID,
        @RequestParam(required = false)
        @Parameter(description = "Resource id (event, interest group, or post id for access scoping)")
        resourceId: UUID?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<Page<CommentResponse>> {
        return ResponseEntity.ok(commentService.listByTargetId(targetId, resourceId, pageable, jwt))
    }

    @Operation(summary = "Get comment by id")
    @GetMapping("/{commentId}")
    fun getById(
        @PathVariable commentId: UUID,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<CommentResponse> {
        val currentUserId = jwt?.getClaimAsString("userId")?.let { UUID.fromString(it) }
        val comment = commentRepository.findById(commentId).orElseThrow { CommentNotFoundException(commentId) }
        return ResponseEntity.ok(comment.toDto(currentUserId))
    }

    @Operation(summary = "Like comment")
    @PostMapping("/{commentId}/likes")
    fun like(
        @PathVariable commentId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        commentService.like(commentId, jwt)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Unlike comment")
    @DeleteMapping("/{commentId}/likes")
    fun unlike(
        @PathVariable commentId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        commentService.unlike(commentId, jwt)
        return ResponseEntity.noContent().build()
    }
}
