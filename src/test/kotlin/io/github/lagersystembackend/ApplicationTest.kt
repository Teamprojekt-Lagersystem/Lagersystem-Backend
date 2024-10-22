package io.github.lagersystembackend

import io.github.lagersystembackend.plugins.*
import io.github.lagersystembackend.testing.FakeSomeRepository
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting(FakeSomeRepository())
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
