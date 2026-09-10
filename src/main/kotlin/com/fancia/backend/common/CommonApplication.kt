package com.fancia.backend.common

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@EntityScan(
    basePackages = [
        "com.fancia.backend.common",
        "com.fancia.backend.shared.common",
        "com.fancia.backend.shared.upload"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "com.fancia.backend.common",
        "com.fancia.backend.shared.common.post.core.repository",
        "com.fancia.backend.shared.common.tag.core.repository",
        "com.fancia.backend.shared.common.rating.core.repository",
    ]
)
@SpringBootApplication
@ConfigurationPropertiesScan
class CommonApplication

fun main(args: Array<String>) {
    runApplication<CommonApplication>(*args)
}
