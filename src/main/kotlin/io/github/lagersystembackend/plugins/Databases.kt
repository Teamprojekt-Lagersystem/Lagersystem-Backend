package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Spaces
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDatabases(isTest: Boolean = false) {
    val port = if (isTest) "5434" else "5432"
    Database.connect(
        url = "jdbc:postgresql://localhost:${port}/postgres",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Spaces, Products, ProductAttributes)
    }
}