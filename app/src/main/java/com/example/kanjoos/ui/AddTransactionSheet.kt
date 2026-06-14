package com.example.kanjoos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddTransactionSheet(
    onSave: (amount: Double, label: String?) -> Unit,
    onClose: () -> Unit
) {
    val amountText = remember { mutableStateOf("") }
    val labelText = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = amountText.value,
            onValueChange = { amountText.value = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = labelText.value,
            onValueChange = { labelText.value = it },
            label = { Text("Label (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val amt = amountText.value.toDoubleOrNull() ?: 0.0
                if (amt != 0.0) {
                    onSave(amt, labelText.value.ifBlank { null })
                    onClose()
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Save")
        }
    }
}