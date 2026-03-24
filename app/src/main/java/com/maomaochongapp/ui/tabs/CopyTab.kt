package com.maomaochongapp.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maomaochongapp.MainUiState
import com.maomaochongapp.ui.components.ExportPreviewPane

@Composable
fun CopyTab(
  state: MainUiState,
  enabled: Boolean,
  onOverrideStartIndexChange: (String) -> Unit,
  onBuildPreview: () -> Unit,
  onApplyCopy: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("已选择文件：${state.selectedFileNames.size}", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      value = state.copyOverrideStartIndex,
      onValueChange = onOverrideStartIndexChange,
      label = { Text("本次起始序号（可选，留空用设置页序号）") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = onBuildPreview, enabled = enabled) { Text("生成复制预览") }
      Button(onClick = onApplyCopy, enabled = enabled) { Text("执行复制") }
    }
    state.plannedCopyNextAfter?.let { Text("本次完成后序号将更新为：$it", style = MaterialTheme.typography.bodySmall) }
    HorizontalDivider()
    ExportPreviewPane(items = state.copyPreview)
    state.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
  }
}

