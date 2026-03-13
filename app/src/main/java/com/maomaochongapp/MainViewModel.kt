package com.maomaochongapp

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maomaochongapp.export.ExportPreviewItem
import com.maomaochongapp.export.SafExporter
import com.maomaochongapp.renamer.RenameMode
import com.maomaochongapp.renamer.RenamePlanner
import com.maomaochongapp.renamer.RenamePreviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
  val folderUri: Uri? = null,
  val folderName: String? = null,
  val files: List<DocumentFile> = emptyList(),
  val mode: RenameMode = RenameMode.IndexPrefix(keepOriginal = false),
  val preview: List<RenamePreviewItem> = emptyList(),
  val exportRootUri: Uri? = null,
  val exportRootName: String? = null,
  val collectionName: String = "",
  val bookName: String = "",
  val targetSubdir: String = "MP3",
  val exportPreview: List<ExportPreviewItem> = emptyList(),
  val exportOverwrite: Boolean = false,
  val lastMessage: String? = null,
  val isBusy: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val _state = MutableStateFlow(MainUiState())
  val state: StateFlow<MainUiState> = _state

  fun setMode(mode: RenameMode) {
    _state.update { it.copy(mode = mode, preview = emptyList(), lastMessage = null) }
  }

  fun onFolderPicked(treeUri: Uri) {
    val cr = getApplication<Application>().contentResolver
    try {
      cr.takePersistableUriPermission(
        treeUri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
          android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (_: SecurityException) {
      // Some providers don't support persistable permissions; still try to use it for this session.
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val folder = DocumentFile.fromTreeUri(getApplication(), treeUri)
      val children = withContext(Dispatchers.IO) {
        folder?.listFiles()
          ?.filter { it.isFile }
          ?.toList()
          ?: emptyList()
      }
      _state.update {
        it.copy(
          folderUri = treeUri,
          folderName = folder?.name ?: treeUri.toString(),
          files = children,
          preview = emptyList(),
          isBusy = false,
        )
      }
    }
  }

  fun onExportRootPicked(treeUri: Uri) {
    val cr = getApplication<Application>().contentResolver
    try {
      cr.takePersistableUriPermission(
        treeUri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
          android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    } catch (_: SecurityException) {
      // Ignore.
    }

    val folder = DocumentFile.fromTreeUri(getApplication(), treeUri)
    _state.update {
      it.copy(
        exportRootUri = treeUri,
        exportRootName = folder?.name ?: treeUri.toString(),
        exportPreview = emptyList(),
        lastMessage = null,
      )
    }
  }

  fun setCollectionName(name: String) {
    _state.update { it.copy(collectionName = name, exportPreview = emptyList()) }
  }

  fun setBookName(name: String) {
    _state.update { it.copy(bookName = name, exportPreview = emptyList()) }
  }

  fun setTargetSubdir(name: String) {
    _state.update { it.copy(targetSubdir = name, exportPreview = emptyList()) }
  }

  fun setExportOverwrite(overwrite: Boolean) {
    _state.update { it.copy(exportOverwrite = overwrite) }
  }

  fun buildPreview() {
    val files = state.value.files
    if (files.isEmpty()) {
      _state.update { it.copy(lastMessage = "当前目录没有可处理的文件。") }
      return
    }

    val names = files.mapNotNull { it.name }
    val preview = RenamePlanner.plan(names, state.value.mode)
    _state.update { it.copy(preview = preview, lastMessage = "预览已生成：${preview.size} 项") }
  }

  fun buildExportPreview() {
    val folderUri = state.value.folderUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = state.value.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    if (state.value.collectionName.trim().isBlank() || state.value.bookName.trim().isBlank()) {
      _state.update { it.copy(lastMessage = "请填写绘本集与绘本名称。") }
      return
    }
    if (state.value.targetSubdir.trim().isBlank()) {
      _state.update { it.copy(lastMessage = "请填写目标子目录（例如 MP3 / DIYAUDIO / BOOK）。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null, exportPreview = emptyList()) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)

      val result = withContext(Dispatchers.IO) {
        val names = sourceFolder?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name } ?: emptyList()
        val planned = RenamePlanner.plan(names, state.value.mode)
        val rel = SafExporter.buildDestRelativePath(state.value.collectionName, state.value.bookName, state.value.targetSubdir)
        val destDir = if (destRoot != null) SafExporter.ensureDirectory(destRoot, rel.split('/').filter { it.isNotBlank() }) else null
        val dirError = if (destRoot != null && destDir == null) "无法创建目标目录" else null
        val existing = if (state.value.exportOverwrite) {
          emptySet()
        } else {
          destDir?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name }?.toSet() ?: emptySet()
        }
        planned.map { p ->
          val invalid = if (p.newName.contains('/') || p.newName.contains('\\')) "文件名包含非法字符" else null
          val conflict = p.conflict ?: dirError ?: invalid ?: if (p.newName in existing) "目标已存在" else null
          ExportPreviewItem(
            oldName = p.oldName,
            newName = p.newName,
            destRelativePath = rel,
            conflict = conflict,
          )
        }
      }

      _state.update {
        it.copy(
          exportPreview = result,
          isBusy = false,
          lastMessage = "导出预览已生成：${result.size} 项",
        )
      }
    }
  }

  fun applyRename() {
    val folderUri = state.value.folderUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择文件夹。") }
      return
    }
    val preview = state.value.preview
    if (preview.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先生成预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val folder = DocumentFile.fromTreeUri(app, folderUri)
      val byName = folder?.listFiles()?.filter { it.isFile }?.associateBy { it.name } ?: emptyMap()

      val (ok, skipped, failed) = withContext(Dispatchers.IO) {
        var okCount = 0
        var skippedCount = 0
        var failedCount = 0
        for (item in preview) {
          if (item.conflict != null) {
            skippedCount++
            continue
          }
          val file = byName[item.oldName]
          if (file == null) {
            failedCount++
            continue
          }
          val renamed = try {
            file.renameTo(item.newName)
          } catch (_: Throwable) {
            false
          }
          if (renamed) okCount++ else failedCount++
        }
        Triple(okCount, skippedCount, failedCount)
      }

      // Refresh file list after rename.
      val refreshed = withContext(Dispatchers.IO) {
        folder?.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
      }
      _state.update {
        it.copy(
          files = refreshed,
          preview = emptyList(),
          isBusy = false,
          lastMessage = "完成：成功 $ok，跳过冲突 $skipped，失败 $failed",
        )
      }
    }
  }

  fun applyExport(move: Boolean) {
    val folderUri = state.value.folderUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = state.value.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val preview = state.value.exportPreview
    if (preview.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先生成导出预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)

      val resultMessage = withContext(Dispatchers.IO) {
        try {
          if (sourceFolder == null || destRoot == null) return@withContext "无法读取源/目标目录。"
          val plan = preview
            .filter { it.conflict == null }
            .map { it.oldName to it.newName }
          val r = SafExporter.export(
            context = app,
            sourceFolder = sourceFolder,
            destRoot = destRoot,
            collectionName = state.value.collectionName,
            bookName = state.value.bookName,
            targetSubdir = state.value.targetSubdir,
            plan = plan,
            move = move,
            overwrite = state.value.exportOverwrite,
          )
          "导出完成：成功 ${r.ok}，跳过 ${r.skipped}，失败 ${r.failed}"
        } catch (t: Throwable) {
          "导出失败：${t.message ?: t::class.java.simpleName}"
        }
      }

      // Refresh source list (move 会减少源文件；copy 不变也刷新一下)
      val refreshed = withContext(Dispatchers.IO) {
        sourceFolder?.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
      }
      _state.update {
        it.copy(
          files = refreshed,
          exportPreview = emptyList(),
          isBusy = false,
          lastMessage = resultMessage,
        )
      }
    }
  }
}
