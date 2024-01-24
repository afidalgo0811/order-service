package com.afidalgo.orderservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache

@EnableWebFluxSecurity
@Configuration
class SecurityConfig {
  @Bean
  @Throws(Exception::class)
  fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http
        .authorizeExchange { it.anyExchange().authenticated() }
        .oauth2ResourceServer { oauth2 -> oauth2.jwt {} }
        .requestCache { it.requestCache(NoOpServerRequestCache.getInstance()) }
        .csrf { it: ServerHttpSecurity.CsrfSpec? -> it?.disable() }
        .build()
  }
}
