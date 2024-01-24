package com.afidalgo.orderservice.order.domain

import com.afidalgo.orderservice.book.BookClient
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import shared.library.order.Book
import shared.library.order.Order
import shared.library.order.OrderAcceptedMessage
import shared.library.order.OrderDispatchedMessage
import shared.library.order.OrderStatus

@Service
class OrderService(
    val orderRepository: OrderRepository,
    val bookClient: BookClient,
    val streamBridge: StreamBridge,
) {
  private val logger = LoggerFactory.getLogger(OrderService::class.java)

  fun getAllOrders(userId: String): Flux<Order> {
    return orderRepository.findAllByCreatedBy(userId)
  }

  @Transactional
  fun submitOrder(isbn: String, quantity: Int): Mono<Order> =
      bookClient
          .getBookByIsbn(isbn)
          .map { book -> buildAcceptedOrder(book, quantity) }
          .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
          .flatMap(orderRepository::save)
          .doOnNext(this::publishOrderAcceptedEvent)

  fun consumerOrderDispatchedEvent(flux: Flux<OrderDispatchedMessage>): Flux<Order> {
    return flux
        .flatMap { message -> orderRepository.findById(message.orderId) }
        .map { order -> buildDispatchedOrder(order) }
        .flatMap(orderRepository::save)
  }

  private fun publishOrderAcceptedEvent(order: Order) {
    if (order.status != OrderStatus.ACCEPTED) {
      return
    }
    val orderAcceptedMessage = order.id?.let { OrderAcceptedMessage(orderId = it) }
    logger.info("Sending order accepted event with id: ${order.id}")
    val result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage)
    logger.info("Result of sending data for order with id ${order.id}: $result")
  }

  companion object {
    fun buildRejectedOrder(bookIsbn: String, quantity: Int) =
        Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED)

    fun buildAcceptedOrder(book: Book, quantity: Int) =
        Order.of(
            book.isbn, "${book.title}-${book.author}", book.price, quantity, OrderStatus.ACCEPTED)

    fun buildDispatchedOrder(existingOrder: Order) =
        Order.of(
            existingOrder.id,
            existingOrder.bookIsbn,
            existingOrder.bookName,
            existingOrder.bookPrice,
            existingOrder.quantity,
            OrderStatus.DISPATCHED,
            existingOrder.createdDate,
            existingOrder.lastModifiedDate,
            existingOrder.createdBy,
            existingOrder.lastModifiedBy,
            existingOrder.version)
  }
}
