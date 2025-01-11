package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.breadcrumb.Breadcrumb
import kotlinx.serialization.Serializable

interface SearchUseCase {
    fun fullTextSearch(query: String): List<SearchResult>
}

data class SearchResult(
    val breadcrumb: Breadcrumb?,
    val type: String,
    val rank: Double,
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String?,
    val attributes: Map<String, Attribute>? = null,
    val unit: String? = null,
    val totalSize: Double? = null,
    val currentSize: Double? = null,
    val size: Double? = null
)

@Serializable
data class NetworkSearchResult(
    val breadcrumb: Breadcrumb?,
    val type: String,
    val rank: Double,
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String?,
    val attributes: Map<String, Attribute>? = null,
    val unit: String? = null,
    val totalSize: Double? = null,
    val currentSize: Double? = null,
    val size: Double? = null
)

fun SearchResult.toNetworkSearchResult(): NetworkSearchResult = NetworkSearchResult(
    breadcrumb = breadcrumb,
    type = this.type,
    rank = this.rank,
    id = this.id,
    name = this.name,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    attributes = this.attributes,
    unit = this.unit,
    totalSize = this.totalSize,
    currentSize = this.currentSize,
    size = this.size
)
