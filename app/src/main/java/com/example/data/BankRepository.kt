package com.example.data

import kotlinx.coroutines.flow.Flow

class BankRepository(private val bankDao: BankDao) {

    val allTransactions: Flow<List<CardTransaction>> = bankDao.getAllTransactions()

    suspend fun getBankState(): BankState {
        return bankDao.getBankState() ?: BankState(balance = 0, isIncomeActive = true).also {
            bankDao.saveBankState(it)
        }
    }

    suspend fun saveBankState(state: BankState) {
        bankDao.saveBankState(state)
    }

    suspend fun addTransaction(transaction: CardTransaction) {
        bankDao.insertTransaction(transaction)
    }
}
