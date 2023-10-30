package com.afidalgo.orderservice.order.domain

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import shared.library.order.Order

interface OrderRepository : ReactiveCrudRepository<Order, Long> {}
