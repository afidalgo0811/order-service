package com.afidalgo.orderservice.order.domain

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import shared.library.order.Order

interface OrderRepository : ReactiveCrudRepository<Order, Long> {
  fun findAllByCreatedBy(userId: String): Flux<Order>
}
