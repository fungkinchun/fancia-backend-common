package com.fancia.backend.common

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
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
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
            "featured" to true,
            "pinned" to false,
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
                jsonPath("$.featured", `is`(true))
                jsonPath("$.pinned", `is`(false))
                jsonPath("$.media.length()", `is`(2))
                jsonPath("$.media[0].objectKey", `is`("tmp/1d61b8be-46d4-4131-b9e5-5c30515c58b4.jpg"))
                jsonPath("$.media[0].sortOrder", `is`(0))
                jsonPath("$.media[1].sortOrder", `is`(1))
            }
            .andReturn()
            .response
            .contentAsString
        val response = jsonMapper.readValue(responseBody, object : TypeReference<PostResponse>() {})
        response.featured shouldBe true
        response.pinned shouldBe false
        response.media shouldHaveSize 2
        response.media[0].mediaType shouldBe PostMediaType.IMAGE
        val savedPost = postRepository.findById(response.id).orElseThrow()
        savedPost.featured shouldBe true
        savedPost.pinned shouldBe false
    }

    afterSpec {
        postRepository.deleteAll()
    }
})
