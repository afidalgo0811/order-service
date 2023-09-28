package com.afidalgo.orderservice

import com.afidalgo.orderservice.order.web.OrderRequest

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class OrderRequestValidationTests {

    @Test
    fun whenAllFieldsCorrectThenValidationSucceeds() {
        val orderRequest = OrderRequest("1234567890", 1)
        val violations: Set<ConstraintViolation<OrderRequest>> = validator.validate(orderRequest)
        assertThat(violations).isEmpty()
    }

    @Test
    fun whenIsbnNotDefinedThenValidationFails() {
        val orderRequest = OrderRequest("", 1)
        val violations: Set<ConstraintViolation<OrderRequest>> = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        assertThat(violations.iterator().next().message)
            .isEqualTo("The book ISBN must be defined.")
    }

    @Test
    fun whenQuantityIsNotDefinedThenValidationFails() {
        val orderRequest = OrderRequest("1234567890", -1)
        val violations: Set<ConstraintViolation<OrderRequest>> = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        assertThat(violations.iterator().next().message)
            .isEqualTo("You must order at least 1 item.")
    }

    @Test
    fun whenQuantityIsLowerThanMinThenValidationFails() {
        val orderRequest = OrderRequest("1234567890", 0)
        val violations: Set<ConstraintViolation<OrderRequest>> = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        assertThat(violations.iterator().next().message)
            .isEqualTo("You must order at least 1 item.")
    }

    @Test
    fun whenQuantityIsGreaterThanMaxThenValidationFails() {
        val orderRequest = OrderRequest("1234567890", 7)
        val violations: Set<ConstraintViolation<OrderRequest>> = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        assertThat(violations.iterator().next().message)
            .isEqualTo("You cannot order more than 5 items.")
    }

    companion object {

        private lateinit var validator: Validator

        @JvmStatic
        @BeforeAll
        fun setUp() {
            val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
            validator = factory.validator
        }
    }
}