package com.afidalgo.orderservice

import com.afidalgo.orderservice.config.DataConfig
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@DataR2dbcTest @Import(DataConfig::class) @Testcontainers class OrderRepositoryR2dbcTests {}
