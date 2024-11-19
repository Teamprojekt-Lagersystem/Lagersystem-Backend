package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
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
                        HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_PRODUCT)
                    )
                }

                val product = productRepository.getProduct(id)
                product ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.PRODUCT_NOT_FOUND))

                call.respond(ApiResponse.Success("Found product: ${id}", product.toNetworkProduct()))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_PRODUCT))

                val deletedProduct = productRepository.deleteProduct(id)
                deletedProduct ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.PRODUCT_NOT_FOUND))

                call.respond(ApiResponse.Success("Deleted product: ${id}", deletedProduct.toNetworkProduct()))
            }
        }
        post {
            val addProductNetworkRequest = runCatching { call.receive<AddProductNetworkRequest>() }.getOrNull()
            addProductNetworkRequest ?: return@post call.respond(HttpStatusCode.BadRequest,
                ApiResponse.Error(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT))

            val createdProduct = addProductNetworkRequest.run {
                if (!spaceId.isUUID())
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_SPACE))


                if (!productRepository.spaceExists(spaceId))
                    return@post call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.SPACE_NOT_FOUND))

                productRepository.createProduct(name, price, description, spaceId) }

            call.respond(HttpStatusCode.Created,
                ApiResponse.Success("Created product: ${createdProduct.id}", createdProduct.toNetworkProduct())) }
    }
}