package io.github.lagersystembackend.search

import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.storage.Storages
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ComparisonOp
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.DoubleColumnType
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.stringParam
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import kotlin.collections.map


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

class DefaultSearchUseCase : SearchUseCase {

    private fun searchTable(
        table: Table, columnsToSearch: List<Column<*>>, tsQuery: ToTSQuery<String>
    ): List<Pair<ResultRow, Double>> {
        return transaction {
            val concat = concatColumns(columnsToSearch)
            val tsVector = concat.toTSVector()
            val tsRank = TSRank(tsVector, tsQuery, normalization = 32)
            table.select(table.columns + tsRank.alias("rank")).where { tsVector tsMatches tsQuery }.orderBy()
                .map { row -> Pair(row, row[tsRank.alias("rank")]) }
        }
    }

    override fun fullTextSearch(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val tsQuery = toTSQuery(query)
        val productResults = searchTable(
            Products, listOf(Products.name, Products.description, Products.createdAt, Products.updatedAt), tsQuery
        ).map { (row, rank) ->
            SearchResult(
                id = row[Products.id].value.toString(),
                name = row[Products.name],
                description = row[Products.description],
                type = "product",
                createdAt = row[Products.createdAt].toString(),
                updatedAt = row[Products.updatedAt]?.toString(),
                rank = rank
            )
        }
        val spaceResults = searchTable(
            Spaces, listOf(Spaces.name, Spaces.description, Spaces.createdAt, Spaces.updatedAt), tsQuery
        ).map { (row, rank) ->
            SearchResult(
                id = row[Spaces.id].value.toString(),
                name = row[Spaces.name],
                description = row[Spaces.description],
                type = "space",
                createdAt = row[Spaces.createdAt].toString(),
                updatedAt = row[Spaces.updatedAt]?.toString(),
                rank = rank
            )
        }

        val storageResults = searchTable(
            Storages, listOf(Storages.name, Storages.description, Storages.createdAt, Storages.updatedAt), tsQuery
        ).map { (row, rank) ->
            SearchResult(
                id = row[Storages.id].value.toString(),
                name = row[Storages.name],
                description = row[Storages.description],
                type = "storages",
                createdAt = row[Storages.createdAt].toString(),
                updatedAt = row[Storages.updatedAt]?.toString(),
                rank = rank
            )
        }

        return productResults + spaceResults + storageResults
    }

    private fun concatColumns(columns: List<Column<*>>) = Expression.build {
        concat(*(columns.flatMap { listOf(it.mapToCharIfNecessary(), stringParam(" ")) }.dropLast(1).toTypedArray()))
    }

    @Suppress("UNCHECKED_CAST")
    private fun Column<*>.mapToCharIfNecessary() = when (this.columnType) {
        is TextColumnType -> this
        is JavaLocalDateTimeColumnType -> (this as? Column<LocalDateTime>)?.toChar() ?: this
        else -> this
    }

    private class TSMatchOp<T : String?>(
        expr1: Expression<T>, expr2: Expression<T>
    ) : ComparisonOp(expr1, expr2, "@@")

    private infix fun <T : String?> Expression<T>.tsMatches(other: Expression<T>) = TSMatchOp(this, other)

    private class ToTSVector<T : String?>(
        config: Expression<T>?, document: Expression<T>
    ) : CustomFunction<String?>("to_tsvector",
        TextColumnType(),
        *config?.let { arrayOf(config, document) } ?: arrayOf(document))

    private fun <T : String?> Expression<T>.toTSVector(config: Expression<T>? = null) = ToTSVector(config, this)

    private class ToTSQuery<T : String?>(
        config: Expression<T>?, query: Expression<out String?>
    ) : CustomFunction<String?>("websearch_to_tsquery",
        TextColumnType(),
        *config?.let { arrayOf(config, query) } ?: arrayOf(query))

    private fun toTSQuery(query: String, config: Expression<String>? = null) = ToTSQuery(
        config, stringParam(
            "$query or " + query.replace(
                Regex("[.\\-_@:T]"), " "
            )
        )
    )

    private fun Column<LocalDateTime>.toChar() = CustomFunction<String>(
        "to_char", TextColumnType(), this, stringParam("YYYY MM DD HH24 MI SS")
    )

    private class TSRank<T : String?>(
        vector: Expression<T>, query: Expression<T>, weights: LiteralOp<List<Float>>? = null, normalization: Int? = null
    ) : CustomFunction<Double>(
        "ts_rank", DoubleColumnType(), *when {
            weights != null && normalization != null -> arrayOf(weights, vector, query, intParam(normalization))
            weights != null -> arrayOf(weights, vector, query)
            normalization != null -> arrayOf(vector, query, intParam(normalization))
            else -> arrayOf(vector, query)
        }
    )

}




