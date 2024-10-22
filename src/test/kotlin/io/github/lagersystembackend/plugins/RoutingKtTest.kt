package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.testing.FakeSomeRepository
import io.github.lagersystembackend.testing.Item
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

var someRepository = FakeSomeRepository()

fun ApplicationTestBuilder.createEnvironment() {
    someRepository = FakeSomeRepository()
    application {
        configureHTTP()
        configureSerialization()
        configureRouting(someRepository)
    }
}
//TODO: Some tests are one sided and should test more cases
class RoutingKtTest {

    @Test
    fun testGetItems() = testApplication {
        createEnvironment()
        val items = listOf(
            Item("item1"),
            Item("item2"),
            Item("item3")
        )
        someRepository.addItem(items[0])
        someRepository.addItem(items[1])
        someRepository.addItem(items[2])


        client.get("/items").apply {
            assertEquals(bodyAsText(), Json.encodeToString(items))
        }
    }

    @Test
    fun testCreateItem() = testApplication {

        createEnvironment()

        client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(Item("item4")))
        }.apply {
            assertEquals(someRepository.allItems().size, 1)
        }
    }

    @Test
    fun testGetItem() = testApplication {
        createEnvironment()
        someRepository.addItem(Item("item1"))
        client.get("/items/item1").apply {
            assertEquals(Json.decodeFromString<Item>(bodyAsText()).name, "item1")
        }
    }

    @Test
    fun testDeleteItem() = testApplication {
        createEnvironment()
        someRepository.addItem(Item("test1"))
        assertEquals(someRepository.allItems().size, 1)

        client.delete("/items?name=test1").apply {
            assertEquals(someRepository.allItems().size, 0)
        }
    }
}