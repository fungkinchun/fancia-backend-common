package com.fancia.backend.common.rating.core.controller

import com.fancia.backend.common.rating.core.service.RatingService
import com.fancia.backend.shared.common.rating.core.dto.ResourceRatingResponse
import com.fancia.backend.shared.common.rating.core.dto.ResourceRatingSummaryResponse
import com.fancia.backend.shared.common.rating.core.dto.UpsertResourceRatingRequest
import com.fancia.backend.shared.common.rating.core.enums.RatedResourceType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ratings")
@Tag(name = "Ratings", description = "1–5 star ratings for events, interest groups, and venues")
@SecurityRequirement(name = "bearerAuth")
class RatingController(
    private val ratingService: RatingService,
) {
    @Operation(summary = "Upsert rating", description = "Create or update the current user's 1–5 star rating.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Rating saved"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
        ],
    )
    @PutMapping("/{resourceType}/{resourceId}")
    fun upsert(
        @PathVariable resourceType: RatedResourceType,
        @PathVariable resourceId: UUID,
        @RequestBody @Valid request: UpsertResourceRatingRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<ResourceRatingResponse> =
        ResponseEntity.ok(ratingService.upsert(resourceType, resourceId, request, jwt))

    @Operation(summary = "Delete own rating")
    @DeleteMapping("/{resourceType}/{resourceId}")
    fun delete(
        @PathVariable resourceType: RatedResourceType,
        @PathVariable resourceId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        ratingService.delete(resourceType, resourceId, jwt)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Get rating summary",
        description = "Average, count, and the current user's rating when authenticated.",
    )
    @GetMapping("/{resourceType}/{resourceId}")
    fun summary(
        @PathVariable resourceType: RatedResourceType,
        @PathVariable resourceId: UUID,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ResourceRatingSummaryResponse> =
        ResponseEntity.ok(ratingService.summary(resourceType, resourceId, jwt))

    @Operation(summary = "Get own rating")
    @GetMapping("/{resourceType}/{resourceId}/me")
    fun getOwn(
        @PathVariable resourceType: RatedResourceType,
        @PathVariable resourceId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<ResourceRatingResponse> {
        val rating = ratingService.getOwn(resourceType, resourceId, jwt)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(rating)
    }
}
