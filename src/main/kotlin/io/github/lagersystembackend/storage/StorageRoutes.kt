package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.isUUID
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.storageRoutes(storageRepository: StorageRepository) {
    route("/storages") {
        get {
            val depthParam = call.request.queryParameters["depth"]
            val depth = depthParam?.toIntOrNull()

            if (depthParam != null && depth == null) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Invalid 'depth' parameter. It must be a positive integer.")
                )
            } else if (depth != null && depth < 0) {
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("'depth' must be a non-negative integer.")
                )
            } else if (depthParam == null ) {
                return@get call.respond(
                    ApiResponse.Success("Listing every storage", storageRepository.getStorages(null).map { it.toNetworkStorage() }))
            }

            return@get call.respond(
                ApiResponse.Success("Listing every storage", storageRepository.getStorages(depth).map { it.toNetworkStorage() }))
        }



        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))

                val depthParam = call.request.queryParameters["depth"]
                val depth = depthParam?.toIntOrNull()

                if (depthParam != null && depth == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("Invalid 'depth' parameter. It must be a positive integer.")
                    )
                } else if (depth != null && depth < 0) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("'depth' must be a non-negative integer.")
                    )
                } else if (depthParam == null ) {
                    val storage = storageRepository.getStorage(id, null)
                    storage ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Storage not found"))

                    call.respond(ApiResponse.Success("Storage found: ${id}", storage.toNetworkStorage()))
                }

                val storage = storageRepository.getStorage(id, depth)
                storage ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Storage not found"))

                call.respond(ApiResponse.Success("Storage found: ${id}", storage.toNetworkStorage()))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))

                val deletedStorage = storageRepository.deleteStorage(id)
                deletedStorage ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Storage not found"))

                call.respond(ApiResponse.Success("Storage deleted: ${id}", deletedStorage.toNetworkStorage()))
            }
        }
        post {
            val addStorageNetworkRequest = runCatching { call.receive<AddStorageNetworkRequest>() }.getOrNull()
            addStorageNetworkRequest ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.Error("Body should be Serialized AddStorageNetworkRequest")
            )

            val createdStorage = addStorageNetworkRequest.run {

                val resolvedParentId = parentId?.let {
                    if (!parentId.isUUID()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))
                    }

                    storageRepository.getStorage(it, null)?.id ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("Parent storage with ID $it not found")
                    )
                }
                storageRepository.createStorage(name, description, resolvedParentId) }

            call.respond(HttpStatusCode.Created,
                ApiResponse.Success("Storage created: ${createdStorage.id}", createdStorage.toNetworkStorage()))
        }

    }
}