package com.afidalgo.orderservice

import com.afidalgo.orderservice.config.DataConfig
import com.afidalgo.orderservice.order.domain.OrderRepository
import com.afidalgo.orderservice.order.domain.OrderService
import com.afidalgo.orderservice.order.domain.OrderStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.test.StepVerifier
import shared.library.order.Book

@DataR2dbcTest
@Import(DataConfig::class)
@Testcontainers
class OrderRepositoryR2dbcTests(@Autowired val orderRepository: OrderRepository) {
  companion object {
    @Container val postgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:14.4"))

    @JvmStatic
    private fun r2dbcUrl() =
        String.format(
            "r2dbc:postgresql://%s:%s/%s",
            postgreSQLContainer.host,
            postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgreSQLContainer.databaseName)

    @JvmStatic
    @DynamicPropertySource
    fun postgresqlProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl)
      registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername)
      registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword)
      registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl)
    }
  }

  @Test
  fun createRejectedOrder() {
    val rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3)
    StepVerifier.create(orderRepository.save(rejectedOrder))
        .expectNextMatches { order -> order.status == OrderStatus.REJECTED }
        .verifyComplete()
  }

  @Test
  fun createAcceptedOrder() {
    val acceptedOrder =
        OrderService.buildAcceptedOrder(Book("1234567890", "Title", "Author", 9.0), 3)
    StepVerifier.create(orderRepository.save(acceptedOrder))
        .expectNextMatches { order -> order.status == OrderStatus.ACCEPTED }
        .verifyComplete()
  }

  @Test
  fun findOrderByIdWhenNotExisting() {
    StepVerifier.create(orderRepository.findById(394L)).expectNextCount(0).verifyComplete()
  }
}
