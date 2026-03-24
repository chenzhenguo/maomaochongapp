package com.maomaochongapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.maomaochongapp.renamer.RenameMode
import com.maomaochongapp.renamer.Sort

@Composable
fun RenameModeEditor(
  mode: RenameMode,
  onModeChange: (RenameMode) -> Unit,
  enabled: Boolean,
) {
  var expanded by remember { mutableStateOf(false) }
  val label = when (mode) {
    is RenameMode.RecSticker -> "点读�?REC"
    is RenameMode.IndexPrefix -> "顺序编号"
    is RenameMode.RegexReplace -> "正则替换"
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = { expanded = true }, enabled = enabled) { Text("改名规则�?label") }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
          text = { Text("点读�?REC") },
          onClick = { expanded = false; onModeChange(RenameMode.RecSticker(startCode = 1201)) },
        )
        DropdownMenuItem(
          text = { Text("顺序编号") },
          onClick = { expanded = false; onModeChange(RenameMode.IndexPrefix()) },
        )
        DropdownMenuItem(
          text = { Text("正则替换") },
          onClick = { expanded = false; onModeChange(RenameMode.RegexReplace(pattern = " ", replacement = "_")) },
        )
      }
    }

    when (mode) {
      is RenameMode.RecSticker -> RecStickerEditor(mode, onModeChange, enabled)
      is RenameMode.IndexPrefix -> IndexPrefixEditor(mode, onModeChange, enabled)
      is RenameMode.RegexReplace -> RegexEditor(mode, onModeChange, enabled)
    }
  }
}

@Composable
private fun RecStickerEditor(
  mode: RenameMode.RecSticker,
  onModeChange: (RenameMode) -> Unit,
  enabled: Boolean,
) {
  var startCodeText by remember(mode.startCode) { mutableStateOf(mode.startCode.toString()) }
  var digitsText by remember(mode.digits) { mutableStateOf(mode.digits.toString()) }

  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      enabled = enabled,
      value = startCodeText,
      onValueChange = {
        startCodeText = it
        val n = it.toIntOrNull() ?: return@OutlinedTextField
        onModeChange(mode.copy(startCode = n))
      },
      label = { Text("起始编码") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
    )
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      enabled = enabled,
      value = digitsText,
      onValueChange = {
        digitsText = it
        val n = it.toIntOrNull() ?: return@OutlinedTextField
        onModeChange(mode.copy(digits = n.coerceIn(1, 8)))
      },
      label = { Text("位数") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
    )
  }
  SortEditor(selected = mode.sort, enabled = enabled) { onModeChange(mode.copy(sort = it)) }
}

@Composable
private fun IndexPrefixEditor(
  mode: RenameMode.IndexPrefix,
  onModeChange: (RenameMode) -> Unit,
  enabled: Boolean,
) {
  var startIndexText by remember(mode.startIndex) { mutableStateOf(mode.startIndex.toString()) }
  var widthText by remember(mode.width) { mutableStateOf(mode.width.toString()) }

  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      enabled = enabled,
      value = startIndexText,
      onValueChange = {
        startIndexText = it
        val n = it.toIntOrNull() ?: return@OutlinedTextField
        onModeChange(mode.copy(startIndex = n.coerceAtLeast(0)))
      },
      label = { Text("起始序号") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
    )
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      enabled = enabled,
      value = widthText,
      onValueChange = {
        widthText = it
        val n = it.toIntOrNull() ?: return@OutlinedTextField
        onModeChange(mode.copy(width = n.coerceIn(1, 8)))
      },
      label = { Text("序号位数") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
    )
  }
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Checkbox(
      enabled = enabled,
      checked = mode.keepOriginal,
      onCheckedChange = { onModeChange(mode.copy(keepOriginal = it)) },
    )
    Text("保留原文件名�?001_原名�?, style = MaterialTheme.typography.bodySmall)
  }
  SortEditor(selected = mode.sort, enabled = enabled) { onModeChange(mode.copy(sort = it)) }
}

@Composable
private fun RegexEditor(
  mode: RenameMode.RegexReplace,
  onModeChange: (RenameMode) -> Unit,
  enabled: Boolean,
) {
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    value = mode.pattern,
    onValueChange = { onModeChange(mode.copy(pattern = it)) },
    label = { Text("pattern（Kotlin Regex�?) },
    singleLine = true,
  )
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    value = mode.replacement,
    onValueChange = { onModeChange(mode.copy(replacement = it)) },
    label = { Text("replacement") },
    singleLine = true,
  )
}

@Composable
private fun SortEditor(
  selected: Sort,
  enabled: Boolean,
  onChange: (Sort) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val label = when (selected) {
    Sort.ByNameAsc -> "按名称升�?
    Sort.ByNameDesc -> "按名称降�?
  }

  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("排序�?label", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(1.dp))
    Button(onClick = { expanded = true }, enabled = enabled) { Text("切换") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(text = { Text("按名称升�?) }, onClick = { expanded = false; onChange(Sort.ByNameAsc) })
      DropdownMenuItem(text = { Text("按名称降�?) }, onClick = { expanded = false; onChange(Sort.ByNameDesc) })
    }
  }
}

