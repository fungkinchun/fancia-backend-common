package com.fancia.backend.common

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication

@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@EntityScan(
    basePackages = [
        "com.fancia.backend.common.core",
        "com.fancia.backend.shared.common.core.entity"
    ]
)
@SpringBootApplication
class CommonApplication

fun main(args: Array<String>) {
    runApplication<CommonApplication>(*args)
}
