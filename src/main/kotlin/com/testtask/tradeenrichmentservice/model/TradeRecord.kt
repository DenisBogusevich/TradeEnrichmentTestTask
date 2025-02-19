package com.testtask.tradeenrichmentservice.model

import jakarta.validation.constraints.Pattern
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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