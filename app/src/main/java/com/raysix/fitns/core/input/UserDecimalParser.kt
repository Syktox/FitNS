package com.raysix.fitns.core.input

private val userDecimalPattern = Regex("""[+-]?(?:(?:\d+(?:[.,]\d+)?)|(?:[.,]\d+))""")

/**
 * Parses a decimal entered by a user, independently of whether their keyboard
 * emits a comma or a period as the decimal separator.
 *
 * Grouping separators and mixed separator styles are deliberately rejected:
 * interpreting them would be ambiguous across locales. Non-finite values are
 * never returned.
 */
fun String.toUserDecimalOrNull(): Double? {
    val candidate = trim()
    if (!userDecimalPattern.matches(candidate)) return null

    return candidate
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf(Double::isFinite)
}
