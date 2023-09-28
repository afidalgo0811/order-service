package com.afidalgo.orderservice

import com.afidalgo.orderservice.order.web.OrderRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester


@JsonTest
class OrderRequestJsonTests(
    @Autowired
    private val json: JacksonTester<OrderRequest>,
) {

    @Test
    @Throws(Exception::class)
    fun testDeserialize() {
        val content = """
                {
                    "isbn": "1234567890",
                    "quantity": 1
                }
                
                """.trimIndent()
        assertThat(json.parse(content))
            .usingRecursiveComparison().isEqualTo(OrderRequest("1234567890", 1))
    }
}