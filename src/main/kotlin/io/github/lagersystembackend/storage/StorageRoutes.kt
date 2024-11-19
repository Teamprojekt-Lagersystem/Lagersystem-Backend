package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.github.lagersystembackend.common.ApiError
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.storageRoutes(storageRepository: StorageRepository) {
    route("/storages") {
        get { call.respond(
            ApiResponse.Success("Listing every storage", storageRepository.getStorages().map { it.toNetworkStorage() })) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_STORAGE))

                val storage = storageRepository.getStorage(id)
                storage ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.STORAGE_NOT_FOUND))

                call.respond(ApiResponse.Success("Storage found: ${id}", storage.toNetworkStorage()))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_STORAGE))

                val deletedStorage = storageRepository.deleteStorage(id)
                deletedStorage ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.STORAGE_NOT_FOUND))

                call.respond(ApiResponse.Success("Storage deleted: ${id}", deletedStorage.toNetworkStorage()))
            }
        }
        post {
            val addStorageNetworkRequest = runCatching { call.receive<AddStorageNetworkRequest>() }.getOrNull()
            addStorageNetworkRequest ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.Error(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )

            val createdStorage = addStorageNetworkRequest.run {

                val resolvedParentId = parentId?.let {
                    if (!parentId.isUUID()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_STORAGE))
                    }

                    storageRepository.getStorage(it)?.id ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.Error(ApiError(
                            ErrorMessages.STORAGE_NOT_FOUND.type,
                            ErrorMessages.STORAGE_NOT_FOUND.message,
                            context = "ID: $it"
                        ))
                    )
                }
                storageRepository.createStorage(name, description, resolvedParentId) }

            call.respond(HttpStatusCode.Created,
                ApiResponse.Success("Storage created: ${createdStorage.id}", createdStorage.toNetworkStorage()))
        }

    }
}