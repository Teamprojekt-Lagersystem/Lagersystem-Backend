package io.github.lagersystembackend.attribute

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresProductAttributeRepository : ProductAttributeRepository {
    override fun createOrUpdateAttribute(key: String, attribute: Attribute, productId: String) = transaction {
        ProductAttributeEntity.new {
            this.productId = UUID.fromString(productId)
            this.key = key
            this.type = attribute.type()
            this.value = attribute.value()
        }.toAttribute()
    }

    override fun deleteAttribute(key: String, productId: String) = transaction {
        ProductAttributeEntity.find {
            (ProductAttributes.productId eq UUID.fromString(productId)) and (ProductAttributes.key eq key)
        }.singleOrNull()?.delete() != null
    }
}