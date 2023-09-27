package com.afidalgo.orderservice.book

import com.afidalgo.orderservice.config.ClientProperties
import java.time.Duration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.*
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import shared.library.order.Book

@Component
class BookClient(val webClient: WebClient, val clientProperties: ClientProperties) {

  fun getBookByIsbn(isbn: String) =
      webClient
          .get()
          .uri(BookRoute.BOOKS_ROOT_API + isbn)
          .retrieve()
          .bodyToMono(Book::class.java)
          .timeout(Duration.ofSeconds(clientProperties.timeoutValue.toLong()), Mono.empty())
          .onErrorResume(NotFound::class.java) { Mono.empty() }
          .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
          .onErrorResume(Exception::class.java) { Mono.empty() }
}
