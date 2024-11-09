package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.productRoutes(productRepository: ProductRepository) {
    route("/products") {
        get { call.respond(
            ApiResponse.Success("Listing every product", productRepository.getProducts().map { it.toNetworkProduct() })) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID()) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID")
                    )
                }

                val product = productRepository.getProduct(id)
                product ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Product not found"))

                call.respond(ApiResponse.Success("Found product: ${id}", product.toNetworkProduct()))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))

                val deletedProduct = productRepository.deleteProduct(id)
                deletedProduct ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Product not found"))
                // ToDo: Error 406 bei Versuch folgendes Response abzuschicken
                call.respond(ApiResponse.Success("Deleted product: ${id}", deletedProduct.toNetworkProduct()))
            }
        }
        post {
            val addProductNetworkRequest = runCatching { call.receive<AddProductNetworkRequest>() }.getOrNull()
            addProductNetworkRequest ?: return@post call.respond(HttpStatusCode.BadRequest,
                ApiResponse.Error("Body should be Serialized AddProductNetworkRequest"))

            val createdProduct = addProductNetworkRequest.run {
                if (!spaceId.isUUID())
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))


                if (!productRepository.spaceExists(spaceId))
                    return@post call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Specified space not found"))

                productRepository.createProduct(name, price, description, spaceId) }

            call.respond(HttpStatusCode.Created,
                ApiResponse.Success("Created product: ${createdProduct.id}", createdProduct.toNetworkProduct())) }
    }
}