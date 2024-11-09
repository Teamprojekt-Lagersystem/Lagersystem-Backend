package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.isUUID
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.storageRoutes(storageRepository: StorageRepository) {
    route("/storages") {
        get { call.respond(storageRepository.getStorages().map { it.toNetworkStorage() }) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                val storage = storageRepository.getStorage(id)
                storage ?: return@get call.respondText("Storage not found", status = HttpStatusCode.NotFound)

                call.respond(storage.toNetworkStorage())
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                if (!storageRepository.deleteStorage(id))
                    return@delete call.respondText("Storage not found", status = HttpStatusCode.NotFound)

                call.respondText("Storage deleted")
            }
        }
        post {
            val addStorageNetworkRequest = runCatching { call.receive<AddStorageNetworkRequest>() }.getOrNull()
            addStorageNetworkRequest ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "Body should be Serialized AddStorageNetworkRequest"
            )

            val createdStorage = addStorageNetworkRequest.run {

                val resolvedParentId = parentId?.let {
                    if (!parentId.isUUID()) {
                        return@post call.respond(HttpStatusCode.BadRequest, "Invalid UUID")
                    }

                    storageRepository.getStorage(it)?.id ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Parent storage with ID $it not found"
                    )
                }
                storageRepository.createStorage(name, description, resolvedParentId) }

            call.respond(HttpStatusCode.Created, "Storage created: ${createdStorage.id} ")
        }

    }
}