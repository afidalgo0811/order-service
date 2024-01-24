package com.afidalgo.orderservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.security.core.context.ReactiveSecurityContextHolder

@Configuration
@EnableR2dbcAuditing
class DataConfig {
  @Bean
  fun auditorAware(): ReactiveAuditorAware<String> {
    return ReactiveAuditorAware<String> {
      ReactiveSecurityContextHolder.getContext()
          .map { it.authentication }
          .filter { it.isAuthenticated }
          .map { it.name }
    }
  }
}
