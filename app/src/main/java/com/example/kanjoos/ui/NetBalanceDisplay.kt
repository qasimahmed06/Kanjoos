package com.example.kanjoos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun NetBalanceDisplay(
    monthLabel: String,
    amount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when {
        amount > 0.0 -> Color(0xFF4CAF50)
        amount < 0.0 -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Text(
            text = formatCurrencyWithSign(amount),
            color = color,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatCurrencyWithSign(amount: Double): String {
    if (amount == 0.0) return "Rs. 0"
    val abs = kotlin.math.abs(amount)
    val df = DecimalFormat("#,##0")
    val formatted = df.format(abs)
    return if (amount > 0.0) "+Rs. $formatted" else "−Rs. $formatted"
}