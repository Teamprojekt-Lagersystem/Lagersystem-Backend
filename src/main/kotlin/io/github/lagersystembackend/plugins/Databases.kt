package io.github.lagersystembackend.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/postgres",
        driver = "org.h2.Driver",
        user = "postgres",
        password = "password"
    )
}
