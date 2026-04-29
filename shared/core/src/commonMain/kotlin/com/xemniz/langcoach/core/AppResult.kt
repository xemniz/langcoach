package com.xemniz.langcoach.core

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String

    data class Network(override val message: String) : AppError
    data class Storage(override val message: String) : AppError
    data class Api(override val message: String, val code: Int? = null) : AppError
    data class Unknown(override val message: String) : AppError
}
