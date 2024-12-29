package io.github.lagersystembackend.common

import kotlinx.serialization.Serializable

sealed class ApiResponse {
    @Serializable
    data class Error(val errors: List<ApiError>) : ApiResponse()
}

@Serializable
data class ApiError(val type: String, val message: String, val context: String? = null)

fun ApiError.withContext(context: String?): ApiError {
    return ApiError(this.type, this.message, context)
}

object ErrorMessages {
    val INVALID_UUID_STORAGE = ApiError("INVALID_UUID" , "The provided storage ID is not a valid UUID.")
    val INVALID_UUID_SPACE = ApiError("INVALID_UUID" , "The provided space ID is not a valid UUID.")
    val INVALID_UUID_PRODUCT = ApiError("INVALID_UUID" , "The provided product ID is not a valid UUID.")
    val SPACE_NOT_FOUND = ApiError("SPACE_NOT_FOUND" , "The specified space was not found.")
    val STORAGE_NOT_FOUND = ApiError("STORAGE_NOT_FOUND" , "The specified storage was not found.")
    val PRODUCT_NOT_FOUND = ApiError("PRODUCT_NOT_FOUND" , "The specified product was not found.")
    val BODY_NOT_SERIALIZED_STORAGE = ApiError("BODY_NOT_SERIALIZED" , "The request of specified storage is not in the expected format.")
    val BODY_NOT_SERIALIZED_SPACE = ApiError("BODY_NOT_SERIALIZED" , "The request of specified space is not in the expected format.")
    val BODY_NOT_SERIALIZED_PRODUCT = ApiError("BODY_NOT_SERIALIZED" , "The request of specified product is not in the expected format.")
    val INVALID_DEPTH = ApiError("INVALID_DEPTH" , "The provided depth parameter is invalid.")
    val RECURSIVE_MOVE = ApiError("RECURSIVE_MOVE" , "Cannot move storage to itself.")
    val RECURSIVE_COPY = ApiError("RECURSIVE_COPY" , "Cannot copy storage to itself.")
    val SIZE_NOT_FITTING = ApiError("PRODUCT_NOT_FITTING" , "The size of the product exceeds the size of the space.")
    val UNIT_NOT_FITTING = ApiError("UNIT_NOT_FITTING", "The unit of the product differs from the unit of the space.")
    val SIZE_TOO_SMALL = ApiError("SIZE_TOO_SMALL", "The new size of the space must be larger than the size of the stored products.")
    val NEGATIVE_SIZE = ApiError("NEGATIVE_SIZE" , "The size must be positive.")
    val WRONG_SPECIFICATION = ApiError("WRONG_SPECIFICATION", "Size and unit have to be either both null or both not null.")
}