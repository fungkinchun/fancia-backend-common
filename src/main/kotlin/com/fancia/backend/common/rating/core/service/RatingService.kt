package com.fancia.backend.common.rating.core.service

import com.fancia.backend.shared.common.core.exception.InvalidAuthenticationException
import com.fancia.backend.shared.common.rating.core.dto.ResourceRatingResponse
import com.fancia.backend.shared.common.rating.core.dto.ResourceRatingSummaryResponse
import com.fancia.backend.shared.common.rating.core.dto.UpsertResourceRatingRequest
import com.fancia.backend.shared.common.rating.core.entity.ResourceRating
import com.fancia.backend.shared.common.rating.core.entity.ResourceRatingId
import com.fancia.backend.shared.common.rating.core.enums.RatedResourceType
import com.fancia.backend.shared.common.rating.core.repository.ResourceRatingRepository
import com.fancia.backend.shared.common.rating.core.support.loadEnrichment
import jakarta.validation.Valid
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RatingService(
    private val resourceRatingRepository: ResourceRatingRepository,
) {
    @Transactional
    fun upsert(
        resourceType: RatedResourceType,
        resourceId: UUID,
        @Valid request: UpsertResourceRatingRequest,
        jwt: Jwt,
    ): ResourceRatingResponse {
        val userId = requireUserId(jwt)
        val id = ResourceRatingId(
            resourceType = resourceType,
            resourceId = resourceId,
            userId = userId,
        )
        val rating = resourceRatingRepository.findById(id).orElse(null)
            ?.also { it.stars = request.stars }
            ?: ResourceRating(id = id, stars = request.stars)
        return resourceRatingRepository.save(rating).toResponse()
    }

    @Transactional
    fun delete(resourceType: RatedResourceType, resourceId: UUID, jwt: Jwt) {
        val userId = requireUserId(jwt)
        resourceRatingRepository.deleteByIdResourceTypeAndIdResourceIdAndIdUserId(
            resourceType,
            resourceId,
            userId,
        )
    }

    @Transactional(readOnly = true)
    fun summary(
        resourceType: RatedResourceType,
        resourceId: UUID,
        jwt: Jwt?,
    ): ResourceRatingSummaryResponse {
        val userId = jwt?.getClaimAsString("userId")
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val enrichment = resourceRatingRepository.loadEnrichment(resourceType, resourceId, userId)
        return ResourceRatingSummaryResponse(
            resourceType = resourceType,
            resourceId = resourceId,
            averageRating = enrichment.averageRating,
            ratingCount = enrichment.ratingCount,
            currentUserRating = enrichment.currentUserRating,
        )
    }

    @Transactional(readOnly = true)
    fun getOwn(
        resourceType: RatedResourceType,
        resourceId: UUID,
        jwt: Jwt,
    ): ResourceRatingResponse? {
        val userId = requireUserId(jwt)
        return resourceRatingRepository
            .findByIdResourceTypeAndIdResourceIdAndIdUserId(resourceType, resourceId, userId)
            .map { it.toResponse() }
            .orElse(null)
    }

    private fun requireUserId(jwt: Jwt): UUID =
        jwt.getClaimAsString("userId")?.let { UUID.fromString(it) }
            ?: throw InvalidAuthenticationException()

    private fun ResourceRating.toResponse() =
        ResourceRatingResponse(
            resourceType = id.resourceType,
            resourceId = id.resourceId,
            userId = id.userId,
            stars = stars,
            updatedAt = updatedAt,
        )
}
