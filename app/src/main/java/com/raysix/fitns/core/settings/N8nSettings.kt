package com.raysix.fitns.core.settings

const val DefaultN8nBaseUrl = "https://pi.pufferfish-lenok.ts.net/"

data class N8nConnectionSettings(
    val baseUrl: String = DefaultN8nBaseUrl,
    val bearerTokenConfigured: Boolean = false,
    val syncEnabled: Boolean = false
)

