package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.*
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
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val storage = storageRepository.getStorage(id)
                if (storage == null) {
                    errors.add(ErrorMessages.STORAGE_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(ApiResponse.Success("Storage found: $id", storage.toNetworkStorage()))
            }


            delete {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                }

                if (errors.isNotEmpty()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val deletedStorage = storageRepository.deleteStorage(id)
                if (deletedStorage == null) {
                    errors.add(ErrorMessages.STORAGE_NOT_FOUND)
                    return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(ApiResponse.Success("Storage deleted: $id", deletedStorage.toNetworkStorage()))
            }
        }
        post {
            val errors = mutableListOf<ApiError>()
            val addStorageNetworkRequest = runCatching { call.receive<AddStorageNetworkRequest>() }.getOrNull()

            if (addStorageNetworkRequest == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            } else {
                if (addStorageNetworkRequest.parentId != null && !addStorageNetworkRequest.parentId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                }

                if (addStorageNetworkRequest.parentId != null && storageRepository.getStorage(addStorageNetworkRequest.parentId!!) == null) {
                    errors.add(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: ${addStorageNetworkRequest.parentId}"))
                }
            }

            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdStorage = addStorageNetworkRequest?.let {
                storageRepository.createStorage(it.name, it.description, it.parentId)
            }

            createdStorage?.let {
                call.respond(HttpStatusCode.Created, ApiResponse.Success("Storage created: ${it.id}", it.toNetworkStorage()))
            }
        }
    }
}