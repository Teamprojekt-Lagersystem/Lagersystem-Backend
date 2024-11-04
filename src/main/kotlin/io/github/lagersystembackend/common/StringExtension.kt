package io.github.lagersystembackend.common

fun String.isUUID() = runCatching { java.util.UUID.fromString(this) }.isSuccess