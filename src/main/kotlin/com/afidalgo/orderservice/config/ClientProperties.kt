package com.afidalgo.orderservice.config

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polar")
data class ClientProperties(
    @field:NotNull val catalogServiceUri: URI,
    @field:NotNull @field:Positive val timeoutValue: Int,
)
