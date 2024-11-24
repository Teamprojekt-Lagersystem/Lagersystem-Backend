package io.github.lagersystembackend.common

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse {
    @Serializable
    data class Success<out T>(val message: String? = null, val data: T? = null) : ApiResponse()
    @Serializable
    data class Error(val errors: List<ApiError>) : ApiResponse()
}

@Serializable
data class ApiError(val type: String, val message: String, val context: String? = null)

fun ApiError.withContext(context: String?): ApiError {
    return ApiError(this.type, this.message, context)
}

@Serializable
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
}