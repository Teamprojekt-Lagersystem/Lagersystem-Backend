package io.github.lagersystembackend.search


data class SearchResult(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String?,
    val rank: Double
)

interface SearchUseCase {
    fun fullTextSearch(query: String): List<SearchResult>
}



