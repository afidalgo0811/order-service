package com.afidalgo.orderservice.order.domain

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class OrderService(val orderRepository: OrderRepository) {

  fun getAllOrders(): Flux<Order> {
    return orderRepository.findAll()
  }

  fun submitOrder(isbn: String, quantity: Int): Mono<Order> {
    return Mono.just(buildRejectedOrder(isbn, quantity)).flatMap(orderRepository::save)
  }

  companion object {
    fun buildRejectedOrder(bookIsbn: String, quantity: Int): Order {
      return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED)
    }
  }
}
