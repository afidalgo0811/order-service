package com.afidalgo.orderservice.book

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import shared.library.order.Book

@Component
class BookClient(val webClient: WebClient) {

  fun getBookByIsbn(isbn: String) =
      webClient.get().uri(BookRoute.BOOKS_ROOT_API + isbn).retrieve().bodyToMono(Book::class.java)
}
