package io.github.lagersystembackend.common

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse {
    @Serializable
    data class Success<out T>(val message: String? = null, val data: T? = null) : ApiResponse()
    @Serializable
    data class Error(val errorMessage: String) : ApiResponse()
}