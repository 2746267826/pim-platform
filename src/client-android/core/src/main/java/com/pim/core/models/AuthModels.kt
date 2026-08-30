package com.pim.core.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    @OptIn(ExperimentalSerializationApi::class)
    @SerialName("user")
    @JsonNames("userInfo")
    val userInfo: UserInfo? = null
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

// Calendar

@Serializable
data class CalendarResponse(
    val id: String,
    val name: String,
    val color: String? = null,
    val description: String? = null
)

@Serializable
data class CreateCalendarRequest(
    val name: String,
    val color: String? = null,
    val description: String? = null
)

// Events

@Serializable
data class EventResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val dtStart: String,
    val dtEnd: String,
    val status: String? = null
)

@Serializable
data class CreateEventRequest(
    val calendarId: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val dtStart: String,
    val dtEnd: String
)

// Tasks

@Serializable
data class TaskResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: Int,
    val due: String? = null,
    val status: String
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val priority: Int = 0,
    val due: String? = null
)

// Search

@Serializable
data class SearchResult(
    val id: String,
    val type: String,
    val title: String,
    val snippet: String,
    val url: String
)

@Serializable
data class PagedResult<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalCount: Int = 0,
    val totalPages: Int = 0
)

// ICS

@Serializable
data class IcsImportResponse(
    val code: Int,
    val message: String,
    val data: Int
)
