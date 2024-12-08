package io.github.lagersystembackend.attribute

interface AttributeRepository {
    fun createOrUpdateAttribute(key: String, attribute: Attribute, id: String): Attribute
    fun deleteAttribute(key: String, id: String): Boolean
}

interface ProductAttributeRepository: AttributeRepository {
    override fun createOrUpdateAttribute(key: String, attribute: Attribute, productId: String): Attribute
    override fun deleteAttribute(key: String, productId: String): Boolean
}
