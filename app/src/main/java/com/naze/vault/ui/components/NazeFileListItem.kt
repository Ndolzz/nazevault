package com.naze.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.naze.vault.data.model.VaultFileKind
import com.naze.vault.data.model.VaultNode
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazePurple
import com.naze.vault.ui.theme.NazeSurfaceElevated
import com.naze.vault.ui.theme.NazeTextSecondary
import com.naze.vault.ui.theme.NazeWarning
import com.naze.vault.util.FileUtils

fun iconFor(kind: VaultFileKind): ImageVector = when (kind) {
    VaultFileKind.FOLDER -> Icons.Filled.Folder
    VaultFileKind.CODE -> Icons.Filled.Code
    VaultFileKind.TEXT -> Icons.Filled.Description
    VaultFileKind.MARKDOWN -> Icons.Filled.Description
    VaultFileKind.JSON -> Icons.Filled.DataObject
    VaultFileKind.IMAGE -> Icons.Filled.Image
    VaultFileKind.VIDEO -> Icons.Filled.VideoFile
    VaultFileKind.AUDIO -> Icons.Filled.AudioFile
    VaultFileKind.PDF -> Icons.Filled.PictureAsPdf
    VaultFileKind.ARCHIVE -> Icons.Filled.FolderZip
    VaultFileKind.UNKNOWN -> Icons.Filled.InsertDriveFile
}

@Composable
fun NazeFileListItem(
    node: VaultNode,
    selected: Boolean,
    selectionModeActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) NazeBlue.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (node.isDirectory) NazePurple.copy(alpha = 0.18f) else NazeBlue.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectionModeActive) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else iconFor(node.kind),
                    contentDescription = null,
                    tint = if (selected) NazeBlue else NazeTextSecondary
                )
            } else {
                Icon(imageVector = iconFor(node.kind), contentDescription = null, tint = if (node.isDirectory) NazePurple else NazeBlue)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(text = node.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            val subtitle = if (node.isDirectory) "Folder" else FileUtils.formatSize(node.sizeBytes)
            Text(
                text = "$subtitle • ${FileUtils.formatDate(node.lastModified)}",
                style = MaterialTheme.typography.bodyMedium,
                color = NazeTextSecondary,
                maxLines = 1
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (node.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Favorite",
                tint = if (node.isFavorite) NazeWarning else NazeTextSecondary
            )
        }
    }
}
