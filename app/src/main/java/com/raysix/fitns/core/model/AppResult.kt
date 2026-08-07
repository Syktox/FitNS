package com.raysix.fitns.core.model

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object Offline : AppError
    data object Unauthorized : AppError
    data object Timeout : AppError
    data object NotFound : AppError
    data class Validation(val message: String) : AppError
    data class Remote(val code: Int?, val message: String) : AppError
    data class Unknown(val message: String) : AppError
}

