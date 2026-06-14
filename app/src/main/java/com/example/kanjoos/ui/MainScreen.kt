package com.example.kanjoos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanjoos.MainViewModel
import com.example.kanjoos.UiState
import com.example.kanjoos.model.Transaction
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val showAddSheet = remember { mutableStateOf(false) }
    val showSubtractSheet = remember { mutableStateOf(false) }
    val showSummarySheet = remember { mutableStateOf(false) }
    val uiState by vm.uiState.collectAsState()
    val allTx by vm.allTransactions.collectAsState()
    val currentYearMonth = YearMonth.now()
    val currentMonthTx = allTx.filter { it.created_at?.startsWith(currentYearMonth.toString()) == true }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kanjoos") }, actions = {
                TextButton(onClick = { vm.fetchTransactions() }) {
                    Text("Refresh")
                }
            })
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (uiState) {
                is UiState.Loading -> Text("Loading...", modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text((uiState as UiState.Error).message, modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val success = uiState as UiState.Success
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NetBalanceDisplay(
                            monthLabel = success.currentMonthLabel,
                            amount = success.currentMonthNetBalance,
                            onClick = { showSummarySheet.value = true }
                        )
                        MonthHistoryRow(
                            monthNets = success.history.map { item -> item.label to item.netAmount },
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { showAddSheet.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Add")
                            }
                            Button(
                                onClick = { showSubtractSheet.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                            ) {
                                Text("Subtract")
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showAddSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AddTransactionSheet(
                onSave = { amount, label ->
                    vm.addTransaction(amount, label, true)
                    showAddSheet.value = false
                },
                onClose = { showAddSheet.value = false }
            )
        }
    }

    if (showSubtractSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showSubtractSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AddTransactionSheet(
                onSave = { amount, label ->
                    vm.addTransaction(amount, label, false)
                    showSubtractSheet.value = false
                },
                onClose = { showSubtractSheet.value = false }
            )
        }
    }

    if (showSummarySheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showSummarySheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TransactionSummarySheet(
                transactions = currentMonthTx,
                onDismiss = { showSummarySheet.value = false }
            )
        }
    }
}

@Composable
fun TransactionSummarySheet(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "Monthly Summary",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.padding(8.dp))
        if (transactions.isEmpty()) {
            Text("No transactions this month.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(transactions) { tx ->
                    TransactionItem(tx = tx)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction) {
    val color = when {
        tx.amount > 0 -> Color(0xFF4CAF50)
        tx.amount < 0 -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                tx.label ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                formatCurrency(tx.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    if (amount == 0.0) return "Rs. 0"
    val abs = kotlin.math.abs(amount)
    val df = java.text.DecimalFormat("#,##0")
    val formatted = df.format(abs)
    return if (amount > 0.0) "+Rs. $formatted" else "−Rs. $formatted"
}