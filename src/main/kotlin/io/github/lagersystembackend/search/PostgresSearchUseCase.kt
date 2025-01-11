package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.attribute.buildAttribute
import io.github.lagersystembackend.breadcrumb.BreadcrumbUseCase
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.storage.Storages
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ComparisonOp
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.DoubleColumnType
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.arrayLiteral
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.stringParam
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.collections.plus

class PostgresSearchUseCase(private val breadcrumbUseCase: BreadcrumbUseCase) : SearchUseCase {

    override fun fullTextSearch(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = normalizeQuery(query.trim())
        val weights = listOf(0.1f, 0.2f, 0.4f, 1.0f) //default Postgres Weights for A, B, C and D

        val searchResults = fullTextSearchWeighted(query.trim(), weights).let {
            if (query != normalizedQuery) it + fullTextSearchWeighted(normalizedQuery, weights.map { it * 0.2f }) else it
        }

        return searchResults.groupBy { it.id }.map { (_, searchResults) ->
            searchResults.reduce { acc, searchResult ->
                acc.copy(rank = acc.rank + searchResult.rank)
            }
        }.sortedByDescending { it.rank }
    }

    private fun fullTextSearchWeighted(query: String, weights: List<Float>? = null): List<SearchResult> {
        val tsQuery = toTSQuery(query)

        val productResults = searchTable(
            Products, Products.tsVector, tsQuery, weights
        ).map { (row, rank) ->
            SearchResult(
                id = row[Products.id].value.toString(),
                name = row[Products.name],
                description = row[Products.description],
                type = "product",
                unit = row[Products.unit],
                size = row[Products.size],
                createdAt = row[Products.createdAt].toString(),
                updatedAt = row[Products.updatedAt]?.toString(),
                rank = rank,
                attributes = getProductAttributes(row[Products.id].value),
                breadcrumb = null
            )
        }
        val spaceResults = searchTable(
            Spaces, Spaces.tsVector, tsQuery, weights
        ).map { (row, rank) ->
            SearchResult(
                id = row[Spaces.id].value.toString(),
                name = row[Spaces.name],
                description = row[Spaces.description],
                totalSize = row[Spaces.totalSize],
                unit = row[Spaces.unit],
                currentSize = row[Spaces.currentSize],
                type = "space",
                createdAt = row[Spaces.createdAt].toString(),
                updatedAt = row[Spaces.updatedAt]?.toString(),
                rank = rank,
                breadcrumb = breadcrumbUseCase.getBreadcrumb(row[Spaces.id].value.toString())!!
            )
        }

        val storageResults = searchTable(
            Storages, Storages.tsVector, tsQuery, weights
        ).map { (row, rank) ->
            SearchResult(
                id = row[Storages.id].value.toString(),
                name = row[Storages.name],
                description = row[Storages.description],
                type = "storages",
                createdAt = row[Storages.createdAt].toString(),
                updatedAt = row[Storages.updatedAt]?.toString(),
                rank = rank,
                breadcrumb = breadcrumbUseCase.getBreadcrumb(row[Storages.id].value.toString())!!
            )
        }

        val productAttributeResults = searchTable(
            ProductAttributes, ProductAttributes.tsVector, tsQuery, weights
        ).map { (row, rank) ->
            SearchResult(
                id = row[ProductAttributes.productId].value.toString(),
                name = "",
                description = "",
                type = "product_attribute",
                createdAt = "",
                updatedAt = "",
                rank = rank,
                attributes = mapOf(row[ProductAttributes.key] to buildAttribute(row[ProductAttributes.value], row[ProductAttributes.type])),
                breadcrumb = null
            )
        }

        val productResultsWithAttributes = (productResults + productAttributeResults).groupBy { it.id }.map { (_, searchResults) ->
            searchResults.reduce { acc, searchResult ->
                acc.copy(rank = acc.rank + searchResult.rank, attributes = acc.attributes?.plus(searchResult.attributes ?: emptyMap()))
            }
        }

        return productResultsWithAttributes + spaceResults + storageResults
    }

    private fun searchTable(
        table: Table, tsVector: Column<String>, tsQuery: ToTSQuery<String>, weights: List<Float>? = null
    ): List<Pair<ResultRow, Double>> {
        return transaction {
            val tsRank = TSRank(tsVector, tsQuery, normalization = 32, weights = weights)

            table.select(table.columns + tsRank.alias("rank")).where { tsVector tsMatches tsQuery }
                .map { row -> Pair(row, row[tsRank.alias("rank")]) }
        }
    }

    private fun getProductAttributes(of: UUID): Map<String, Attribute> = transaction {
        ProductAttributes.select(ProductAttributes.key, ProductAttributes.value, ProductAttributes.type)
            .where { ProductAttributes.productId eq of }.associate { row ->
                row[ProductAttributes.key] to buildAttribute(row[ProductAttributes.value], row[ProductAttributes.type])
            }
    }

    private class TSMatchOp(
        expr1: Expression<*>, expr2: Expression<*>
    ) : ComparisonOp(expr1, expr2, "@@")

    private infix fun Expression<*>.tsMatches(other: Expression<*>) = TSMatchOp(this, other)

    private class ToTSQuery<T : String?>(
        config: Expression<T>?, query: Expression<out String?>
    ) : CustomFunction<String?>("websearch_to_tsquery",
        TextColumnType(),
        *config?.let { arrayOf(config, query) } ?: arrayOf(query))

    private fun toTSQuery(query: String, config: Expression<String>? = null) = ToTSQuery(
        config, stringParam(query)
    )

    private fun normalizeQuery(query: String): String = query
        .replace(Regex("-(\\d{2})T(\\d{2}:)"), "-$1 $2") // replaces 'T' in -ddTdd:
        .replace(Regex("\\b(\\d+)\\.0\\b"), "$1") // removes .0 from numbers
        .replace(Regex("[^a-zA-Z0-9 ]"), " ")  //Filters out all non-alphanumeric characters


    private class TSRank(
        vector: Expression<*>, query: Expression<*>, weights: List<Float>? = null, normalization: Int? = null
    ) : CustomFunction<Double>(
        "ts_rank", DoubleColumnType(), *when {
            weights != null && normalization != null -> arrayOf(
                arrayLiteral(weights), vector, query, intParam(normalization)
            )

            weights != null -> arrayOf(arrayLiteral(weights), vector, query)
            normalization != null -> arrayOf(vector, query, intParam(normalization))
            else -> arrayOf(vector, query)
        }
    )

}

fun createPostgresFullTextSearchTriggers() = transaction {
    createProductsPostgresFullTextSearchTriggers()
    createProductAttributesPostgresFullTextSearchTriggers()
    createSpacesPostgresFullTextSearchTriggers()
    createStoragesPostgresFullTextSearchTriggers()
}

private fun createTrigger(
    tableName: String, tsVectorLogic: String
) = transaction {
    val functionName = "update_${tableName}_tsvector"
    val triggerName = "trigger_update_${tableName}_tsvector"

    if (checkIfTriggerExists(triggerName)) return@transaction

    exec(
        """
        CREATE OR REPLACE FUNCTION $functionName() RETURNS TRIGGER AS ${'$'}${'$'}
        BEGIN
        NEW."tsVector" := $tsVectorLogic;
        RETURN NEW;
        END;
        ${'$'}${'$'} LANGUAGE plpgsql;

        CREATE TRIGGER $triggerName
        BEFORE INSERT OR UPDATE ON "$tableName"
        FOR EACH ROW
        EXECUTE FUNCTION $functionName();
    """.trimIndent()
    )
}

private fun createProductsPostgresFullTextSearchTriggers() = transaction {
    createTrigger(
        Products.nameInDatabaseCase(), """
                    setweight(to_tsvector('english', COALESCE(NEW."name", '')), 'A') ||
                    setweight(to_tsvector('english', COALESCE(NEW."description", '')), 'B') ||
                    setweight(to_tsvector('english', COALESCE(NEW."unit", '')), 'C') ||
                    setweight(to_tsvector('english', COALESCE(NEW."size"::text, '')), 'C') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."createdAt"::text, ''), '[-]', ' ', 'g')), 'C') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."updatedAt"::text, ''), '[-]', ' ', 'g')), 'C')
    """.trimIndent()
    )
}

private fun createProductAttributesPostgresFullTextSearchTriggers() = transaction {
    createTrigger(
        ProductAttributes.nameInDatabaseCase(), """
                    setweight(to_tsvector('english', COALESCE(NEW."key", '')), 'B') ||
                    setweight(to_tsvector('english', COALESCE(NEW."value", '')), 'C') ||
                    setweight(to_tsvector('english', COALESCE(NEW."type", '')), 'C')
    """.trimIndent()
    )
}

private fun createSpacesPostgresFullTextSearchTriggers() = transaction {
    createTrigger(
        Spaces.nameInDatabaseCase(), """
                    setweight(to_tsvector('english', COALESCE(NEW."name", '')), 'A') ||
                    setweight(to_tsvector('english', COALESCE(NEW."description", '')), 'B') ||
                    setweight(to_tsvector('english', COALESCE(NEW."unit", '')), 'C') ||
                    setweight(to_tsvector('english', COALESCE(NEW."totalSize"::text, '')), 'D') ||
                    setweight(to_tsvector('english', COALESCE(NEW."currentSize"::text, '')), 'D') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."createdAt"::text, ''), '[-]', ' ', 'g')), 'C') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."updatedAt"::text, ''), '[-]', ' ', 'g')), 'C')
    """.trimIndent()
    )
}

private fun createStoragesPostgresFullTextSearchTriggers() = transaction {
    createTrigger(
        Storages.nameInDatabaseCase(), """
                    setweight(to_tsvector('english', COALESCE(NEW."name", '')), 'A') ||
                    setweight(to_tsvector('english', COALESCE(NEW."description", '')), 'B') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."createdAt"::text, ''), '[-]', ' ', 'g')), 'C') ||
                    setweight(to_tsvector('english', regexp_replace(COALESCE(NEW."updatedAt"::text, ''), '[-]', ' ', 'g')), 'C')
    """.trimIndent()
    )
}

private fun checkIfTriggerExists(triggerName: String): Boolean = transaction {
    exec(
        """
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.triggers 
            WHERE trigger_name = '$triggerName'
        );
    """.trimIndent()
    ) { rs ->
        rs.next()
        rs.getBoolean(1)
    } == true
}