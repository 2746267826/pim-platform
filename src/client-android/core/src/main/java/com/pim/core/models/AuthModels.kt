package com.pim.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val timestamp: String = ""
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String,
    val userInfo: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class EventResponse(
    val id: String,
    val title: String,
    val dtStart: String,
    val dtEnd: String,
    val location: String? = null
)

@Serializable
data class TaskResponse(
    val id: String,
    val title: String,
    val priority: Int,
    val due: String? = null,
    val status: String
)

@Serializable
data class CreateTaskRequest(
    val calendarId: String,
    val title: String,
    val priority: Int = 0,
    val due: String? = null
)
