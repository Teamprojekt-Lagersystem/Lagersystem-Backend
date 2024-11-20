package io.github.lagersystembackend.storage

import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Spaces
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

fun insertRootStorage() = transaction {
    StorageEntity.new {
        name = "Root"
        description = "Root storage"
    }
}

class PostgresStorageRepositoryTest {
    val sut = PostgresStorageRepository()


    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @Test
    fun `create Storage should return Storage`() = testApplication {
        val rootStorage = insertRootStorage()
        val expectedStorage =
            Storage("anyId", "Storage", "Storage description", emptyList(), rootStorage.id.toString(), emptyList())
        expectedStorage.run { sut.createStorage(name, description, parentId) }.apply {
            name shouldBe expectedStorage.name
            description shouldBe expectedStorage.description
            parentId shouldBe expectedStorage.parentId
            sut.getStorage(rootStorage.id.toString())!!.subStorages shouldContain this
        }
    }
}