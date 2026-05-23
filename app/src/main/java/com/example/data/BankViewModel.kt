package com.example.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface TransferStatus {
    object Idle : TransferStatus
    data class Success(val amount: Long, val card: String) : TransferStatus
    data class Error(val message: String) : TransferStatus
}

class BankViewModel(private val repository: BankRepository) : ViewModel() {

    private val _balance = MutableStateFlow<Long>(0)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    private val _isIncomeActive = MutableStateFlow<Boolean>(true)
    val isIncomeActive: StateFlow<Boolean> = _isIncomeActive.asStateFlow()

    private val _targetCardNumber = MutableStateFlow("")
    val targetCardNumber: StateFlow<String> = _targetCardNumber.asStateFlow()

    private val _transferAmount = MutableStateFlow("")
    val transferAmount: StateFlow<String> = _transferAmount.asStateFlow()

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    val transactions: StateFlow<List<CardTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val state = repository.getBankState()
            _balance.value = state.balance
            _isIncomeActive.value = state.isIncomeActive
            startTicker()
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                if (_isIncomeActive.value) {
                    _balance.update { it + 1 }
                    // Persist periodically or on each update
                    repository.saveBankState(
                        BankState(
                            balance = _balance.value,
                            isIncomeActive = _isIncomeActive.value
                        )
                    )
                }
            }
        }
    }

    fun toggleIncome() {
        viewModelScope.launch {
            val nextState = !_isIncomeActive.value
            _isIncomeActive.value = nextState
            repository.saveBankState(
                BankState(
                    balance = _balance.value,
                    isIncomeActive = nextState
                )
            )
        }
    }

    fun onCardNumberChanged(newValue: String) {
        // Remove spaces first
        val digitsOnly = newValue.replace(" ", "").filter { it.isDigit() }
        val truncated = if (digitsOnly.length > 16) digitsOnly.take(16) else digitsOnly
        // Group by 4 with spaces
        val formatted = truncated.chunked(4).joinToString(" ")
        _targetCardNumber.value = formatted
    }

    fun onAmountChanged(newValue: String) {
        val digitsOnly = newValue.filter { it.isDigit() }
        _transferAmount.value = if (digitsOnly.length > 9) digitsOnly.take(9) else digitsOnly
    }

    fun dismissStatus() {
        _transferStatus.value = TransferStatus.Idle
    }

    fun executeTransfer() {
        val card = _targetCardNumber.value
        val digits = card.replace(" ", "")
        val amountStr = _transferAmount.value

        if (digits.length != 16) {
            _transferStatus.value = TransferStatus.Error("Введите правильный 16-значный номер карты.")
            return
        }

        val amount = amountStr.toLongOrNull() ?: 0
        if (amount <= 0) {
            _transferStatus.value = TransferStatus.Error("Введите корректную сумму больше нуля.")
            return
        }

        val currentBalance = _balance.value
        if (amount > currentBalance) {
            _transferStatus.value = TransferStatus.Error("Недостаточно средств. Баланс: $currentBalance ₴")
            return
        }

        viewModelScope.launch {
            val newBalance = currentBalance - amount
            _balance.value = newBalance

            // Save state
            repository.saveBankState(
                BankState(
                    balance = newBalance,
                    isIncomeActive = _isIncomeActive.value
                )
            )

            // Log Transaction
            repository.addTransaction(
                CardTransaction(
                    targetCard = card,
                    amount = amount
                )
            )

            // Clear fields on success
            _targetCardNumber.value = ""
            _transferAmount.value = ""

            // Push Success status
            _transferStatus.value = TransferStatus.Success(amount, card)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}

class BankViewModelFactory(private val repository: BankRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BankViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BankViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
