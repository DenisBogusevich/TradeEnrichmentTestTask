package com.testtask.tradeenrichmentservice.model

import jakarta.validation.constraints.Pattern
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Data class representing a trade record.
 * Includes validation for date format and required fields.
 *
 * @property dateStr Date in yyyyMMdd format
 * @property productIdOrName Product identifier or name
 * @property currency Trade currency
 * @property price Trade price
 */
data class TradeRecord(
    @field:Pattern(
        regexp = "\\d{8}",
        message = "Date must be in format yyyyMMdd"
    )
    val dateStr: String,

    val productIdOrName: String,
    val currency: String,
    val price: String
) {
    init {
        require(isValidDate(dateStr)) { "Invalid date format: $dateStr. Expected format: yyyyMMdd." }
    }

    companion object {
        private fun isValidDate(date: String): Boolean {
            return try {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                LocalDate.parse(date, formatter)
                true
            } catch (e: DateTimeParseException) {
                false
            }
        }
    }
}