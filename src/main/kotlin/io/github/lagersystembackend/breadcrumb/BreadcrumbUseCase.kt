package io.github.lagersystembackend.breadcrumb

import kotlinx.serialization.Serializable

@Serializable
data class Breadcrumb(
    val entries: List<BreadcrumbEntry>
) {
    @Serializable
    data class BreadcrumbEntry(
        val id: String, val name: String, val type: String
    )
}

interface BreadcrumbUseCase {
    fun getBreadcrumb(of: String): Breadcrumb?
    fun objectWithIdExists(id: String): Boolean
}

