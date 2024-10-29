package io.github.lagersystembackend.product

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
                val id = call.parameters["id"]
                if (id == null) {
                    call.respondText("Missing parameter 'id'", status = HttpStatusCode.BadRequest)
                    return@get
                }
                val product = productRepository.getProduct(id)
                if (product == null) {
                    call.respondText("Product not found", status = HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(product.toNetworkProduct())
            }

            delete {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respondText("Missing parameter 'name'", status = HttpStatusCode.BadRequest)
                    return@delete
                }
                if (!productRepository.deleteProduct(id)) {
                    call.respondText("Product not found", status = HttpStatusCode.NotFound)
                    return@delete
                }
                call.respondText("Product deleted")
            }
        }
        post {
            call.receive<AddProductNetworkRequest>().run {
                productRepository.createProduct(name, price, description, spaceId)
            }
            call.respond(HttpStatusCode.Created, "Product created") }
    }
}