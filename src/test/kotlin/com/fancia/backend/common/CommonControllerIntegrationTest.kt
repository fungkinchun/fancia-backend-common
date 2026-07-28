package com.fancia.backend.common

import com.fancia.backend.common.tag.core.repository.TagRepository
import com.fancia.backend.common.tag.mapper.toEntity
import com.fancia.backend.shared.common.tag.core.dto.TagResponse
import com.fancia.backend.shared.common.tag.core.entity.Tag
import com.fancia.backend.shared.common.tag.core.enums.TagType
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
) : FunSpec({
    test("should create new tags") {
        val testUserId = UUID.randomUUID()
        val response = mockMvc
            .post("/api/tags") {
                with(jwt().jwt { it.claim("userId", testUserId) })
                val requestBody = mapOf(
                    "tags" to listOf(
                        mapOf("name" to "good", "type" to "INTEREST"),
                        mapOf("name" to "bad", "type" to "INTEREST"),
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
                jsonPath("$.content[0].type", `is`("INTEREST"))
                jsonPath("$.content[1].name", `is`("bad"))
                jsonPath("$.content[1].type", `is`("INTEREST"))
            }
        val createdTags = response.toTags(jsonMapper)
        createdTags.forEach { createdTag ->
            val found = tagRepository.existsByNameAndType(createdTag.name, createdTag.type)
            found shouldBe true
        }
    }

    test("should search similar tags by type") {
        mockMvc
            .post("/api/tags") {
                with(jwt().jwt { it.claim("userId", UUID.randomUUID()) })
                val requestBody = mapOf(
                    "tags" to listOf(
                        mapOf("name" to "good", "type" to "INTEREST"),
                        mapOf("name" to "bad", "type" to "INTEREST"),
                    )
                )
                content = jsonMapper.writeValueAsString(requestBody)
                contentType = APPLICATION_JSON
                accept = APPLICATION_JSON
            }
            .andExpect { status { isCreated() } }

        mockMvc
            .get("/api/tags?type=INTEREST&search=goo&search=ba&page=0&size=3") {
                accept = APPLICATION_JSON
            }
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements", `is`(2))
                jsonPath("$.content[0].name", `is`("good"))
                jsonPath("$.content[0].type", `is`("INTEREST"))
                jsonPath("$.content[1].name", `is`("bad"))
                jsonPath("$.content[1].type", `is`("INTEREST"))
            }
    }

    test("should not create tags when searching") {
        mockMvc
            .get("/api/tags?type=INTEREST&search=nonexistent&page=0&size=3") {
                accept = APPLICATION_JSON
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.totalElements", `is`(0))
            }
        tagRepository.existsByNameAndType("nonexistent", TagType.INTEREST) shouldBe false
    }

    afterSpec {
        tagRepository.deleteAll()
    }
})

private fun ResultActionsDsl.toTags(
    jsonMapper: JsonMapper,
): List<Tag> = this.andReturn()
    .response
    .contentAsString
    .let {
        jsonMapper.readValue(it, object : TypeReference<Page<TagResponse>>() {})
            .content.map { it.toEntity() }
    }
