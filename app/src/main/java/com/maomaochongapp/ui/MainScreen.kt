package com.maomaochongapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maomaochongapp.MainViewModel
import com.maomaochongapp.renamer.RenameMode
import com.maomaochongapp.renamer.Sort

@Composable
fun MainScreen(viewModel: MainViewModel) {
  val state by viewModel.state.collectAsState()

  val pickFolderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
    onResult = { uri: Uri? -> if (uri != null) viewModel.onFolderPicked(uri) },
  )
  val pickExportRootLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
    onResult = { uri: Uri? -> if (uri != null) viewModel.onExportRootPicked(uri) },
  )

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { pickFolderLauncher.launch(null) }, enabled = !state.isBusy) { Text("选择源文件夹") }
        Button(onClick = { pickExportRootLauncher.launch(null) }, enabled = !state.isBusy) { Text("选择目标目录") }
      }
      if (state.isBusy) CircularProgressIndicator(modifier = Modifier.height(24.dp))
    }

    Text(
      text = "当前目录：${state.folderName ?: "未选择"}（${state.files.size} 文件）",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text = "目标目录：${state.exportRootName ?: "未选择"}",
      style = MaterialTheme.typography.bodyMedium,
    )

    ModeEditor(
      mode = state.mode,
      onModeChange = viewModel::setMode,
      enabled = !state.isBusy,
    )

    ExportEditor(
      collectionName = state.collectionName,
      bookName = state.bookName,
      targetSubdir = state.targetSubdir,
      overwrite = state.exportOverwrite,
      onCollectionChange = viewModel::setCollectionName,
      onBookChange = viewModel::setBookName,
      onTargetSubdirChange = viewModel::setTargetSubdir,
      onOverwriteChange = viewModel::setExportOverwrite,
      enabled = !state.isBusy,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = viewModel::buildPreview, enabled = !state.isBusy) { Text("改名预览") }
      Button(onClick = viewModel::applyRename, enabled = !state.isBusy) { Text("仅在源目录改名") }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = viewModel::buildExportPreview, enabled = !state.isBusy) { Text("导出预览") }
      Button(onClick = { viewModel.applyExport(move = false) }, enabled = !state.isBusy) { Text("复制导出") }
      Button(onClick = { viewModel.applyExport(move = true) }, enabled = !state.isBusy) { Text("移动导出") }
    }

    state.lastMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

    Divider()

    PreviewPane(
      renamePreviewEmptyHint = "改名预览为空：先选择源目录，再点“改名预览”。",
      exportPreviewEmptyHint = "导出预览为空：先选源/目标目录，填绘本集与绘本名，再点“导出预览”。",
      showExport = state.exportPreview.isNotEmpty(),
      renamePreview = state.preview,
      exportPreview = state.exportPreview,
    )
  }
}

@Composable
private fun ModeEditor(
  mode: RenameMode,
  onModeChange: (RenameMode) -> Unit,
  enabled: Boolean,
) {
  var expanded by remember { mutableStateOf(false) }
  val label = when (mode) {
    is RenameMode.RecSticker -> "点读贴 REC"
    is RenameMode.IndexPrefix -> "顺序编号"
    is RenameMode.RegexReplace -> "正则替换"
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = { expanded = true }, enabled = enabled) { Text("规则：$label") }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
          text = { Text("点读贴 REC") },
          onClick = {
            expanded = false
            onModeChange(RenameMode.RecSticker(startCode = 1201))
          },
        )
        DropdownMenuItem(
          text = { Text("顺序编号") },
          onClick = {
            expanded = false
            onModeChange(RenameMode.IndexPrefix())
          },
        )
        DropdownMenuItem(
          text = { Text("正则替换") },
          onClick = {
            expanded = false
            onModeChange(RenameMode.RegexReplace(pattern = " ", replacement = "_"))
          },
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
    Text("保留原文件名（0001_原名）", style = MaterialTheme.typography.bodySmall)
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
    label = { Text("pattern（Kotlin Regex）") },
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
    Sort.ByNameAsc -> "按名称升序"
    Sort.ByNameDesc -> "按名称降序"
  }

  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("排序：$label", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(1.dp))
    Button(onClick = { expanded = true }, enabled = enabled) { Text("切换") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
        text = { Text("按名称升序") },
        onClick = { expanded = false; onChange(Sort.ByNameAsc) },
      )
      DropdownMenuItem(
        text = { Text("按名称降序") },
        onClick = { expanded = false; onChange(Sort.ByNameDesc) },
      )
    }
  }
}

@Composable
private fun ExportEditor(
  collectionName: String,
  bookName: String,
  targetSubdir: String,
  overwrite: Boolean,
  onCollectionChange: (String) -> Unit,
  onBookChange: (String) -> Unit,
  onTargetSubdirChange: (String) -> Unit,
  onOverwriteChange: (Boolean) -> Unit,
  enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("导出流程：设置绘本集/绘本名 → 导出到 目标目录/绘本集/绘本名/子目录", style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        enabled = enabled,
        value = collectionName,
        onValueChange = onCollectionChange,
        label = { Text("绘本集") },
        singleLine = true,
      )
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        enabled = enabled,
        value = bookName,
        onValueChange = onBookChange,
        label = { Text("绘本名称") },
        singleLine = true,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        enabled = enabled,
        value = targetSubdir,
        onValueChange = onTargetSubdirChange,
        label = { Text("目标子目录") },
        singleLine = true,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(enabled = enabled, checked = overwrite, onCheckedChange = onOverwriteChange)
        Text("覆盖同名", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun PreviewPane(
  renamePreviewEmptyHint: String,
  exportPreviewEmptyHint: String,
  showExport: Boolean,
  renamePreview: List<com.maomaochongapp.renamer.RenamePreviewItem>,
  exportPreview: List<com.maomaochongapp.export.ExportPreviewItem>,
) {
  if (showExport) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(exportPreview, key = { it.oldName }) { item ->
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(text = item.oldName, style = MaterialTheme.typography.bodySmall)
          Text(
            text = "→ ${item.newName}  (${item.destRelativePath})",
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.conflict == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
          )
          item.conflict?.let { Text("冲突：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
      }
    }
  } else if (renamePreview.isNotEmpty()) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(renamePreview, key = { it.oldName }) { item ->
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(text = item.oldName, style = MaterialTheme.typography.bodySmall)
          Text(
            text = "→ ${item.newName}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.conflict == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
          )
          item.conflict?.let { Text("冲突：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
      }
    }
  } else {
    Text(renamePreviewEmptyHint, style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(4.dp))
    Text(exportPreviewEmptyHint, style = MaterialTheme.typography.bodySmall)
  }
}
