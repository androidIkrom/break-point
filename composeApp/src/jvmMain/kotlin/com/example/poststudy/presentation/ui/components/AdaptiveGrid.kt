package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lays [items] out in as many equal columns as fit (each at least [minItemWidth]); cards in a
 * row share the tallest card's height so long texts wrap instead of being cut off. Works inside
 * scrolling columns, unlike lazy grids.
 */
@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 24.dp,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit
) {
    BoxWithConstraints(modifier) {
        val columns = ((maxWidth + spacing) / (minItemWidth + spacing)).toInt().coerceIn(1, items.size.coerceAtLeast(1))
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowItems.forEach { item ->
                        itemContent(item, Modifier.weight(1f).fillMaxHeight())
                    }
                    // Keep the last row's cards the same width as the others
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
