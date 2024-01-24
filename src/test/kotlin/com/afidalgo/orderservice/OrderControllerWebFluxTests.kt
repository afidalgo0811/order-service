package com.afidalgo.orderservice

import com.afidalgo.orderservice.config.SecurityConfig
import com.afidalgo.orderservice.order.domain.OrderService
import com.afidalgo.orderservice.order.web.OrderController
import com.afidalgo.orderservice.order.web.OrderRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import shared.library.order.Order
import shared.library.order.OrderStatus

@WebFluxTest(OrderController::class)
@Import(SecurityConfig::class)
class OrderControllerWebFluxTests(
    @Autowired val webClient: WebTestClient,
) {

  @MockBean private lateinit var orderService: OrderService

  @MockBean private lateinit var reactiveJWTDecoder: ReactiveJwtDecoder

  @Test
  fun whenBookNotAvailableThenRejectOrder() {
    val orderRequest = OrderRequest("1234567890", 3)
    val expectedOrder: Order =
        OrderService.buildRejectedOrder(orderRequest.isbn, orderRequest.quantity)
    given(orderService.submitOrder(orderRequest.isbn, orderRequest.quantity))
        .willReturn(Mono.just(expectedOrder))
    webClient
        .mutateWith(
            SecurityMockServerConfigurers.mockJwt()
                .authorities(SimpleGrantedAuthority("ROLE_customer")))
        .post()
        .uri("/orders")
        .bodyValue(orderRequest)
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(Order::class.java)
        .value { actualOrder ->
          assertThat(actualOrder).isNotNull()
          assertThat(actualOrder.status).isEqualTo(OrderStatus.REJECTED)
        }
  }
}
