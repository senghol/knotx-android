package com.senghol.knotx.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.ListField
import com.senghol.knotx.ui.components.formatPrice
import com.senghol.knotx.ui.components.rememberListColors

@Composable
fun ListSummaryRow(
    listData: ListData,
    onClick: () -> Unit,
) {
    val totalItems = listData.items.size
    val checkedItems = listData.items.count { it.checked }
    val totalPrice = listData.items.mapNotNull { it.price }.sum()
    val showPrice = ListField.PRICE in listData.enabledFields && totalPrice > 0.0

    val colors = rememberListColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listData.title.ifBlank { "Untitled list" },
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (totalItems == 0) "No items" else "$checkedItems of $totalItems done",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (showPrice) {
            Text(
                text = formatPrice(totalPrice),
                color = colors.price,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
