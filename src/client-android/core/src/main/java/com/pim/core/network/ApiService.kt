package com.pim.core.network

import com.pim.core.models.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthResponse>

    @GET("calendar/events")
    suspend fun getEvents(
        @Query("start") start: String,
        @Query("end") end: String
    ): ApiResponse<List<EventResponse>>

    @GET("calendar/tasks")
    suspend fun getTasks(
        @Query("inbox") inbox: Boolean? = null
    ): ApiResponse<List<TaskResponse>>

    @POST("calendar/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest
    ): ApiResponse<TaskResponse>
}
