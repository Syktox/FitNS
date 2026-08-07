package com.raysix.fitns.core.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.raysix.fitns.domain.model.Micronutrients

object MicronutrientsCodec {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(Micronutrients::class.java)

    fun encode(value: Micronutrients): String? {
        if (value.values.isEmpty()) return null
        val json = adapter.toJson(value)
        return json.takeIf { it.isNotBlank() && it != "{}" }
    }

    fun decode(value: String?): Micronutrients {
        if (value.isNullOrBlank()) return Micronutrients()
        return runCatching { adapter.fromJson(value) }.getOrNull() ?: Micronutrients()
    }
}
