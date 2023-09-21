package com.afidalgo.orderservice.config

import jakarta.validation.constraints.NotNull
import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polar")
data class ClientProperties(
    @field:NotNull val catalogServiceUri: URI,
)
