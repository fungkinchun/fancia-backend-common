package com.fancia.backend.common

import com.fancia.backend.common.core.entity.Tag
import com.fancia.backend.common.core.repository.TagRepository
import com.fancia.backend.common.mapper.TagMapper
import com.fancia.backend.shared.common.core.dto.TagResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.hamcrest.CoreMatchers.`is`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
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
class CommonControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val tagRepository: TagRepository,
    private val jsonMapper: JsonMapper,
    private val tagMapper: TagMapper
) : FunSpec({
    test("should create new tags") {
        val testUserId = UUID.randomUUID()
        val response = mockMvc
            .post("/api/tags") {
                with(jwt().jwt { it.claim("userId", testUserId) })
                val requestBody = mapOf(
                    "tags" to listOf(
                        mapOf("name" to "good"),
                        mapOf("name" to "bad")
                    )
                )
                content = jsonMapper.writeValueAsString(requestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andDo { print() }
            .andExpect {
                status { isCreated() }
                jsonPath("$.content[0].name", `is`("good"))
                jsonPath("$.content[1].name", `is`("bad"))
            }
        val createdTags = response.toTags(jsonMapper, tagMapper)
        createdTags.forEach { createdTag ->
            val found = tagRepository.existsByName(createdTag.name)
            found shouldBe true
        }
    }

    test("should list tags") {
        mockMvc
            .get("/api/tags?search=goo&search=ba&page=0&size=3") {
                accept = APPLICATION_JSON
            }
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements", `is`(2))
                jsonPath("$.content[0].name", `is`("good"))
                jsonPath("$.content[1].name", `is`("bad"))
            }
    }

    afterSpec {
        tagRepository.deleteAll()
    }
})

private fun ResultActionsDsl.toTags(
    jsonMapper: JsonMapper,
    tagMapper: TagMapper
): List<Tag> = this.andReturn()
    .response
    .contentAsString
    .let {
        jsonMapper.readValue(it, object : TypeReference<Page<TagResponse>>() {})
            .content.map(tagMapper::toBean)
    }