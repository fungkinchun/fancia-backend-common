package com.fancia.backend.common

import com.fancia.backend.common.comment.core.repository.CommentRepository
import com.fancia.backend.common.post.core.repository.PostRepository
import com.fancia.backend.shared.common.comment.core.dto.CommentResponse
import io.kotest.core.spec.style.FunSpec
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
import java.util.*

@SpringBootTest(classes = [CommonApplication::class])
@AutoConfigureMockMvc
@Testcontainers
@Import(TestConfig::class)
class CommentControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val jsonMapper: JsonMapper,
) : FunSpec({
    test("should create comment on post with resourceId and null likes collection") {
        val userId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val postRequestBody = mapOf(
            "targetId" to targetId.toString(),
            "authorUserId" to userId.toString(),
            "body" to "Test post",
            "media" to emptyList<Any>(),
            "featured" to false,
            "pinned" to false,
        )
        val postResponseBody = mockMvc
            .post("/internal/posts") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(postRequestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response
            .contentAsString
        val postId = jsonMapper.readTree(postResponseBody).get("id").asText()
        val commentRequestBody = mapOf(
            "targetId" to postId,
            "resourceId" to postId,
            "body" to "London Hikers",
        )
        val responseBody = mockMvc
            .post("/internal/comments") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(commentRequestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id", notNullValue())
                jsonPath("$.targetId", `is`(postId))
                jsonPath("$.resourceId", `is`(postId))
                jsonPath("$.authorUserId", `is`(userId.toString()))
                jsonPath("$.body", `is`("London Hikers"))
                jsonPath("$.likeCount", `is`(0))
                jsonPath("$.likedByCurrentUser", `is`(false))
            }
            .andReturn()
            .response
            .contentAsString
        val response = jsonMapper.readValue(responseBody, object : TypeReference<CommentResponse>() {})
        response.likeCount shouldBe 0
        response.likedByCurrentUser shouldBe false
    }

    test("should list comments by targetId and resourceId") {
        val userId = UUID.randomUUID()
        val resourceId = UUID.randomUUID()
        val commentRequestBody = mapOf(
            "targetId" to resourceId.toString(),
            "resourceId" to resourceId.toString(),
            "body" to "Top-level comment",
        )
        val createResponseBody = mockMvc
            .post("/internal/comments") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(commentRequestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response
            .contentAsString
        val commentId = jsonMapper.readTree(createResponseBody).get("id").asText()

        mockMvc
            .get("/internal/comments") {
                with(jwt().jwt { it.claim("userId", userId) })
                param("targetId", resourceId.toString())
                param("resourceId", resourceId.toString())
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.content.length()", `is`(1))
                jsonPath("$.content[0].id", `is`(commentId))
                jsonPath("$.content[0].resourceId", `is`(resourceId.toString()))
            }
        val replyRequestBody = mapOf(
            "targetId" to commentId,
            "resourceId" to resourceId.toString(),
            "body" to "Reply comment",
        )
        mockMvc
            .post("/internal/comments") {
                with(jwt().jwt { it.claim("userId", userId) })
                content = jsonMapper.writeValueAsString(replyRequestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect { status { isCreated() } }

        mockMvc
            .get("/internal/comments") {
                with(jwt().jwt { it.claim("userId", userId) })
                param("targetId", commentId)
                param("resourceId", resourceId.toString())
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.content.length()", `is`(1))
                jsonPath("$.content[0].targetId", `is`(commentId))
                jsonPath("$.content[0].resourceId", `is`(resourceId.toString()))
                jsonPath("$.content[0].body", `is`("Reply comment"))
            }
    }

    afterSpec {
        commentRepository.deleteAll()
        postRepository.deleteAll()
    }
})
