package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.productRoutes(productRepository: ProductRepository) {
    route("/products") {
        get { call.respond(productRepository.getProducts().map { it.toNetworkProduct() }) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                val product = productRepository.getProduct(id)
                product ?: return@get call.respondText("Product not found", status = HttpStatusCode.NotFound)

                call.respond(product.toNetworkProduct())
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                if (!productRepository.deleteProduct(id))
                    return@delete call.respondText("Product not found", status = HttpStatusCode.NotFound)

                call.respondText("Product deleted")
            }
        }
        post {
            val addProductNetworkRequest = runCatching { call.receive<AddProductNetworkRequest>() }.getOrNull()
            addProductNetworkRequest ?: return@post call.respond(HttpStatusCode.BadRequest, "Body should be Serialized AddProductNetworkRequest")

            // TODO: Check if space exists
            addProductNetworkRequest.run { productRepository.createProduct(name, price, description, spaceId) }
            //TODO: should return productid
            call.respond(HttpStatusCode.Created, "Product created") }
    }
}