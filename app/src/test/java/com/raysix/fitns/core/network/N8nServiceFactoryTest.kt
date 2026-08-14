package com.raysix.fitns.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class N8nServiceFactoryTest {
    private val factory = N8nServiceFactory(
        client = OkHttpClient(),
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    )

    @Test
    fun `normalizes valid https URLs and removes query secrets`() {
        assertEquals(
            "https://example.com/n8n/",
            factory.normalizeBaseUrl(" https://example.com/n8n?token=must-not-persist ")
        )
    }

    @Test
    fun `rejects insecure or malformed URLs`() {
        assertNull(factory.normalizeBaseUrl("http://example.com"))
        assertNull(factory.normalizeBaseUrl("not a url"))
    }

    @Test
    fun `reuses services for the same normalized URL`() {
        assertSame(
            factory.serviceFor("https://example.com/n8n"),
            factory.serviceFor("https://example.com/n8n/")
        )
    }
}
