package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_state")
data class BankState(
    @PrimaryKey val id: Int = 1,
    val balance: Long = 0,
    val isIncomeActive: Boolean = true
)
