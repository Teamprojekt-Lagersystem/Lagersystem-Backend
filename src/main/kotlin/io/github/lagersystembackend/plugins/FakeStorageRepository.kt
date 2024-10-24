package io.github.lagersystembackend.plugins

/** this class is supposed to implement the functionality of the
 * IStorageRepository and communicate with a mock database
 */

class FakeStorageRepository : IStorageRepository {
    private val storages: MutableMap<String, Storage>  = mutableMapOf()

    override suspend fun createStorage(
        name: String,
        description: String,
        spaces: MutableList<Space>,
        subStorages: MutableList<Storage>
    ) {
        storages[name] = Storage(name, description, spaces, subStorages)
    }

    override suspend fun addSubstorageToStorage(parentStorageName: String, storage: Storage): Boolean {
        val parentStorage = storages[parentStorageName]
        if (parentStorage?.subStorages != null) {
            if (!parentStorage.subStorages.contains(storage)) {
                parentStorage.subStorages.add(storage)
                return true
            }
        }
        return false
    }

    override suspend fun addSpaceToStorage(storageName: String, space: Space): Boolean {
        val storage = storages[storageName]
        if (storage?.spaces != null) {
            if (!storage.spaces.contains(space)) {
                storage.spaces.add(space)
                return true
            }
        }
        return false
    }

    override suspend fun findFreeSpaceForProduct(storageName: String, productSize: Float): String? {
        val storage = storages[storageName]
        if (storage?.spaces != null) {
            val space = storage.spaces.first { space -> space.products.isEmpty() }
            return space.name
        }
        return null
    }

    override suspend fun addProductToSpace(storageName: String, spaceName: String, product: Product): Boolean {
        val storage = storages[storageName]
        if (storage?.spaces != null) {
            val space = storage.spaces.first { space -> space.name == spaceName }
            return space.products.add(product)
        }
        return false
    }

    override suspend fun findProduct(product: Product): String? {
        for (storage in storages) {
            for (space in storage.value.spaces) {
                if (space.products.contains(product)) {
                    return storage.value.name + space.name;
                }
            }
        }
        return null;
    }

    override suspend fun deleteProduct(storageName: String, spaceName: String, productName: String): Boolean {
        val storage = storages[storageName]
        if (storage?.spaces != null) {
            val space = storage.spaces.first { x -> x.name == spaceName }
            return space.products.removeAll { product -> product.description.name == productName }
        }
        return false
    }

}