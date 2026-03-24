package com.maomaochongapp.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maomaochongapp.MainUiState
import com.maomaochongapp.ui.components.SequenceEditor
import com.maomaochongapp.ui.components.TargetSubdirEditor

@Composable
fun SettingsTab(
  state: MainUiState,
  enabled: Boolean,
  onPickSource: () -> Unit,
  onPickTarget: () -> Unit,
  onTargetSubdirChange: (String) -> Unit,
  onOverwriteChange: (Boolean) -> Unit,
  onSequenceNextChange: (Int) -> Unit,
  onSequenceWidthChange: (Int) -> Unit,
  onSequencePrefixChange: (String) -> Unit,
  onAvoidDupChange: (Boolean) -> Unit,
  onScanNextIndex: () -> Unit,
  onExportDebug: () -> Unit,
  onClearDebug: () -> Unit,
) {
  val scroll = rememberScrollState()
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(scroll),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = onPickSource, enabled = enabled) { Text("选择源文件夹") }
      Button(onClick = onPickTarget, enabled = enabled) { Text("选择目标目录") }
    }
    Text("源：${state.folderName ?: "未选择"}", style = MaterialTheme.typography.bodySmall)
    Text("目标：${state.exportRootName ?: "未选择"}", style = MaterialTheme.typography.bodySmall)

    TargetSubdirEditor(selected = state.targetSubdir, enabled = enabled, onChange = onTargetSubdirChange)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Checkbox(enabled = enabled, checked = state.exportOverwrite, onCheckedChange = onOverwriteChange)
      Text("覆盖同名", style = MaterialTheme.typography.bodySmall)
    }

    SequenceEditor(
      next = state.sequenceNext,
      width = state.sequenceWidth,
      prefix = state.sequencePrefix,
      avoidDuplicates = state.avoidDuplicateSequence,
      enabled = enabled,
      onNextChange = onSequenceNextChange,
      onWidthChange = onSequenceWidthChange,
      onPrefixChange = onSequencePrefixChange,
      onAvoidDupChange = onAvoidDupChange,
      onScanNext = onScanNextIndex,
    )

    HorizontalDivider()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Button(onClick = onExportDebug, enabled = enabled) { Text("导出调试日志") }
      Button(onClick = onClearDebug, enabled = enabled && state.debugLogs.isNotEmpty()) { Text("清空日志") }
      Text("${state.debugLogs.size} 条", style = MaterialTheme.typography.bodySmall)
    }

    state.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
  }
}
