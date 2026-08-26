package com.raysix.fitns.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserDecimalParserTest {

    @Test
    fun `parses German and English decimal separators`() {
        assertEquals(72.5, "72,5".toUserDecimalOrNull()!!, 0.0)
        assertEquals(72.5, "72.5".toUserDecimalOrNull()!!, 0.0)
        assertEquals(0.25, ",25".toUserDecimalOrNull()!!, 0.0)
        assertEquals(0.25, ".25".toUserDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `trims surrounding whitespace and preserves signs`() {
        assertEquals(-12.75, "  -12,75  ".toUserDecimalOrNull()!!, 0.0)
        assertEquals(12.75, "+12.75".toUserDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `rejects ambiguous mixed or malformed separators`() {
        assertNull("1,234.56".toUserDecimalOrNull())
        assertNull("1.234,56".toUserDecimalOrNull())
        assertNull("12,,5".toUserDecimalOrNull())
        assertNull("12.".toUserDecimalOrNull())
        assertNull("".toUserDecimalOrNull())
    }

    @Test
    fun `rejects non finite and exponent values`() {
        assertNull("NaN".toUserDecimalOrNull())
        assertNull("Infinity".toUserDecimalOrNull())
        assertNull("-Infinity".toUserDecimalOrNull())
        assertNull("1e309".toUserDecimalOrNull())
        assertNull("1e3".toUserDecimalOrNull())
    }
}
