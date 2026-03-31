package com.maomaochongapp

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maomaochongapp.download.DownloadPreviewItem
import com.maomaochongapp.download.DownloadResult
import com.maomaochongapp.export.ExportPreviewItem
import com.maomaochongapp.export.SafExporter
import com.maomaochongapp.renamer.RenameMode
import com.maomaochongapp.renamer.RenamePlanner
import com.maomaochongapp.renamer.RenamePreviewItem
import com.maomaochongapp.sequence.SequenceAllocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MainUiState(
  val folderUri: Uri? = null,
  val folderName: String? = null,
  val files: List<DocumentFile> = emptyList(),
  val selectedFileNames: Set<String> = emptySet(),
  val mode: RenameMode = RenameMode.IndexPrefix(keepOriginal = false),
  val preview: List<RenamePreviewItem> = emptyList(),
  val exportRootUri: Uri? = null,
  val exportRootName: String? = null,
  val targetFiles: List<DocumentFile> = emptyList(),
  val collectionName: String = "",
  val bookName: String = "",
  val targetSubdir: String = "MP3",
  val exportPreview: List<ExportPreviewItem> = emptyList(),
  val copyPreview: List<ExportPreviewItem> = emptyList(),
  val exportOverwrite: Boolean = false,
  val sequenceNext: Int = 1,
  val sequenceWidth: Int = 4,
  val sequencePrefix: String = "",
  val avoidDuplicateSequence: Boolean = true,
  val copyOverrideStartIndex: String = "",
  val plannedCopyNextAfter: Int? = null,
  val downloadUrlsText: String = "",
  val downloadPreview: List<DownloadPreviewItem> = emptyList(),
  val plannedDownloadNextAfter: Int? = null,
  val debugLogs: List<String> = emptyList(),
  val lastMessage: String? = null,
  val isBusy: Boolean = false,
  val downloadProgress: Map<String, Int> = emptyMap(), // URL -> progress percentage
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
  companion object {
    private const val DebugTag = "MaomaochongDebug"
    private const val MaxDebugLogs = 300
    private val DebugTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

    private const val PrefsName = "maomaochong_settings"
    private const val PrefFolderUri = "folder_uri"
    private const val PrefExportRootUri = "export_root_uri"
    private const val PrefTargetSubdir = "target_subdir"
    private const val PrefSequenceNext = "sequence_next"
    private const val PrefSequenceWidth = "sequence_width"
    private const val PrefSequencePrefix = "sequence_prefix"
    private const val PrefAvoidDup = "avoid_dup"
    private const val PrefOverwrite = "overwrite"
  }

  private val _state = MutableStateFlow(MainUiState())
  val state: StateFlow<MainUiState> = _state

  init {
    appendDebug("应用启动")
    restoreSettings()
  }

  private fun appendDebug(message: String, throwable: Throwable? = null) {
    val line = buildString {
      append(LocalDateTime.now().format(DebugTimeFormatter))
      append(' ')
      append(message)
      throwable?.let {
        append(" | ")
        append(it.message ?: it::class.java.simpleName)
      }
    }
    if (throwable == null) {
      Log.d(DebugTag, line)
    } else {
      Log.e(DebugTag, line, throwable)
    }
    _state.update { current ->
      current.copy(debugLogs = (current.debugLogs + line).takeLast(MaxDebugLogs))
    }
  }

  private fun modeLabel(mode: RenameMode): String {
    return when (mode) {
      is RenameMode.RecSticker -> "点读贴 REC(startCode=${mode.startCode}, digits=${mode.digits}, sort=${mode.sort})"
      is RenameMode.IndexPrefix -> "顺序编号(startIndex=${mode.startIndex}, width=${mode.width}, keepOriginal=${mode.keepOriginal}, sort=${mode.sort})"
      is RenameMode.RegexReplace -> "正则替换(pattern=${mode.pattern}, replacement=${mode.replacement})"
    }
  }

  private fun buildDebugReport(snapshot: MainUiState = state.value): String {
    return buildString {
      appendLine("毛毛虫资源管家 Debug 报告")
      appendLine("生成时间: ${LocalDateTime.now().format(DebugTimeFormatter)}")
      appendLine("当前目录: ${snapshot.folderName ?: "未选择"}")
      appendLine("文件数: ${snapshot.files.size}")
      appendLine("目标目录: ${snapshot.exportRootName ?: "未选择"}")
      appendLine("规则: ${modeLabel(snapshot.mode)}")
      appendLine("改名预览数: ${snapshot.preview.size}")
      appendLine("导出预览数: ${snapshot.exportPreview.size}")
      appendLine("覆盖同名: ${snapshot.exportOverwrite}")
      appendLine("最近消息: ${snapshot.lastMessage ?: "无"}")
      appendLine()
      appendLine("==== Debug Logs ====")
      if (snapshot.debugLogs.isEmpty()) {
        appendLine("<empty>")
      } else {
        snapshot.debugLogs.forEach { appendLine(it) }
      }
    }
  }

  fun clearDebugLogs() {
    _state.update { it.copy(debugLogs = emptyList(), lastMessage = "调试日志已清空。") }
    appendDebug("调试日志已清空")
  }

  private fun prefs() = getApplication<Application>().getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

  private fun sanitizeSubdirForKey(subdir: String): String {
    val trimmed = subdir.trim().ifBlank { "DEFAULT" }
    val sanitized = buildString {
      for (c in trimmed.uppercase()) {
        append(
          when {
            c.isLetterOrDigit() -> c
            c == '.' || c == '_' || c == '-' -> c
            else -> '_'
          },
        )
      }
    }
    return sanitized.take(40).ifBlank { "DEFAULT" }
  }

  private fun prefSequenceNextKey(subdir: String) = "sequence_next.${sanitizeSubdirForKey(subdir)}"

  private fun prefSequenceWidthKey(subdir: String) = "sequence_width.${sanitizeSubdirForKey(subdir)}"

  private fun prefSequencePrefixKey(subdir: String) = "sequence_prefix.${sanitizeSubdirForKey(subdir)}"

  private fun defaultPrefixForSubdir(subdir: String): String {
    return if (subdir.trim().equals("REC", ignoreCase = true)) "REC" else ""
  }

  private fun persistSettings(snapshot: MainUiState = state.value) {
    val subdir = snapshot.targetSubdir
    prefs().edit()
      .putString(PrefFolderUri, snapshot.folderUri?.toString())
      .putString(PrefExportRootUri, snapshot.exportRootUri?.toString())
      .putString(PrefTargetSubdir, snapshot.targetSubdir)
      .putInt(PrefSequenceNext, snapshot.sequenceNext)
      .putInt(PrefSequenceWidth, snapshot.sequenceWidth)
      .putString(PrefSequencePrefix, snapshot.sequencePrefix)
      .putInt(prefSequenceNextKey(subdir), snapshot.sequenceNext)
      .putInt(prefSequenceWidthKey(subdir), snapshot.sequenceWidth)
      .putString(prefSequencePrefixKey(subdir), snapshot.sequencePrefix)
      .putBoolean(PrefAvoidDup, snapshot.avoidDuplicateSequence)
      .putBoolean(PrefOverwrite, snapshot.exportOverwrite)
      .apply()
  }

  private fun restoreSettings() {
    val p = prefs()
    val avoidDup = p.getBoolean(PrefAvoidDup, true)
    val overwrite = p.getBoolean(PrefOverwrite, false)
    val targetSubdir = p.getString(PrefTargetSubdir, "MP3") ?: "MP3"

    val seqNextFallback = p.getInt(PrefSequenceNext, 1).coerceAtLeast(1)
    val seqWidthFallback = p.getInt(PrefSequenceWidth, 4).coerceIn(1, 8)
    val seqNext = p.getInt(prefSequenceNextKey(targetSubdir), seqNextFallback).coerceAtLeast(1)
    val seqWidth = p.getInt(prefSequenceWidthKey(targetSubdir), seqWidthFallback).coerceIn(1, 8)
    val prefixFallback = p.getString(PrefSequencePrefix, defaultPrefixForSubdir(targetSubdir)) ?: defaultPrefixForSubdir(targetSubdir)
    val seqPrefix = p.getString(prefSequencePrefixKey(targetSubdir), prefixFallback) ?: prefixFallback

    val folderUri = p.getString(PrefFolderUri, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    val exportRootUri = p.getString(PrefExportRootUri, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    _state.update {
      it.copy(
        sequenceNext = seqNext,
        sequenceWidth = seqWidth,
        sequencePrefix = seqPrefix,
        avoidDuplicateSequence = avoidDup,
        exportOverwrite = overwrite,
        targetSubdir = targetSubdir,
      )
    }

    val app = getApplication<Application>()
    val persisted = app.contentResolver.persistedUriPermissions.map { it.uri.toString() }.toSet()

    folderUri?.let { uri ->
      if (uri.toString() in persisted) {
        appendDebug("恢复源文件夹 URI -> $uri")
        onFolderPicked(uri)
      } else {
        appendDebug("源文件夹权限已失效，需要重新授权 -> $uri")
        _state.update { it.copy(lastMessage = "源文件夹授权已失效，请重新选择源文件夹。") }
      }
    }
    exportRootUri?.let { uri ->
      if (uri.toString() in persisted) {
        appendDebug("恢复目标目录 URI -> $uri")
        onExportRootPicked(uri)
      } else {
        appendDebug("目标目录权限已失效，需要重新授权 -> $uri")
        _state.update { it.copy(lastMessage = "目标目录授权已失效，请重新选择目标目录。") }
      }
    }
  }

  fun exportDebugLog(targetUri: Uri) {
    viewModelScope.launch {
      appendDebug("开始导出调试日志 -> $targetUri")
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val report = buildDebugReport()
      val error = withContext(Dispatchers.IO) {
        runCatching {
          val app = getApplication<Application>()
          app.contentResolver.openOutputStream(targetUri, "w").use { output ->
            requireNotNull(output) { "无法打开输出流" }
            output.write(report.toByteArray(Charsets.UTF_8))
          }
        }.exceptionOrNull()
      }
      if (error == null) {
        appendDebug("调试日志导出成功 -> $targetUri")
        _state.update { it.copy(isBusy = false, lastMessage = "调试日志已导出。") }
      } else {
        appendDebug("调试日志导出失败 -> $targetUri", error)
        _state.update { it.copy(isBusy = false, lastMessage = "调试日志导出失败：${error.message ?: error::class.java.simpleName}") }
      }
    }
  }

  fun setMode(mode: RenameMode) {
    appendDebug("切换规则 -> ${modeLabel(mode)}")
    _state.update { it.copy(mode = mode, preview = emptyList(), lastMessage = null) }
    persistSettings()
  }

  fun onFolderPicked(treeUri: Uri) {
    appendDebug("选择源文件夹 -> $treeUri")
    val cr = getApplication<Application>().contentResolver
    try {
      cr.takePersistableUriPermission(
        treeUri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
          android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
      appendDebug("源文件夹权限持久化成功")
    } catch (e: SecurityException) {
      appendDebug("源文件夹权限持久化失败，继续使用会话权限", e)
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
      appendDebug("源文件夹加载完成：name=${folder?.name ?: treeUri} files=${children.size}")
      _state.update {
        it.copy(
          folderUri = treeUri,
          folderName = folder?.name ?: treeUri.toString(),
          files = children,
          selectedFileNames = emptySet(),
          preview = emptyList(),
          isBusy = false,
        )
      }
      persistSettings(state.value)
    }
  }

  fun onExportRootPicked(treeUri: Uri) {
    appendDebug("选择目标目录 -> $treeUri")
    val cr = getApplication<Application>().contentResolver
    try {
      cr.takePersistableUriPermission(
        treeUri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
          android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
      appendDebug("目标目录权限持久化成功")
    } catch (e: SecurityException) {
      appendDebug("目标目录权限持久化失败，继续使用会话权限", e)
    }

    val folder = DocumentFile.fromTreeUri(getApplication(), treeUri)
    appendDebug("目标目录已设置：name=${folder?.name ?: treeUri}")
    _state.update {
      it.copy(
        exportRootUri = treeUri,
        exportRootName = folder?.name ?: treeUri.toString(),
        targetFiles = emptyList(),
        exportPreview = emptyList(),
        copyPreview = emptyList(),
        lastMessage = null,
      )
    }
    persistSettings(state.value)
  }

  fun setCollectionName(name: String) {
    _state.update { it.copy(collectionName = name, exportPreview = emptyList()) }
  }

  fun setBookName(name: String) {
    _state.update { it.copy(bookName = name, exportPreview = emptyList()) }
  }

  fun setTargetSubdir(name: String) {
    val subdir = name.trim()
    val p = prefs()
    val nextFallback = p.getInt(PrefSequenceNext, 1).coerceAtLeast(1)
    val widthFallback = p.getInt(PrefSequenceWidth, 4).coerceIn(1, 8)
    val next = p.getInt(prefSequenceNextKey(subdir), nextFallback).coerceAtLeast(1)
    val width = p.getInt(prefSequenceWidthKey(subdir), widthFallback).coerceIn(1, 8)
    val prefixFallback = defaultPrefixForSubdir(subdir)
    val prefix = if (p.contains(prefSequencePrefixKey(subdir))) {
      p.getString(prefSequencePrefixKey(subdir), prefixFallback) ?: prefixFallback
    } else {
      prefixFallback
    }

    _state.update {
      it.copy(
        targetSubdir = subdir,
        sequenceNext = next,
        sequenceWidth = width,
        sequencePrefix = prefix,
        exportPreview = emptyList(),
        copyPreview = emptyList(),
        targetFiles = emptyList(),
        copyOverrideStartIndex = "",
        downloadPreview = emptyList(),
        plannedCopyNextAfter = null,
        plannedDownloadNextAfter = null,
        lastMessage = null,
      )
    }
    persistSettings(state.value)
    // 切换子目录后自动刷新目标文件列表
    refreshTargetFiles()
  }

  fun setExportOverwrite(overwrite: Boolean) {
    _state.update { it.copy(exportOverwrite = overwrite) }
    persistSettings()
  }

  fun setSequenceNext(next: Int) {
    _state.update { it.copy(sequenceNext = next.coerceAtLeast(1)) }
    persistSettings()
  }

  fun setSequenceWidth(width: Int) {
    _state.update { it.copy(sequenceWidth = width.coerceIn(1, 8)) }
    persistSettings()
  }

  fun setSequencePrefix(prefix: String) {
    _state.update { it.copy(sequencePrefix = prefix.trim()) }
    persistSettings()
  }

  fun setAvoidDuplicateSequence(enabled: Boolean) {
    _state.update { it.copy(avoidDuplicateSequence = enabled) }
    persistSettings()
  }

  fun setDownloadUrlsText(text: String) {
    _state.update { it.copy(downloadUrlsText = text, downloadPreview = emptyList(), plannedDownloadNextAfter = null) }
  }

  fun toggleSelectedFile(name: String) {
    _state.update { current ->
      val set = current.selectedFileNames.toMutableSet()
      if (name in set) set.remove(name) else set.add(name)
      current.copy(selectedFileNames = set, preview = emptyList(), copyPreview = emptyList())
    }
  }

  fun selectAllFiles() {
    _state.update { current ->
      val all = current.files.mapNotNull { it.name }.toSet()
      current.copy(selectedFileNames = all, preview = emptyList(), copyPreview = emptyList())
    }
  }

  fun clearSelection() {
    _state.update { it.copy(selectedFileNames = emptySet(), preview = emptyList(), copyPreview = emptyList()) }
  }

  fun refreshTargetFiles() {
    val exportRootUri = state.value.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val subdir = state.value.targetSubdir.trim()
    if (subdir.isBlank()) {
      _state.update { it.copy(lastMessage = "请先选择目标子目录。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      val (files, errorMsg) = withContext(Dispatchers.IO) {
        if (destRoot == null) return@withContext emptyList<DocumentFile>() to "无法读取目标目录，请重新选择。"
        val (destDir, dirError) = SafExporter.ensureDirectoryWithError(destRoot, listOf(subdir))
        if (destDir == null) return@withContext emptyList<DocumentFile>() to (dirError?.message() ?: "无法访问目标子目录：$subdir")
        destDir.listFiles().filter { it.isFile }.toList() to null
      }
      _state.update { it.copy(targetFiles = files, isBusy = false, lastMessage = errorMsg ?: "目标目录文件数：${files.size}") }
    }
  }

  fun scanTargetAndJumpToNextIndex() {
    val snapshot = state.value
    val exportRootUri = snapshot.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val subdir = snapshot.targetSubdir.trim()
    if (subdir.isBlank()) {
      _state.update { it.copy(lastMessage = "请先选择目标子目录（例如 MP3 / DIYAUDIO / BOOK）。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      val result = withContext(Dispatchers.IO) {
        if (destRoot == null) return@withContext null to "无法读取目标目录。"
        val (destDir, scanDirError) = SafExporter.ensureDirectoryWithError(destRoot, listOf(subdir))
        if (destDir == null) return@withContext null to (scanDirError?.message() ?: "无法创建目标子目录：$subdir")
        val used = listUsedIndices(destDir, snapshot.sequencePrefix)
        val next = ((used.maxOrNull() ?: 0) + 1).coerceAtLeast(1)
        next to "已扫描目标子目录：占用 ${used.size} 个序号，已更新为 $next"
      }

      val next = result.first
      val message = result.second
      if (next != null) {
        appendDebug("扫描目标目录序号完成：subdir=$subdir next=$next")
        _state.update { it.copy(sequenceNext = next, isBusy = false, lastMessage = message) }
        persistSettings(state.value)
      } else {
        appendDebug("扫描目标目录序号失败：subdir=$subdir message=$message")
        _state.update { it.copy(isBusy = false, lastMessage = message) }
      }
    }
  }

  fun buildPreview() {
    val snapshot = state.value
    val allNames = snapshot.files.mapNotNull { it.name }
    val names = if (snapshot.selectedFileNames.isNotEmpty()) {
      allNames.filter { it in snapshot.selectedFileNames }
    } else {
      allNames
    }
    if (names.isEmpty()) {
      appendDebug("生成改名预览失败：当前目录没有可处理文件")
      _state.update { it.copy(lastMessage = "当前目录没有可处理的文件。") }
      return
    }

    appendDebug("开始生成改名预览：files=${names.size} mode=${modeLabel(state.value.mode)}")
    val preview = RenamePlanner.plan(names, state.value.mode)
    val conflictCount = preview.count { it.conflict != null }
    appendDebug("改名预览完成：total=${preview.size} conflicts=$conflictCount")
    _state.update { it.copy(preview = preview, lastMessage = "预览已生成：${preview.size} 项") }
  }

  private fun splitExtWithDot(name: String): String {
    val dot = name.lastIndexOf('.')
    if (dot <= 0 || dot == name.length - 1) return ""
    return name.substring(dot)
  }

  private fun buildNumberName(index: Int, width: Int, prefix: String, extWithDot: String): String {
    val num = index.toString().padStart(width, '0')
    return prefix.trim() + num + extWithDot
  }

  // Cache for used indices to avoid repeated filesystem scans
  private var usedIndicesCache: MutableMap<String, Pair<Set<Int>, Long>> = mutableMapOf()
  private val cacheTimeoutMs = 5000L // 5 seconds

  private fun listUsedIndices(destDir: DocumentFile?, seqPrefix: String): Set<Int> {
    if (destDir == null) return emptySet()

    val cacheKey = "${destDir.uri}|$seqPrefix"
    val now = System.currentTimeMillis()

    // Check cache first
    usedIndicesCache[cacheKey]?.let { (cachedIndices, timestamp) ->
      if (now - timestamp < cacheTimeoutMs) {
        return cachedIndices
      }
    }

    // Perform filesystem scan
    val usedIndices = destDir.listFiles()
      .filter { it.isFile }
      .mapNotNull { SequenceAllocator.parseIndexWithPrefix(it.name, seqPrefix) }
      .toSet()

    // Update cache
    usedIndicesCache[cacheKey] = usedIndices to now

    return usedIndices
  }

  fun buildCopyPreview() {
    val snapshot = state.value
    val folderUri = snapshot.folderUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = snapshot.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val subdir = snapshot.targetSubdir.trim()
    if (subdir.isBlank()) {
      _state.update { it.copy(lastMessage = "请先选择目标子目录（例如 MP3 / DIYAUDIO / BOOK）。") }
      return
    }
    if (snapshot.selectedFileNames.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先在“文件”页选择要复制的文件。") }
      return
    }

    val overrideStart = snapshot.copyOverrideStartIndex.trim().toIntOrNull()?.coerceAtLeast(1)
    val startIndex = overrideStart ?: snapshot.sequenceNext

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null, copyPreview = emptyList(), plannedCopyNextAfter = null) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)

      val result = withContext(Dispatchers.IO) {
        val sourceNames = sourceFolder?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name } ?: emptyList()
        val selected = sourceNames.filter { it in snapshot.selectedFileNames }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        val (destDir, dirError) = if (destRoot != null)
          SafExporter.ensureDirectoryWithError(destRoot, listOf(subdir))
        else null to null
        val dirErrorMsg = when {
          destRoot == null -> "请先选择目标目录。"
          dirError != null -> dirError.message()
          else -> null
        }
        val existing = if (snapshot.exportOverwrite) emptySet() else destDir?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name }?.toSet() ?: emptySet()
        val used = listUsedIndices(destDir, snapshot.sequencePrefix)
        val seqPlan = SequenceAllocator.plan(
          start = startIndex,
          count = selected.size,
          used = used,
          avoidDuplicates = snapshot.avoidDuplicateSequence,
        )

        val items = selected.mapIndexed { i, oldName ->
          val ext = splitExtWithDot(oldName)
          val index = seqPlan.allocated[i]
          val newName = buildNumberName(index, snapshot.sequenceWidth, snapshot.sequencePrefix, ext)
          val invalid = if (newName.contains('/') || newName.contains('\\')) "文件名包含非法字符" else null
          val conflict = dirErrorMsg ?: invalid ?: if (!snapshot.exportOverwrite && newName in existing) "目标已存在" else null
          ExportPreviewItem(
            oldName = oldName,
            newName = newName,
            destRelativePath = subdir,
            conflict = conflict,
          )
        }
        items to seqPlan.nextAfter
      }

      val items = result.first
      val nextAfter = result.second
      _state.update {
        it.copy(
          copyPreview = items,
          plannedCopyNextAfter = nextAfter,
          isBusy = false,
          lastMessage = "复制预览已生成：${items.size} 项",
        )
      }
    }
  }

  fun applyCopy() {
    val snapshot = state.value
    val folderUri = snapshot.folderUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = snapshot.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val preview = snapshot.copyPreview
    if (preview.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先生成复制预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      appendDebug("开始执行复制：total=${preview.size} overwrite=${snapshot.exportOverwrite}")

      val resultMessage = withContext(Dispatchers.IO) {
        try {
          if (sourceFolder == null || destRoot == null) return@withContext "无法读取源/目标目录。"
          val plan = preview.filter { it.conflict == null }.map { it.oldName to it.newName }
          val r = SafExporter.export(
            context = app,
            sourceFolder = sourceFolder,
            destRoot = destRoot,
            collectionName = "",
            bookName = "",
            targetSubdir = snapshot.targetSubdir,
            plan = plan,
            move = false,
            overwrite = snapshot.exportOverwrite,
            logger = ::appendDebug,
          )
          "复制完成：成功 ${r.ok}，跳过 ${r.skipped}，失败 ${r.failed}"
        } catch (t: Throwable) {
          appendDebug("复制异常", t)
          "复制失败：${t.message ?: t::class.java.simpleName}"
        }
      }

      val refreshed = withContext(Dispatchers.IO) {
        sourceFolder?.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
      }

      val nextAfter = snapshot.plannedCopyNextAfter
      _state.update {
        it.copy(
          files = refreshed,
          copyPreview = emptyList(),
          plannedCopyNextAfter = null,
          isBusy = false,
          lastMessage = resultMessage,
          sequenceNext = if (nextAfter != null) maxOf(it.sequenceNext, nextAfter) else it.sequenceNext,
        )
      }
      persistSettings(state.value)
    }
  }

  fun setCopyOverrideStartIndex(text: String) {
    _state.update { it.copy(copyOverrideStartIndex = text, copyPreview = emptyList(), plannedCopyNextAfter = null) }
  }

  private fun normalizeUrlLines(text: String): List<String> {
    return text.lineSequence()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .toList()
  }

  fun buildDownloadPreview() {
    val snapshot = state.value
    val exportRootUri = snapshot.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val subdir = snapshot.targetSubdir.trim()
    if (subdir.isBlank()) {
      _state.update { it.copy(lastMessage = "请先选择目标子目录（例如 MP3 / DIYAUDIO / BOOK）。") }
      return
    }
    val urls = normalizeUrlLines(snapshot.downloadUrlsText)
    if (urls.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先输入 URL（每行一个）。") }
      return
    }

    val overrideStart = snapshot.copyOverrideStartIndex.trim().toIntOrNull()?.coerceAtLeast(1)
    val startIndex = overrideStart ?: snapshot.sequenceNext

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null, downloadPreview = emptyList(), plannedDownloadNextAfter = null) }
      val app = getApplication<Application>()
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)

      val result = withContext(Dispatchers.IO) {
        val (destDir, dirError) = if (destRoot != null)
          SafExporter.ensureDirectoryWithError(destRoot, listOf(subdir))
        else null to null
        val dirErrorMsg = when {
          destRoot == null -> "请先选择目标目录。"
          dirError != null -> dirError.message()
          else -> null
        }
        val existing = if (snapshot.exportOverwrite) emptySet() else destDir?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name }?.toSet() ?: emptySet()
        val used = listUsedIndices(destDir, snapshot.sequencePrefix)
        val seqPlan = SequenceAllocator.plan(
          start = startIndex,
          count = urls.size,
          used = used,
          avoidDuplicates = snapshot.avoidDuplicateSequence,
        )

        val items = urls.mapIndexed { i, url ->
          val ext = runCatching {
            val path = Uri.parse(url).lastPathSegment ?: return@runCatching ""
            val dot = path.lastIndexOf('.')
            if (dot <= 0 || dot == path.length - 1) "" else path.substring(dot)
          }.getOrDefault("")
          val safeExt = if (ext.isBlank()) ".bin" else ext
          val index = seqPlan.allocated[i]
          val destName = buildNumberName(index, snapshot.sequenceWidth, snapshot.sequencePrefix, safeExt)
          val invalid = if (destName.contains('/') || destName.contains('\\')) "文件名包含非法字符" else null
          val conflict = dirErrorMsg ?: invalid ?: if (!snapshot.exportOverwrite && destName in existing) "目标已存在" else null
          DownloadPreviewItem(
            url = url,
            destName = destName,
            destRelativePath = subdir,
            conflict = conflict,
          )
        }
        items to seqPlan.nextAfter
      }

      _state.update {
        it.copy(
          downloadPreview = result.first,
          plannedDownloadNextAfter = result.second,
          isBusy = false,
          lastMessage = "下载预览已生成：${result.first.size} 项",
        )
      }
    }
  }

  private fun mimeFromFileName(name: String): String {
    val dot = name.lastIndexOf('.')
    val ext = if (dot <= 0 || dot == name.length - 1) "" else name.substring(dot + 1)
    val mimeFromExt = if (ext.isBlank()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    return mimeFromExt ?: "application/octet-stream"
  }

  fun applyDownload() {
    val snapshot = state.value
    val exportRootUri = snapshot.exportRootUri ?: run {
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val subdir = snapshot.targetSubdir.trim()
    if (subdir.isBlank()) {
      _state.update { it.copy(lastMessage = "请先选择目标子目录。") }
      return
    }
    val preview = snapshot.downloadPreview
    if (preview.isEmpty()) {
      _state.update { it.copy(lastMessage = "请先生成下载预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      appendDebug("开始执行下载：total=${preview.size} overwrite=${snapshot.exportOverwrite}")

      var dirErrorMsg: String? = null
      val result = withContext(Dispatchers.IO) {
        if (destRoot == null) return@withContext DownloadResult(ok = 0, skipped = 0, failed = preview.size)
        val (destDir, dirError) = SafExporter.ensureDirectoryWithError(destRoot, listOf(subdir))
        if (destDir == null) {
          val reason = dirError?.message() ?: "无法创建目标子目录：$subdir"
          dirErrorMsg = reason
          appendDebug("下载失败：$reason")
          return@withContext DownloadResult(ok = 0, skipped = 0, failed = preview.size)
        }
        val cr = app.contentResolver

        var ok = 0
        var skipped = 0
        var failed = 0

        for (item in preview) {
          if (item.conflict != null) {
            skipped++
            continue
          }
          val existing = destDir.findFile(item.destName)
          if (existing != null && !snapshot.exportOverwrite) {
            skipped++
            continue
          }
          if (existing != null && snapshot.exportOverwrite) {
            existing.delete()
          }

          val created = destDir.createFile(mimeFromFileName(item.destName), item.destName)
          if (created == null) {
            failed++
            continue
          }

          // Update progress state to show current file being downloaded
          _state.update { currentState ->
            currentState.copy(downloadProgress = mapOf(item.url to 50))
          }

          val error = runCatching {
            // Download with retry logic
            var lastError: Exception? = null
            var attempt = 0
            val maxRetries = 3

            while (attempt <= maxRetries) {
              try {
                val conn = (URL(item.url).openConnection() as HttpURLConnection).apply {
                  connectTimeout = 15_000
                  readTimeout = 30_000
                  instanceFollowRedirects = true
                  requestMethod = "GET"
                }
                conn.inputStream.use { input ->
                  cr.openOutputStream(created.uri, "w").use { output ->
                    requireNotNull(output) { "无法打开输出流" }
                    input.copyTo(output)
                  }
                }
                // Success - break out of retry loop
                lastError = null
                break
              } catch (e: Exception) {
                lastError = e
                attempt++
                if (attempt <= maxRetries) {
                  // Exponential backoff: 1s, 2s, 4s
                  delay(1000L * (1L shl (attempt - 1)))
                }
              }
            }

            if (lastError != null) {
              throw lastError
            }
          }.exceptionOrNull()

          // Clear progress after completion
          _state.update { currentState ->
            currentState.copy(downloadProgress = emptyMap())
          }

          if (error == null) {
            ok++
          } else {
            created.delete()
            failed++
            appendDebug("下载失败：${item.url} -> ${item.destName}", error)
          }
        }

        DownloadResult(ok = ok, skipped = skipped, failed = failed)
      }

      val message = dirErrorMsg
        ?: "下载完成：成功 ${result.ok}，跳过 ${result.skipped}，失败 ${result.failed}"
      val nextAfter = if (dirErrorMsg == null) snapshot.plannedDownloadNextAfter else null
      _state.update {
        it.copy(
          downloadPreview = emptyList(),
          plannedDownloadNextAfter = null,
          isBusy = false,
          lastMessage = message,
          sequenceNext = if (nextAfter != null) maxOf(it.sequenceNext, nextAfter) else it.sequenceNext,
        )
      }
      persistSettings(state.value)
    }
  }

  fun buildExportPreview() {
    val folderUri = state.value.folderUri ?: run {
      appendDebug("生成导出预览失败：未选择源文件夹")
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = state.value.exportRootUri ?: run {
      appendDebug("生成导出预览失败：未选择目标目录")
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    if (state.value.collectionName.trim().isBlank() || state.value.bookName.trim().isBlank()) {
      appendDebug("生成导出预览失败：绘本集或绘本名称为空")
      _state.update { it.copy(lastMessage = "请填写绘本集与绘本名称。") }
      return
    }
    if (state.value.targetSubdir.trim().isBlank()) {
      appendDebug("生成导出预览失败：目标子目录为空")
      _state.update { it.copy(lastMessage = "请填写目标子目录（例如 MP3 / DIYAUDIO / BOOK）。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null, exportPreview = emptyList()) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      appendDebug("开始生成导出预览：collection=${state.value.collectionName} book=${state.value.bookName} subdir=${state.value.targetSubdir} overwrite=${state.value.exportOverwrite}")

      val result = withContext(Dispatchers.IO) {
        val names = sourceFolder?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name } ?: emptyList()
        val planned = RenamePlanner.plan(names, state.value.mode)
        val rel = SafExporter.buildDestRelativePath(state.value.collectionName, state.value.bookName, state.value.targetSubdir)
        val (destDir, dirError) = if (destRoot != null)
          SafExporter.ensureDirectoryWithError(destRoot, rel.split('/').filter { it.isNotBlank() })
        else null to null
        val dirErrorMsg = when {
          destRoot == null -> "请先选择目标目录。"
          dirError != null -> dirError.message()
          else -> null
        }
        val existing = if (state.value.exportOverwrite) {
          emptySet()
        } else {
          destDir?.listFiles()?.filter { it.isFile }?.mapNotNull { it.name }?.toSet() ?: emptySet()
        }
        planned.map { p ->
          val invalid = if (p.newName.contains('/') || p.newName.contains('\\')) "文件名包含非法字符" else null
          val conflict = p.conflict ?: dirErrorMsg ?: invalid ?: if (p.newName in existing) "目标已存在" else null
          ExportPreviewItem(
            oldName = p.oldName,
            newName = p.newName,
            destRelativePath = rel,
            conflict = conflict,
          )
        }
      }

      appendDebug("导出预览完成：total=${result.size} conflicts=${result.count { it.conflict != null }}")
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
      appendDebug("执行改名失败：未选择文件夹")
      _state.update { it.copy(lastMessage = "请先选择文件夹。") }
      return
    }
    val preview = state.value.preview
    if (preview.isEmpty()) {
      appendDebug("执行改名失败：预览为空")
      _state.update { it.copy(lastMessage = "请先生成预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val folder = DocumentFile.fromTreeUri(app, folderUri)
      val byName = folder?.listFiles()?.filter { it.isFile }?.associateBy { it.name } ?: emptyMap()
      appendDebug("开始执行改名：items=${preview.size}")

      val (ok, skipped, failed) = withContext(Dispatchers.IO) {
        var okCount = 0
        var skippedCount = 0
        var failedCount = 0
        for (item in preview) {
          if (item.conflict != null) {
            appendDebug("改名跳过：${item.oldName} -> ${item.newName} reason=${item.conflict}")
            skippedCount++
            continue
          }
          val file = byName[item.oldName]
          if (file == null) {
            appendDebug("改名失败：找不到源文件 ${item.oldName}")
            failedCount++
            continue
          }
          val renamed = try {
            file.renameTo(item.newName)
          } catch (t: Throwable) {
            appendDebug("改名异常：${item.oldName} -> ${item.newName}", t)
            false
          }
          if (renamed) {
            appendDebug("改名成功：${item.oldName} -> ${item.newName}")
            okCount++
          } else {
            appendDebug("改名失败：${item.oldName} -> ${item.newName}")
            failedCount++
          }
        }
        Triple(okCount, skippedCount, failedCount)
      }

      val refreshed = withContext(Dispatchers.IO) {
        folder?.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
      }
      appendDebug("改名完成：ok=$ok skipped=$skipped failed=$failed remaining=${refreshed.size}")
      _state.update {
        it.copy(
          files = refreshed,
          selectedFileNames = emptySet(),
          preview = emptyList(),
          isBusy = false,
          lastMessage = "完成：成功 $ok，跳过冲突 $skipped，失败 $failed",
        )
      }
    }
  }

  fun applyExport(move: Boolean) {
    val folderUri = state.value.folderUri ?: run {
      appendDebug("执行导出失败：未选择源文件夹")
      _state.update { it.copy(lastMessage = "请先选择源文件夹。") }
      return
    }
    val exportRootUri = state.value.exportRootUri ?: run {
      appendDebug("执行导出失败：未选择目标目录")
      _state.update { it.copy(lastMessage = "请先选择目标目录。") }
      return
    }
    val preview = state.value.exportPreview
    if (preview.isEmpty()) {
      appendDebug("执行导出失败：导出预览为空")
      _state.update { it.copy(lastMessage = "请先生成导出预览。") }
      return
    }

    viewModelScope.launch {
      _state.update { it.copy(isBusy = true, lastMessage = null) }
      val app = getApplication<Application>()
      val sourceFolder = DocumentFile.fromTreeUri(app, folderUri)
      val destRoot = DocumentFile.fromTreeUri(app, exportRootUri)
      appendDebug("开始执行导出：move=$move total=${preview.size} overwrite=${state.value.exportOverwrite}")

      val resultMessage = withContext(Dispatchers.IO) {
        try {
          if (sourceFolder == null || destRoot == null) return@withContext "无法读取源/目标目录。"
          val plan = preview
            .filter { it.conflict == null }
            .map { it.oldName to it.newName }
          appendDebug("导出有效计划数：${plan.size}")
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
            logger = ::appendDebug,
          )
          "导出完成：成功 ${r.ok}，跳过 ${r.skipped}，失败 ${r.failed}"
        } catch (t: Throwable) {
          appendDebug("导出异常", t)
          "导出失败：${t.message ?: t::class.java.simpleName}"
        }
      }

      val refreshed = withContext(Dispatchers.IO) {
        sourceFolder?.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
      }
      appendDebug("导出流程结束：message=$resultMessage remaining=${refreshed.size}")
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
