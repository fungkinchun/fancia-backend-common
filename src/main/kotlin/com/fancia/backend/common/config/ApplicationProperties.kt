package com.fancia.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class ApplicationProperties {
    var applicationName: String? = null
    var baseUrl: String? = null
    var loginPageUrl: String? = null
}
