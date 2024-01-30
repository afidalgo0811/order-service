package com.afidalgo.orderservice.order.web

import com.afidalgo.orderservice.order.domain.OrderService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import shared.library.order.Order

@RestController
@RequestMapping("orders")
class OrderController(val orderService: OrderService) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping
  fun getAllOrders(@AuthenticationPrincipal jwt: Jwt): Flux<Order> {
    logger.info("Fetching all orders")
    return orderService.getAllOrders(jwt.subject)
  }

  @PostMapping
  fun submitOrder(@RequestBody @Valid orderRequest: OrderRequest): Mono<Order> {
    logger.info(
        "Order for {} copies of the book with ISBN {}", orderRequest.quantity, orderRequest.isbn)
    return orderService.submitOrder(orderRequest.isbn, orderRequest.quantity)
  }
}
