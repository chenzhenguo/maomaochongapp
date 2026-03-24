package com.maomaochongapp.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.maomaochongapp.ui.components.DownloadPreviewPane

@Composable
fun DownloadTab(
  state: MainUiState,
  enabled: Boolean,
  onUrlsTextChange: (String) -> Unit,
  onOverrideStartIndexChange: (String) -> Unit,
  onBuildPreview: () -> Unit,
  onApplyDownload: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
      enabled = enabled,
      value = state.downloadUrlsText,
      onValueChange = onUrlsTextChange,
      label = { Text("URL 列表（每行一个）") },
    )
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
      Button(onClick = onBuildPreview, enabled = enabled) { Text("生成下载预览") }
      Button(onClick = onApplyDownload, enabled = enabled) { Text("执行下载") }
    }
    state.plannedDownloadNextAfter?.let { Text("本次完成后序号将更新为：$it", style = MaterialTheme.typography.bodySmall) }
    HorizontalDivider()
    DownloadPreviewPane(items = state.downloadPreview)
    state.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
  }
}

