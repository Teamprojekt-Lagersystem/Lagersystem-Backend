package io.github.lagersystembackend.breadcrumb

import io.github.lagersystembackend.breadcrumb.Breadcrumb.BreadcrumbEntry
import io.github.lagersystembackend.space.SpaceRepository
import io.github.lagersystembackend.storage.StorageRepository

class DefaultBreadcrumbUseCase(
    private val storageRepository: StorageRepository,
    private val spaceRepository: SpaceRepository,
) : BreadcrumbUseCase {
    override fun getBreadcrumb(of: String): Breadcrumb? {
        if (!objectWithIdExists(of)) return null
        getStorageBreadcrumb(of).takeIf { it.isNotEmpty() }?.let { return Breadcrumb(it) }
        getSpaceBreadcrumb(of).let { return Breadcrumb(it) }
    }

    override fun objectWithIdExists(id: String) = storageRepository.storageExists(id) || spaceRepository.spaceExists(id)

    private fun getStorageBreadcrumb(storageId: String): List<BreadcrumbEntry> {
        val storage = storageRepository.getStorage(storageId) ?: return emptyList()
        val path = listOf(BreadcrumbEntry(storage.id.toString(), storage.name, "storage"))
        val parentBreadcrumb = storage.parentId?.let { getStorageBreadcrumb(it.toString()) }
        return if (parentBreadcrumb != null) parentBreadcrumb + path else path
    }

    private fun getSpaceBreadcrumb(spaceId: String): List<BreadcrumbEntry> {
        val space = spaceRepository.getSpace(spaceId) ?: return emptyList()
        val path = listOf(BreadcrumbEntry(space.id.toString(), space.name, "space"))
        return getStorageBreadcrumb(space.storageId.toString()) + path
    }
}