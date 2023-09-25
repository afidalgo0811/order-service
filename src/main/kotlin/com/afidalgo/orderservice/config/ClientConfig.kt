package com.afidalgo.orderservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ClientConfig {
  @Bean
  fun webClient(clientProperties: ClientProperties, webClientBuilder: WebClient.Builder) =
      webClientBuilder.baseUrl(clientProperties.catalogServiceUri.toString()).build()
}
