package com.fancia.backend.common

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestConfig::class)
class CommonApplicationTests {
    @Test
    fun contextLoads() {
    }
}
