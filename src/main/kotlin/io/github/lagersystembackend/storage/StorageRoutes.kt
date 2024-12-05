package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.storageRoutes(storageRepository: StorageRepository) {
    route("/storages") {
        get {
            val depthParam = call.request.queryParameters["depth"]
            val depth = depthParam?.toIntOrNull()
            val errors = mutableListOf<ApiError>()

            if (depthParam != null && (depth == null || depth < 0)) {
                errors.add(ErrorMessages.INVALID_DEPTH)
            }
            if (errors.isNotEmpty()) {
                return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val storages = storageRepository.getStorages()
                .filter { it.parentId == null }
                .map { it.toNetworkStorage(maxDepth = depth) }

            call.respond(storages)
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()
                val depthParam = call.request.queryParameters["depth"]
                val depth = depthParam?.toIntOrNull()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                }
                if (depthParam != null && (depth == null || depth < 0)) {
                    errors.add(ErrorMessages.INVALID_DEPTH)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val storage = storageRepository.getStorage(id)
                if (storage == null) {
                    errors.add(ErrorMessages.STORAGE_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                val networkStorage = storage.toNetworkStorage(maxDepth = depth)
                call.respond(networkStorage)
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

                call.respond(deletedStorage.toNetworkStorage())
            }
        }
        post {
            val errors = mutableListOf<ApiError>()
            val addStorageNetworkRequest = runCatching { call.receive<AddStorageNetworkRequest>() }.getOrNull()

            if (addStorageNetworkRequest == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            }  else {
                val parentId = addStorageNetworkRequest.parentId

                if (parentId != null) {
                    if (!parentId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                    } else if (storageRepository.getStorage(parentId) == null) {
                        errors.add(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $parentId"))
                    }
                }
            }
            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }
            val createdStorage = addStorageNetworkRequest?.let {
                storageRepository.createStorage(it.name, it.description, it.parentId)
            }

            createdStorage?.let {
                call.respond(HttpStatusCode.Created, it.toNetworkStorage())
            }
        }
    }
}