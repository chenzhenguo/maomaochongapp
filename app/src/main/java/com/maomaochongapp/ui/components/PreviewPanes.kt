package com.maomaochongapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maomaochongapp.download.DownloadPreviewItem
import com.maomaochongapp.export.ExportPreviewItem
import com.maomaochongapp.renamer.RenamePreviewItem

@Composable
fun RenamePreviewPane(items: List<RenamePreviewItem>) {
  if (items.isEmpty()) {
    Text("改名预览为空。", style = MaterialTheme.typography.bodySmall)
    return
  }
  LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
    items(items, key = { it.oldName }) { item ->
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(item.oldName, style = MaterialTheme.typography.bodySmall)
        Text(
          "→ ${item.newName}",
          style = MaterialTheme.typography.bodyMedium,
          color = if (item.conflict == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        item.conflict?.let { Text("冲突：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    }
  }
}

@Composable
fun ExportPreviewPane(items: List<ExportPreviewItem>) {
  if (items.isEmpty()) {
    Text("预览为空。", style = MaterialTheme.typography.bodySmall)
    return
  }
  LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
    items(items, key = { it.oldName }) { item ->
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(item.oldName, style = MaterialTheme.typography.bodySmall)
        Text(
          "→ ${item.newName}  (${item.destRelativePath})",
          style = MaterialTheme.typography.bodyMedium,
          color = if (item.conflict == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        item.conflict?.let { Text("冲突：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    }
  }
}

@Composable
fun DownloadPreviewPane(items: List<DownloadPreviewItem>) {
  if (items.isEmpty()) {
    Text("预览为空。", style = MaterialTheme.typography.bodySmall)
    return
  }
  LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
    items(items, key = { it.url }) { item ->
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(item.url, style = MaterialTheme.typography.bodySmall)
        Text(
          "→ ${item.destName}  (${item.destRelativePath})",
          style = MaterialTheme.typography.bodyMedium,
          color = if (item.conflict == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        item.conflict?.let { Text("冲突：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      }
    }
  }
}

