package com.pim.core.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthResponseSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun authResponseAcceptsServerUserField() {
        val response = json.decodeFromString<ApiResponse<AuthResponse>>(
            """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "accessToken": "access-token",
                "refreshToken": "refresh-token",
                "expiresAt": "2026-07-07T09:00:00Z",
                "user": {
                  "id": "3dc80613-e62b-4af2-970b-1022e781e67e",
                  "username": "alice",
                  "displayName": "Alice",
                  "role": "user"
                }
              },
              "timestamp": "2026-07-07T09:00:00Z"
            }
            """.trimIndent()
        )

        val auth = response.data

        assertNotNull(auth)
        assertEquals("access-token", auth!!.accessToken)
        assertEquals("refresh-token", auth.refreshToken)
        assertEquals("alice", auth.userInfo?.username)
    }

    @Test
    fun authResponseAcceptsLegacyUserInfoField() {
        val response = json.decodeFromString<ApiResponse<AuthResponse>>(
            """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "accessToken": "access-token",
                "refreshToken": "refresh-token",
                "expiresAt": "2026-07-07T09:00:00Z",
                "userInfo": {
                  "id": "3dc80613-e62b-4af2-970b-1022e781e67e",
                  "username": "alice",
                  "displayName": "Alice",
                  "role": "user"
                }
              },
              "timestamp": "2026-07-07T09:00:00Z"
            }
            """.trimIndent()
        )

        assertEquals("alice", response.data?.userInfo?.username)
    }

    @Test
    fun authResponseKeepsTokensWhenUserIsMissing() {
        val response = json.decodeFromString<ApiResponse<AuthResponse>>(
            """
            {
              "code": 0,
              "message": "ok",
              "data": {
                "accessToken": "access-token",
                "refreshToken": "refresh-token",
                "expiresAt": "2026-07-07T09:00:00Z"
              },
              "timestamp": "2026-07-07T09:00:00Z"
            }
            """.trimIndent()
        )

        assertEquals("access-token", response.data?.accessToken)
        assertEquals(null, response.data?.userInfo)
    }
}
