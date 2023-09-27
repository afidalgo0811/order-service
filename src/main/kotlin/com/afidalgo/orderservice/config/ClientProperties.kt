package com.afidalgo.orderservice.config

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polar")
class ClientProperties {
  @field:NotNull lateinit var catalogServiceUri: URI

  @field:NotNull @field:Positive var timeoutValue: Int = 1
}
