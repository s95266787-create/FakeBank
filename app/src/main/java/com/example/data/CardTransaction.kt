package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_transactions")
data class CardTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetCard: String,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis()
)
