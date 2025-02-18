package com.testtask.tradeenrichmentservice.model

data class TradeRecord(
    val dateStr: String,
    var productIdOrName: String,
    val currency: String,
    val price: String
)