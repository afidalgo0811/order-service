package com.afidalgo.orderservice

import com.afidalgo.orderservice.book.BookClient
import com.afidalgo.orderservice.config.ClientProperties
import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import shared.library.order.Book

@ActiveProfiles("test")
class BookClientTests() {

  private val mockWebServer: MockWebServer = MockWebServer()
  private lateinit var bookClient: BookClient
  private var clientProperties: ClientProperties = ClientProperties()

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    mockWebServer.start()
    val webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build()
    bookClient = BookClient(webClient, clientProperties)
  }

  @AfterEach
  @Throws(IOException::class)
  fun clean() {
    mockWebServer.shutdown()
  }

  @Test
  fun whenBookExistsThenReturnBook() {
    val bookIsbn = "1234567890"
    val mockResponse =
        MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(
                """
                {   "isbn":%s,
                    "title":"Title",
                    "author":"Author",
                    "price":9.90,
                    "publisher": "PolarSophia"
                }
                """
                    .format(bookIsbn))
    mockWebServer.enqueue(mockResponse)
    val book: Mono<Book> = bookClient.getBookByIsbn(bookIsbn)
    StepVerifier.create(book).expectNextMatches { b -> b.isbn == bookIsbn }.verifyComplete()
  }
}
