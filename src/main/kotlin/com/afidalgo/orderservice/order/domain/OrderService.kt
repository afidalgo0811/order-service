package com.afidalgo.orderservice.order.domain

import com.afidalgo.orderservice.book.BookClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import shared.library.order.Book

@Service
class OrderService(val orderRepository: OrderRepository, val bookClient: BookClient) {

  fun getAllOrders(): Flux<Order> {
    return orderRepository.findAll()
  }

  fun submitOrder(isbn: String, quantity: Int): Mono<Order> =
      bookClient
          .getBookByIsbn(isbn)
          .map { book -> buildAcceptedOrder(book, quantity) }
          .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
          .flatMap(orderRepository::save)

  companion object {
    fun buildRejectedOrder(bookIsbn: String, quantity: Int) =
        Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED)

    fun buildAcceptedOrder(book: Book, quantity: Int) =
        Order.of(
            book.isbn, "${book.title}-${book.author}", book.price, quantity, OrderStatus.ACCEPTED)
  }
}