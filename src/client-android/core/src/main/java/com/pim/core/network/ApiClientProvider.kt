package com.pim.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pim.core.settings.ServerSettingsStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClientProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val settingsStore: ServerSettingsStore
) {
    @Volatile
    private var cachedClients: Clients? = null

    fun apiService(): ApiService = clients().apiService

    fun refreshApiService(): ApiService = clients().refreshApiService

    fun dynamicApiService(): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { proxy, method, args ->
            if (method.declaringClass == Any::class.java) {
                return@newProxyInstance invokeAnyMethod(proxy, method, args)
            }

            invokeApiMethod(method, args)
        } as ApiService
    }

    private fun clients(): Clients {
        val baseUrl = settingsStore.getBaseUrl()
        val current = cachedClients
        if (current != null && current.baseUrl == baseUrl) return current

        return synchronized(this) {
            val synchronizedCurrent = cachedClients
            if (synchronizedCurrent != null && synchronizedCurrent.baseUrl == baseUrl) {
                synchronizedCurrent
            } else {
                val updated = Clients(
                    baseUrl = baseUrl,
                    apiService = createApiService(baseUrl, okHttpClient),
                    refreshApiService = createApiService(baseUrl, refreshOkHttpClient)
                )
                cachedClients = updated
                updated
            }
        }
    }

    private fun createApiService(baseUrl: String, client: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(ApiService::class.java)
    }

    private fun invokeApiMethod(method: Method, args: Array<Any?>?): Any? {
        return try {
            method.invoke(apiService(), *(args ?: emptyArray()))
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }

    private fun invokeAnyMethod(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        return when (method.name) {
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "ApiService(baseUrl=${settingsStore.getBaseUrl()})"
            else -> method.invoke(this, *(args ?: emptyArray()))
        }
    }

    private data class Clients(
        val baseUrl: String,
        val apiService: ApiService,
        val refreshApiService: ApiService
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val refreshOkHttpClient = OkHttpClient.Builder().build()
    }
}
