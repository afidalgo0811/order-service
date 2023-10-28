package com.afidalgo.orderservice.event

import com.afidalgo.orderservice.order.domain.OrderService
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import shared.library.order.OrderDispatchedMessage

@Configuration
class OrderFunctions {
  companion object {
    private val logger = LoggerFactory.getLogger(OrderFunctions::class.java)
  }

  @Bean
  fun dispatchOrder(orderService: OrderService): Consumer<Flux<OrderDispatchedMessage>> {
    return Consumer { flux ->
      orderService
          .consumerOrderDispatchedEvent(flux)
          .doOnNext { order -> logger.info("The order with id ${order.id} is dispatched") }
          .subscribe()
    }
  }
}
