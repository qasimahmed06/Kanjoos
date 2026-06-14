package com.example.kanjoos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanjoos.data.TransactionRepository
import com.example.kanjoos.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainViewModel(
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // NEW: expose all transactions as a StateFlow for UI layers
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    init {
        fetchTransactions()
    }

    fun fetchTransactions() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val transactions = repository.getAllTransactions()
                processTransactions(transactions)
                // Update the exposed list as well
                _allTransactions.value = transactions
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to fetch transactions")
            }
        }
    }

    fun addTransaction(amount: Double, label: String?, isIncome: Boolean) {
        viewModelScope.launch {
            try {
                val transactionAmount = if (isIncome) amount else -amount
                val newTransaction = Transaction(
                    amount = transactionAmount,
                    label = label?.takeIf { it.isNotBlank() }
                )
                repository.insertTransaction(newTransaction)
                val transactions = repository.getAllTransactions()
                processTransactions(transactions)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to save transaction")
            }
        }
    }

    private fun processTransactions(transactions: List<Transaction>) {
        val currentYearMonth = YearMonth.now()

        val grouped = transactions.mapNotNull { tx ->
            parseTransactionMonth(tx.created_at)?.let { it to tx }
        }.groupBy({ it.first }, { it.second })

        val currentMonthTransactions = grouped[currentYearMonth] ?: emptyList()
        val currentMonthNetBalance = currentMonthTransactions.sumOf { it.amount }
        val currentMonthLabel = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))

        val history = grouped.filterKeys { it != currentYearMonth }
            .map { (yearMonth, monthTransactions) ->
                val netAmount = monthTransactions.sumOf { it.amount }
                val label = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                MonthHistoryItem(yearMonth, label, netAmount)
            }
            .sortedByDescending { it.yearMonth }

        _uiState.value = UiState.Success(
            currentMonthNetBalance = currentMonthNetBalance,
            currentMonthLabel = currentMonthLabel,
            history = history
        )
    }

    private fun parseTransactionMonth(createdAt: String?): YearMonth? {
        if (createdAt.isNullOrBlank()) return null
        return try {
            YearMonth.parse(createdAt.substring(0, 7))
        } catch (e: Exception) {
            null
        }
    }
}

sealed interface UiState {
    object Loading : UiState
    data class Success(
        val currentMonthNetBalance: Double,
        val currentMonthLabel: String,
        val history: List<MonthHistoryItem>
    ) : UiState
    data class Error(val message: String) : UiState
}

data class MonthHistoryItem(
    val yearMonth: YearMonth,
    val label: String,
    val netAmount: Double
)