package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM bank_state WHERE id = 1 LIMIT 1")
    suspend fun getBankState(): BankState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBankState(state: BankState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CardTransaction)

    @Query("SELECT * FROM card_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<CardTransaction>>
}
