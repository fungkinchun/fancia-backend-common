package com.fancia.backend.common

import com.fancia.backend.shared.common.post.core.enums.PostStatus
import com.fancia.backend.common.post.core.repository.PostRepository
import com.fancia.backend.shared.common.post.core.dto.PostResponse
import com.fancia.backend.shared.common.post.core.enums.PostMediaType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [CommonApplication::class])
@AutoConfigureMockMvc
@Testcontainers
@Import(TestConfig::class)
class PostControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val postRepository: PostRepository,
    private val jsonMapper: JsonMapper,
) : FunSpec({
    test("should create featured post with media only") {
        val userId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val requestBody = mapOf(
            "targetId" to targetId.toString(),
            "authorUserId" to userId.toString(),
            "body" to null,
            "media" to listOf(
                mapOf(
                    "objectKey" to "tmp/1d61b8be-46d4-4131-b9e5-5c30515c58b4.jpg",
                    "mediaType" to "image",
                ),
                mapOf(
                    "objectKey" to "tmp/e63f5417-de54-45b9-a793-62b7ebda8050.jpg",
                    "mediaType" to "image",
                ),
            ),
            "status" to "FEATURED",
        )
        val responseBody = mockMvc
            .post("/internal/posts") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(requestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andDo { print() }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id", notNullValue())
                jsonPath("$.targetId", `is`(targetId.toString()))
                jsonPath("$.authorUserId", `is`(userId.toString()))
                jsonPath("$.status", `is`("FEATURED"))
                jsonPath("$.kind", `is`("TEXT"))
                jsonPath("$.media.length()", `is`(2))
                jsonPath("$.media[0].objectKey", `is`("tmp/1d61b8be-46d4-4131-b9e5-5c30515c58b4.jpg"))
                jsonPath("$.media[0].sortOrder", `is`(0))
                jsonPath("$.media[1].sortOrder", `is`(1))
            }
            .andReturn()
            .response
            .contentAsString
        val response = jsonMapper.readValue(responseBody, object : TypeReference<PostResponse>() {})
        response.status shouldBe PostStatus.FEATURED
        response.media shouldHaveSize 2
        response.media[0].mediaType shouldBe PostMediaType.IMAGE
        val savedPost = postRepository.findById(response.id).orElseThrow()
        savedPost.status shouldBe PostStatus.FEATURED
    }

    test("should create poll post and accept votes") {
        val userId = UUID.randomUUID()
        val voterId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val createBody = mapOf(
            "targetId" to targetId.toString(),
            "authorUserId" to userId.toString(),
            "body" to "Where should we meet?",
            "kind" to "POLL",
            "poll" to mapOf(
                "options" to listOf("Park", "Cafe", "Pub"),
                "allowMultiple" to false,
            ),
            "media" to emptyList<Any>(),
            "status" to "VISIBLE",
        )
        val created = mockMvc
            .post("/internal/posts") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(createBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isCreated() }
                jsonPath("$.kind", `is`("POLL"))
                jsonPath("$.poll.options.length()", `is`(3))
                jsonPath("$.poll.totalVotes", `is`(0))
                jsonPath("$.poll.closed", `is`(false))
            }
            .andReturn()
            .response
            .contentAsString
            .let { jsonMapper.readValue(it, object : TypeReference<PostResponse>() {}) }

        val optionId = created.poll!!.options.first { it.label == "Cafe" }.id

        mockMvc
            .post("/internal/posts/${created.id}/votes") {
                with(jwt().jwt { it.claim("userId", voterId) })
                content = jsonMapper.writeValueAsString(mapOf("optionIds" to listOf(optionId.toString())))
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.poll.totalVotes", `is`(1))
                jsonPath("$.poll.options[?(@.label == 'Cafe')].voteCount", `is`(listOf(1)))
                jsonPath("$.poll.options[?(@.label == 'Cafe')].selectedByCurrentUser", `is`(listOf(true)))
            }

        mockMvc
            .get("/internal/posts?targetId=$targetId&kind=POLL&status=VISIBLE&status=FEATURED&status=PINNED") {
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements", `is`(1))
                jsonPath("$.content[0].kind", `is`("POLL"))
                jsonPath("$.content[0].poll.closed", `is`(false))
            }

        val closedBody = mapOf(
            "targetId" to targetId.toString(),
            "authorUserId" to userId.toString(),
            "body" to "Already closed",
            "kind" to "POLL",
            "poll" to mapOf(
                "options" to listOf("Yes", "No"),
                "allowMultiple" to false,
                "closesAt" to LocalDateTime.now().minusHours(1).toString(),
            ),
            "media" to emptyList<Any>(),
            "status" to "VISIBLE",
        )
        mockMvc
            .post("/internal/posts") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(closedBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect { status { isCreated() } }

        mockMvc
            .get("/internal/posts?targetId=$targetId&kind=POLL&status=VISIBLE&status=FEATURED&status=PINNED") {
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements", `is`(1))
                jsonPath("$.content[0].body", `is`("Where should we meet?"))
            }
    }

    afterSpec {
        postRepository.deleteAll()
    }
})
