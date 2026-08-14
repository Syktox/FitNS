package com.raysix.fitns.core.network

import com.squareup.moshi.Moshi
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates n8n services for user-configurable hosts while retaining the shared
 * OkHttp connection pool and Moshi instance. The small cache avoids rebuilding
 * Retrofit on every request without allowing an unbounded set of user-entered
 * URLs to accumulate.
 */
@Singleton
class N8nServiceFactory @Inject constructor(
    private val client: OkHttpClient,
    moshi: Moshi
) {
    private val converterFactory = MoshiConverterFactory.create(moshi)
    private val services = object : LinkedHashMap<String, N8nApiService>(CacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, N8nApiService>?): Boolean {
            return size > CacheSize
        }
    }

    @Synchronized
    fun serviceFor(baseUrl: String): N8nApiService {
        val normalized = normalizeBaseUrl(baseUrl)
            ?: throw IllegalArgumentException("Enter a valid HTTPS base URL.")
        return services.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(converterFactory)
                .build()
                .create(N8nApiService::class.java)
        }
    }

    fun normalizeBaseUrl(baseUrl: String): String? {
        val parsed = baseUrl.trim().toHttpUrlOrNull() ?: return null
        if (!parsed.isHttps || parsed.host.isBlank()) return null
        return parsed.newBuilder()
            .query(null)
            .fragment(null)
            .encodedPath(if (parsed.encodedPath.endsWith('/')) parsed.encodedPath else "${parsed.encodedPath}/")
            .build()
            .toString()
    }

    private companion object {
        const val CacheSize = 4
    }
}
