package com.pim.core.network

import com.pim.core.models.*
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthResponse>

    // Calendars
    @GET("calendar/calendars")
    suspend fun getCalendars(): ApiResponse<List<CalendarResponse>>

    @POST("calendar/calendars")
    suspend fun createCalendar(@Body request: CreateCalendarRequest): ApiResponse<CalendarResponse>

    // Events
    @GET("calendar/events")
    suspend fun getEvents(
        @Query("start") start: String,
        @Query("end") end: String
    ): ApiResponse<List<EventResponse>>

    @POST("calendar/events")
    suspend fun createEvent(@Body request: CreateEventRequest): ApiResponse<EventResponse>

    @PUT("calendar/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: String,
        @Body request: CreateEventRequest
    ): ApiResponse<EventResponse>

    @DELETE("calendar/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): ApiResponse<String>

    // Tasks
    @GET("calendar/tasks")
    suspend fun getTasks(
        @Query("inbox") inbox: Boolean? = null
    ): ApiResponse<List<TaskResponse>>

    @POST("calendar/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): ApiResponse<TaskResponse>

    @PUT("calendar/tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body request: CreateTaskRequest
    ): ApiResponse<TaskResponse>

    @DELETE("calendar/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): ApiResponse<String>

    // Search
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null
    ): ApiResponse<List<SearchResult>>

    // ICS
    @POST("calendar/import-ics")
    suspend fun importIcs(@Body body: RequestBody): ApiResponse<Int>

    @GET("calendar/export-ics")
    suspend fun exportIcs(
        @Query("start") start: String,
        @Query("end") end: String
    ): ApiResponse<String>

    // Outlook
    @POST("calendar/outlook/sync")
    suspend fun syncOutlook(): ApiResponse<String>
}
