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

            if (depthParam == null)
                return@get call.respond(
                    ApiResponse.Success(
                        "Listing every storage",
                        storageRepository.getStorages().filter { it.parentId == null }
                            .map { it.toNetworkStorage() })
                )

            if (depth == null || depth < 0)
                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse.Error("Invalid 'depth' parameter '$depthParam'. It must be a positive integer.")
                )

            call.respond(
                ApiResponse.Success(
                    "Listing every storage",
                    storageRepository.getStorages().filter { it.parentId == null }
                        .map { it.toNetworkStorage(maxDepth = depth) })
            )
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))

                val storage = storageRepository.getStorage(id)
                storage ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error("Storage not found"))

                val depthParam = call.request.queryParameters["depth"]
                val depth = depthParam?.toIntOrNull()

                if (depthParam == null)
                    return@get call.respond(
                        ApiResponse.Success(
                            "Storage found: $id",
                            storage.toNetworkStorage()
                        )
                    )

                if (depth == null || depth < 0)
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("Invalid 'depth' parameter '$depthParam'. It must be a positive integer.")
                    )

                call.respond(ApiResponse.Success("Storage found: $id", storage.toNetworkStorage(maxDepth = depth)))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error("Invalid UUID"))

                val deletedStorage = storageRepository.deleteStorage(id)
                deletedStorage ?: return@delete call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse.Error("Storage not found")
                )

                call.respond(ApiResponse.Success("Storage deleted: $id", deletedStorage.toNetworkStorage()))
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

                    storageRepository.getStorage(it)?.id ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error("Parent storage with ID $it not found")
                    )
                }
                storageRepository.createStorage(name, description, resolvedParentId)
            }

            call.respond(
                HttpStatusCode.Created,
                ApiResponse.Success("Storage created: ${createdStorage.id}", createdStorage.toNetworkStorage())
            )
        }
    }
}