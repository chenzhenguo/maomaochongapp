package com.maomaochongapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SequenceEditor(
  next: Int,
  width: Int,
  prefix: String,
  avoidDuplicates: Boolean,
  enabled: Boolean,
  onNextChange: (Int) -> Unit,
  onWidthChange: (Int) -> Unit,
  onPrefixChange: (String) -> Unit,
  onAvoidDupChange: (Boolean) -> Unit,
  onScanNext: () -> Unit,
) {
  var nextText by remember(next) { mutableStateOf(next.toString()) }
  var widthText by remember(width) { mutableStateOf(width.toString()) }
  var prefixText by remember(prefix) { mutableStateOf(prefix) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("序号配置", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        enabled = enabled,
        value = nextText,
        onValueChange = {
          nextText = it
          it.toIntOrNull()?.let(onNextChange)
        },
        label = { Text("当前序号（下一个）") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
      )
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        enabled = enabled,
        value = widthText,
        onValueChange = {
          widthText = it
          it.toIntOrNull()?.let(onWidthChange)
        },
        label = { Text("位数（0001）") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
      )
    }
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      value = prefixText,
      onValueChange = {
        prefixText = it
        onPrefixChange(it)
      },
      label = { Text("前缀（可选，例如 REC）") },
      singleLine = true,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("一键扫描目标目录并跳号：", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
      Button(onClick = onScanNext, enabled = enabled) { Text("扫描并更新") }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Checkbox(enabled = enabled, checked = avoidDuplicates, onCheckedChange = onAvoidDupChange)
      Text("避免序号重复（扫描目标目录占用序号）", style = MaterialTheme.typography.bodySmall)
    }
  }
}
