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
    configureRouting(Di.someRepository)
}

private object Di {
    val someRepository: SomeRepository = FakeSomeRepository()
}