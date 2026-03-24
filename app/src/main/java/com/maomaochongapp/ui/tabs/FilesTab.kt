package com.maomaochongapp.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.maomaochongapp.MainUiState
import com.maomaochongapp.renamer.RenameMode
import com.maomaochongapp.ui.components.RenameModeEditor
import com.maomaochongapp.ui.components.RenamePreviewPane

@Composable
fun FilesTab(
  state: MainUiState,
  enabled: Boolean,
  onToggleSelect: (String) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onBuildRenamePreview: () -> Unit,
  onApplyRename: () -> Unit,
  onModeChange: (RenameMode) -> Unit,
  onRefreshTarget: () -> Unit,
  onOpenFile: (DocumentFile) -> Unit,
) {
  var showTarget by rememberSaveable { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Button(onClick = { showTarget = false }, enabled = enabled) { Text("源文件夹") }
      Button(onClick = { showTarget = true; onRefreshTarget() }, enabled = enabled) { Text("目标子目录") }
      Text(
        if (showTarget) "目标：${state.exportRootName ?: "未选择"}" else "源：${state.folderName ?: "未选择"}",
        style = MaterialTheme.typography.bodySmall,
      )
    }

    if (!showTarget) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSelectAll, enabled = enabled) { Text("全选") }
        Button(onClick = onClearSelection, enabled = enabled) { Text("清空选择") }
        Text("已选：${state.selectedFileNames.size} / ${state.files.size}", style = MaterialTheme.typography.bodySmall)
      }

      RenameModeEditor(mode = state.mode, onModeChange = onModeChange, enabled = enabled)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onBuildRenamePreview, enabled = enabled) { Text("改名预览") }
        Button(onClick = onApplyRename, enabled = enabled) { Text("在源目录改名") }
      }

      HorizontalDivider()
      RenamePreviewPane(items = state.preview)
      HorizontalDivider()

      LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.files, key = { it.uri.toString() }) { file ->
          val name = file.name ?: return@items
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Checkbox(enabled = enabled, checked = name in state.selectedFileNames, onCheckedChange = { onToggleSelect(name) })
            Text(name, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onOpenFile(file) }, enabled = enabled) { Text("打开") }
          }
        }
      }
    } else {
      Text("目标文件数：${state.targetFiles.size}", style = MaterialTheme.typography.bodySmall)
      LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.targetFiles, key = { it.uri.toString() }) { file ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(file.name ?: "（无名）", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onOpenFile(file) }, enabled = enabled) { Text("打开") }
          }
        }
      }
    }

    state.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
  }
}
