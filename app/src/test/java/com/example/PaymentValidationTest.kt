package com.example

import org.junit.Assert.*
import org.junit.Test

class PaymentValidationTest {

    private fun sanitizeNumericInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val dotIndex = filtered.indexOf('.')
        return if (dotIndex != -1) {
            val integerPart = filtered.substring(0, dotIndex)
            val decimalPart = filtered.substring(dotIndex + 1).replace(".", "")
            val limitedDecimal = if (decimalPart.length > 2) decimalPart.substring(0, 2) else decimalPart
            "$integerPart.$limitedDecimal"
        } else {
            filtered
        }
    }

    private fun validateAmount(text: String, currencySymbol: String = "₹"): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return "Payment amount cannot be empty"
        }
        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null) {
            return "Please enter a valid numeric value"
        }
        if (parsed.isNaN() || parsed.isInfinite()) {
            return "Amount must be a finite number"
        }
        if (parsed <= 0.0) {
            return "Amount must be greater than 0 (positive numeric value)"
        }
        if (parsed > 10_000_000.0) {
            return "Amount exceeds maximum limit ($currencySymbol 10,000,000)"
        }
        return null
    }

    @Test
    fun `sanitizeNumericInput filters out non-digits and restricts decimals`() {
        assertEquals("15000", sanitizeNumericInput("abc15000xyz"))
        assertEquals("15000.50", sanitizeNumericInput("15000.5099"))
        assertEquals("15000.50", sanitizeNumericInput("₹15000..50.."))
        assertEquals("2000.75", sanitizeNumericInput("2000.75"))
    }

    @Test
    fun `validateAmount rejects empty, zero, negative, or invalid strings`() {
        assertNotNull(validateAmount(""))
        assertNotNull(validateAmount("   "))
        assertNotNull(validateAmount("0"))
        assertNotNull(validateAmount("0.00"))
        assertNotNull(validateAmount("-500"))
        assertNotNull(validateAmount("abc"))
        assertNotNull(validateAmount("15000000")) // Exceeds 10,000,000 limit
    }

    @Test
    fun `validateAmount accepts valid positive numbers`() {
        assertNull(validateAmount("500"))
        assertNull(validateAmount("15000"))
        assertNull(validateAmount("2500.50"))
        assertNull(validateAmount("9999999"))
    }
}
