package io.github.lagersystembackend.space

import io.github.lagersystembackend.common.*
import io.github.lagersystembackend.storage.StorageRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.spaceRoutes(spaceRepository: SpaceRepository, storageRepository: StorageRepository) {
    route("/spaces") {
        get { call.respond(
            spaceRepository.getSpaces().map { it.toNetworkSpace() }) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_SPACE)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val space = spaceRepository.getSpace(id)
                if (space == null) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(space.toNetworkSpace())
            }

            delete {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_SPACE)
                }

                if (errors.isNotEmpty()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val deletedSpace = spaceRepository.deleteSpace(id)
                if (deletedSpace == null) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                    return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(deletedSpace.toNetworkSpace())
            }
            route("/update") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updateSpaceNetworkRequest = runCatching { call.receive<UpdateSpaceNetworkRequest>() }.getOrNull()

                    if (updateSpaceNetworkRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    if (!spaceRepository.spaceExists(id)) {
                        errors.add(ErrorMessages.SPACE_NOT_FOUND)
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    val updatedSpace = updateSpaceNetworkRequest.let {
                        spaceRepository.updateSpace(id, it.name, it.size, it.description)
                    }

                    updatedSpace?.let {
                        call.respond(it.toNetworkSpace())
                    }
                }
            }
            route("/move") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE.withContext("ID: $id"))
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val moveSpaceNetworkRequest = runCatching { call.receive<MoveSpaceNetworkRequest>() }.getOrNull()

                    if (moveSpaceNetworkRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val space = spaceRepository.getSpace(id)
                    if (space == null) {
                        errors.add(ErrorMessages.SPACE_NOT_FOUND.withContext("ID: $id"))
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    val targetStorageId = moveSpaceNetworkRequest.targetStorageId
                    if (!targetStorageId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_STORAGE.withContext("Target Storage ID: $targetStorageId"))
                    } else if (!storageRepository.storageExists(targetStorageId)) {
                        errors.add(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $targetStorageId"))
                    }

                    if (errors.isNotEmpty()) {
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val movedSpace = spaceRepository.moveSpace(id, moveSpaceNetworkRequest.targetStorageId)

                    call.respond(movedSpace.toNetworkSpace())
                }
            }
        }
        post {
            val errors = mutableListOf<ApiError>()
            val addSpaceNetworkRequest = runCatching { call.receive<AddSpaceNetworkRequest>() }.getOrNull()

            if (addSpaceNetworkRequest == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            } else {
                if (!addSpaceNetworkRequest.storageId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORAGE)
                }

                if (addSpaceNetworkRequest.storageId.isUUID() && !storageRepository.storageExists(addSpaceNetworkRequest.storageId)) {
                    errors.add(ErrorMessages.STORAGE_NOT_FOUND)
                }
            }

            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdSpace = addSpaceNetworkRequest?.let {
                spaceRepository.createSpace(it.name, it.size, it.description, it.storageId)
            }

            createdSpace?.let {
                call.respond(HttpStatusCode.Created, it.toNetworkSpace())
            }
        }
    }
}