package io.github.lagersystembackend.plugins

data class Produktbeschreibung(
    val einheit: String,
    val groesse: Float,
    val name: String,
    val beschreibung: String
)

data class Produkt(
    val beschreibung: Produktbeschreibung,
    val space: Space
)

data class Space(
    val name: String,
    val size: Float,
    val description: String,
    val unit: String? = null,
    val products: MutableList<Produkt> = mutableListOf()
)

data class Storage(
    val name: String,
    val description: String,
    val spaces: MutableList<Space> = mutableListOf(),
    val subStorages: MutableList<Storage> = mutableListOf()
)

// Abstract Repository Interface

interface ILagerRepository {

    //Erstellt ein neues Storage in einem Depot oder einem übergeordneten Storage.
    //Beispiel: Ein Storage für "Kleinteile" wird erstellt und dem "Hauptlager" zugeordnet.
    fun createStorage(name: String, description: String, parentName: String? = null): Storage

    //Fügt einem bestehenden Storage ein weiteres Storage hinzu.
    //Beispiel: Eine Box namens "Box1" wird zu "Kleinteile" hinzugefügt.
    fun addStorageToStorage(parentStorageName: String, storage: Storage): Boolean

    //Fügt einen Space zu einem bestehenden Storage hinzu.
    //Beispiel: Ein Space namens "Fach1" wird zu "Kleinteile" hinzugefügt.
    fun addSpaceToStorage(storageName: String, space: Space): Boolean

    //Findet einen freien Space, der groß genug für ein Produkt ist.
    //Beispiel: Sucht nach einem Platz für ein Produkt mit der Größe 2.0.
    fun findFreeSpaceForProduct(depotName: String, productSize: Float): String?

    //Fügt ein Produkt einem bestimmten Space hinzu.
    //Beispiel: Ein "Laptop" wird in "Fach1" abgelegt
    fun addProductToSpace(product: Produkt, spaceName: String): Boolean

    //Sucht nach einem Produkt und gibt dessen Pfad zurück.
    //Beispiel: Sucht nach einem "Laptop" und gibt den Pfad zu seinem Lagerort zurück.
    fun findProduct(productName: String): String?

    //Löscht ein Produkt aus einem Space.
    //Beispiel: Der "Laptop" wird aus dem Space entfernt.
    fun deleteProduct(productName: String): Boolean
}