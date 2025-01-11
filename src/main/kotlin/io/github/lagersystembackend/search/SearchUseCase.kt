package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.Attribute
import kotlinx.serialization.Serializable


interface SearchUseCase {
    fun fullTextSearch(query: String): List<SearchResult>
}

data class SearchResult(
    val type: String,
    val rank: Double,
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String?,
    val attributes: Map<String, Attribute>? = null
)

@Serializable
data class NetworkSearchResult(
    val type: String,
    val rank: Double,
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String?,
    val attributes: Map<String, Attribute>? = null
)

fun SearchResult.toNetworkSearchResult(): NetworkSearchResult = NetworkSearchResult(
    type = this.type,
    rank = this.rank,
    id = this.id,
    name = this.name,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    attributes = this.attributes
)
