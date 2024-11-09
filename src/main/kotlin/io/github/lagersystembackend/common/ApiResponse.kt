package io.github.lagersystembackend.common

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<out T> {
    @Serializable
    data class Success<out T>(val message: String? = null, val data: T? = null) : ApiResponse<T>()
    @Serializable
    data class Error(val errorMessage: String) : ApiResponse<Nothing>()
}