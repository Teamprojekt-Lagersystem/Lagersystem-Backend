package io.github.lagersystembackend.plugins

data class ProductDescription(
    val unit: String,
    val size: Float,
    val name: String,
    val description: String
)

data class Product(
    val description: ProductDescription,
    val space: Space
)

data class Space(
    val name: String,
    val size: Float,
    val description: String,
    val unit: String? = null,
    val products: MutableList<Product> = mutableListOf()
)

data class Storage(
    val name: String,
    val description: String,
    val spaces: MutableList<Space> = mutableListOf(),
    val subStorages: MutableList<Storage> = mutableListOf()
)

// Abstract Repository Interface

interface IStorageRepository {

    //Erstellt ein neues Storage in einem Depot oder einem übergeordneten Storage.
    //Beispiel: Ein Storage für "Kleinteile" wird erstellt und dem "Hauptlager" zugeordnet.
    suspend fun createStorage(name: String, description: String, spaces: MutableList<Space>, subStorages: MutableList<Storage>)

    //Fügt einem bestehenden Storage ein weiteres Storage hinzu.
    //Beispiel: Eine Box namens "Box1" wird zu "Kleinteile" hinzugefügt.
    suspend fun addSubstorageToStorage(parentStorageName: String, storage: Storage): Boolean

    //Fügt einen Space zu einem bestehenden Storage hinzu.
    //Beispiel: Ein Space namens "Fach1" wird zu "Kleinteile" hinzugefügt.
    suspend fun addSpaceToStorage(storageName: String, space: Space): Boolean

    //Findet einen freien Space, der groß genug für ein Produkt ist.
    //Beispiel: Sucht nach einem Platz für ein Produkt mit der Größe 2.0.
    suspend fun findFreeSpaceForProduct(storageName: String, productSize: Float): String?

    //Fügt ein Produkt einem bestimmten Space hinzu.
    //Beispiel: Ein "Laptop" wird in "Fach1" abgelegt
    suspend fun addProductToSpace(storageName: String, spaceName: String, product: Product): Boolean

    //Sucht nach einem Produkt und gibt dessen Pfad zurück.
    //Beispiel: Sucht nach einem "Laptop" und gibt den Pfad zu seinem Lagerort zurück.
    suspend fun findProduct(product: Product): String?

    //Löscht ein Produkt aus einem Space.
    //Beispiel: Der "Laptop" wird aus dem Space entfernt.
    suspend fun deleteProduct(storageName: String, spaceName: String, productName: String): Boolean
}