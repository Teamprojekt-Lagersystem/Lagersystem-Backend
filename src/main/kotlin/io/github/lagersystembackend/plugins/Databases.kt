package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Spaces
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/postgres",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Spaces, Products)
    }
}
