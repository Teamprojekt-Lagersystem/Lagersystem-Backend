package io.github.lagersystembackend.testing

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val name: String,
)

interface SomeRepository {
    fun allItems() : List<Item>
    fun itemByName(name: String) : Item?
    fun addItem(item: Item)
    fun removeItem(item: Item): Boolean
}

class FakeSomeRepository : SomeRepository {
    private val items = mutableListOf<Item>()
    override fun allItems() : List<Item> = items
    override fun itemByName(name: String) : Item? = items.find { it.name == name }
    override fun addItem(item: Item) { items.add(item) }
    override fun removeItem(item: Item): Boolean = items.remove(item)
}
