package io.github.lagersystembackend

import io.github.lagersystembackend.plugins.*
import io.github.lagersystembackend.product.PostgresProductRepository
import io.github.lagersystembackend.product.ProductRepository
import io.github.lagersystembackend.space.PostgresSpaceRepository
import io.github.lagersystembackend.space.SpaceRepository
import io.github.lagersystembackend.storage.PostgresStorageRepository
import io.github.lagersystembackend.storage.StorageRepository
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureHTTP()
    configureRouting(DependencyProvider.productRepository, DependencyProvider.spaceRepository, DependencyProvider.storageRepository)
}

private object DependencyProvider {
    val productRepository: ProductRepository = PostgresProductRepository()
    val spaceRepository: SpaceRepository = PostgresSpaceRepository()
    val storageRepository: StorageRepository = PostgresStorageRepository()
}