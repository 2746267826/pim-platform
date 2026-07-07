package com.pim.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pim.core.models.LoginRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class ApiServiceConverterTest {
    @Test
    fun loginRequestSerializerWritesJson() {
        val json = Json.encodeToString(LoginRequest("alice", "secret"))

        assertTrue(json, json.contains("\"username\":\"alice\""))
        assertTrue(json, json.contains("\"password\":\"secret\""))
    }

    @Test
    fun loginRequestBodyConverterWritesJson() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()

        val converter = retrofit.requestBodyConverter<LoginRequest>(
            LoginRequest::class.java,
            emptyArray<Annotation>(),
            emptyArray<Annotation>()
        )
        val body = converter.convert(LoginRequest("alice", "secret")) ?: error("Converter returned null")
        val buffer = Buffer()

        body.writeTo(buffer)
        val json = buffer.readUtf8()

        assertTrue(json, json.contains("\"username\":\"alice\""))
        assertTrue(json, json.contains("\"password\":\"secret\""))
    }
}
