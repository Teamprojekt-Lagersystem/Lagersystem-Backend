package io.github.lagersystembackend

import io.github.lagersystembackend.plugins.*
import io.github.lagersystembackend.testing.FakeSomeRepository
import io.github.lagersystembackend.testing.SomeRepository
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureHTTP()
    configureRouting(DependencyProvider.someRepository)
}

private object DependencyProvider {
    val someRepository: SomeRepository = FakeSomeRepository()
}