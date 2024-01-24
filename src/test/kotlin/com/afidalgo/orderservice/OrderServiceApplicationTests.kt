package com.afidalgo.orderservice

import com.afidalgo.orderservice.book.BookClient
import com.afidalgo.orderservice.order.web.OrderRequest
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import dasniko.testcontainers.keycloak.KeycloakContainer
import java.io.IOException
import java.util.stream.Collectors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import shared.library.order.Book
import shared.library.order.Order
import shared.library.order.OrderAcceptedMessage
import shared.library.order.OrderStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration::class)
@Testcontainers
class OrderServiceApplicationTests(
    @Autowired private val webTestClient: WebTestClient,
) {
  @MockBean private lateinit var bookClient: BookClient

  @Autowired private lateinit var output: OutputDestination

  @Autowired private lateinit var objectMapper: ObjectMapper

  companion object {
    @Container val postgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:14.4"))
    private var bjornTokens: KeycloakToken? = null
    private var isabelleTokens: KeycloakToken? = null

    @JvmStatic
    @Container
    private val keycloakContainer =
        KeycloakContainer("quay.io/keycloak/keycloak:19.0")
            .withRealmImportFile("test-realm-config.json")

    @JvmStatic
    private fun r2dbcUrl() =
        String.format(
            "r2dbc:postgresql://%s:%s/%s",
            postgreSQLContainer.host,
            postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgreSQLContainer.databaseName)

    private fun authenticateWith(
        username: String,
        password: String,
        webClient: WebClient,
    ): KeycloakToken? =
        webClient
            .post()
            .body(
                BodyInserters.fromFormData("grant_type", "password")
                    .with("client_id", "polar-test")
                    .with("username", username)
                    .with("password", password))
            .retrieve()
            .bodyToMono(KeycloakToken::class.java)
            .block()

    @JvmStatic
    @BeforeAll
    fun setUp() {
      val webClient =
          WebClient.builder()
              .baseUrl(
                  keycloakContainer.authServerUrl +
                      "realms/PolarBookshop/protocol/openid-connect/token")
              .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
              .build()
      isabelleTokens = authenticateWith("isabelle", "password", webClient)
      bjornTokens = authenticateWith("bjorn", "password", webClient)
    }

    @JvmStatic
    @DynamicPropertySource
    fun postgresqlProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl)
      registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername)
      registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword)
      registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl)
      registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") {
        keycloakContainer.authServerUrl + "realms/PolarBookshop"
      }
    }
  }

  @Test
  fun whenGetOrdersThenReturn() {
    val bookIsbn = "1234567893"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 1)
    val expectedOrder: Order? =
        webTestClient
            .post()
            .uri("/orders")
            .headers { bjornTokens?.let { it1 -> it.setBearerAuth(it1.accessToken) } }
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order::class.java)
            .returnResult()
            .responseBody
    assertThat(expectedOrder).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
        .isEqualTo(expectedOrder?.id?.let { OrderAcceptedMessage(it) })
    webTestClient
        .get()
        .uri("/orders")
        .headers { bjornTokens?.let { it1 -> it.setBearerAuth(it1.accessToken) } }
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBodyList(Order::class.java)
        .value<WebTestClient.ListBodySpec<Order>> { orders ->
          assertThat(orders.stream().filter { order -> order.bookIsbn == bookIsbn }.findAny())
              .isNotEmpty
        }
  }

  @Test
  fun whenPostRequestAndBookExistsThenOrderAccepted() {
    val bookIsbn = "1234567899"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 3)
    val createdOrder: Order? =
        webTestClient
            .post()
            .uri("/orders")
            .headers { isabelleTokens?.let { it1 -> it.setBearerAuth(it1.accessToken) } }
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order::class.java)
            .returnResult()
            .responseBody
    assertThat(createdOrder).isNotNull()
    assertThat(createdOrder?.bookIsbn).isEqualTo(orderRequest.isbn)
    assertThat(createdOrder?.quantity).isEqualTo(orderRequest.quantity)
    assertThat(createdOrder?.bookName).isEqualTo(book.title + "-" + book.author)
    assertThat(createdOrder?.bookPrice).isEqualTo(book.price)
    assertThat(createdOrder?.status).isEqualTo(OrderStatus.ACCEPTED)

    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
        .isEqualTo(createdOrder?.id?.let { OrderAcceptedMessage(it) })
  }

  @Test
  fun whenPostRequestAndBookNotExistsThenOrderRejected() {
    val bookIsbn = "1234567894"
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty())
    val orderRequest = OrderRequest(bookIsbn, 3)
    val createdOrder: Order? =
        webTestClient
            .post()
            .uri("/orders")
            .headers { bjornTokens?.let { it1 -> it.setBearerAuth(it1.accessToken) } }
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order::class.java)
            .returnResult()
            .responseBody
    assertThat(createdOrder).isNotNull()
    assertThat(createdOrder?.bookIsbn).isEqualTo(orderRequest.isbn)
    assertThat(createdOrder?.quantity).isEqualTo(orderRequest.quantity)
    assertThat(createdOrder?.status).isEqualTo(OrderStatus.REJECTED)
  }

  @Test
  @Throws(IOException::class)
  fun whenGetOrdersForAnotherUserThenNotReturned() {
    val bookIsbn = "1234567899"
    val book = Book(bookIsbn, "Title", "Author", 9.90)
    given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book))
    val orderRequest = OrderRequest(bookIsbn, 1)
    val orderByBjorn =
        webTestClient
            .post()
            .uri("/orders")
            .headers { headers: HttpHeaders -> headers.setBearerAuth(bjornTokens!!.accessToken) }
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order::class.java)
            .returnResult()
            .responseBody
    assertThat(orderByBjorn).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
        .isEqualTo(OrderAcceptedMessage(orderByBjorn!!.id!!))
    val orderByIsabelle =
        webTestClient
            .post()
            .uri("/orders")
            .headers { headers: HttpHeaders -> headers.setBearerAuth(isabelleTokens!!.accessToken) }
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order::class.java)
            .returnResult()
            .responseBody
    assertThat(orderByIsabelle).isNotNull()
    assertThat(objectMapper.readValue(output.receive().payload, OrderAcceptedMessage::class.java))
        .isEqualTo(OrderAcceptedMessage(orderByIsabelle!!.id!!))
    webTestClient
        .get()
        .uri("/orders")
        .headers { headers: HttpHeaders -> headers.setBearerAuth(bjornTokens!!.accessToken) }
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBodyList(Order::class.java)
        .value<WebTestClient.ListBodySpec<Order>> { orders ->
          val orderIds: MutableList<Long?>? =
              orders.stream().map(Order::id).collect(Collectors.toList())
          assertThat(orderIds).contains(orderByBjorn.id)
          assertThat(orderIds).doesNotContain(orderByIsabelle.id)
        }
  }
}

// should be moved to a helper class in the future
data class KeycloakToken(@JsonProperty("access_token") val accessToken: String) {
  companion object {
    @JsonCreator fun create(accessToken: String) = KeycloakToken(accessToken)
  }
}
