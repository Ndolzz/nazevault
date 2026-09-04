package com.naze.vault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naze.vault.ui.theme.NazeTextPrimary
import com.naze.vault.ui.theme.NazeTextSecondary

/**
 * Renders "Vault / Projects / Website / src" with each segment tappable to
 * jump straight back to that level.
 */
@Composable
fun NazeBreadcrumb(
    segments: List<String>,
    onSegmentClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.lastIndex
            Text(
                text = segment,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast) NazeTextPrimary else NazeTextSecondary,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable(enabled = !isLast) { onSegmentClick(index) }
            )
            if (!isLast) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NazeTextSecondary
                )
            }
        }
    }
}
