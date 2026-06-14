package com.example.kanjoos.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String? = null,
    val amount: Double,
    val label: String? = null,
    val created_at: String? = null
)
