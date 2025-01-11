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

interface BreadcrumbsUseCase {
    fun getBreadcrumb(of: String): Breadcrumb
}

