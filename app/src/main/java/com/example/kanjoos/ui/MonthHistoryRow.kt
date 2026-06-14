package com.example.kanjoos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MonthHistoryRow(
    monthNets: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(monthNets) { pair ->
            val (label, amount) = pair
            val color = when {
                amount > 0.0 -> Color(0xFF4CAF50)
                amount < 0.0 -> Color(0xFFF44336)
                else -> Color(0xFF9E9E9E)
            }

            Card(
                modifier = Modifier
                    .width(140.dp)
                    .height(96.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = formatShortCurrency(amount),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = color,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatShortCurrency(amount: Double): String {
    if (amount == 0.0) return "Rs. 0"
    val abs = kotlin.math.abs(amount)
    val df = java.text.DecimalFormat("#,##0")
    val formatted = df.format(abs)
    return if (amount > 0.0) "+Rs. $formatted" else "−Rs. $formatted"
}
