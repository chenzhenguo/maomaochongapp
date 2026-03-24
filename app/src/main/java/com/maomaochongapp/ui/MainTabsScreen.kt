package com.maomaochongapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maomaochongapp.MainViewModel
import com.maomaochongapp.ui.tabs.CopyTab
import com.maomaochongapp.ui.tabs.DownloadTab
import com.maomaochongapp.ui.tabs.FilesTab
import com.maomaochongapp.ui.tabs.SettingsTab

private enum class AppTab(val title: String) {
  Settings("设置"),
  Files("文件"),
  Copy("复制"),
  Download("下载"),
}

@Composable
fun MainTabsScreen(viewModel: MainViewModel) {
  val state by viewModel.state.collectAsState()
  val context = LocalContext.current

  val pickSourceLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
    onResult = { uri: Uri? -> if (uri != null) viewModel.onFolderPicked(uri) },
  )
  val pickTargetLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
    onResult = { uri: Uri? -> if (uri != null) viewModel.onExportRootPicked(uri) },
  )
  val exportDebugLogLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/plain"),
    onResult = { uri: Uri? -> if (uri != null) viewModel.exportDebugLog(uri) },
  )

  var tabIndex by rememberSaveable { mutableIntStateOf(0) }
  val tabs = remember { AppTab.entries.toList() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("毛毛虫资源管家") },
        actions = { if (state.isBusy) CircularProgressIndicator() },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding),
    ) {
      TabRow(selectedTabIndex = tabIndex) {
        tabs.forEachIndexed { idx, tab ->
          Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(tab.title) })
        }
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (tabs[tabIndex]) {
          AppTab.Settings -> SettingsTab(
            state = state,
            enabled = !state.isBusy,
            onPickSource = { pickSourceLauncher.launch(null) },
            onPickTarget = { pickTargetLauncher.launch(null) },
            onTargetSubdirChange = viewModel::setTargetSubdir,
            onOverwriteChange = viewModel::setExportOverwrite,
            onSequenceNextChange = viewModel::setSequenceNext,
            onSequenceWidthChange = viewModel::setSequenceWidth,
            onSequencePrefixChange = viewModel::setSequencePrefix,
            onAvoidDupChange = viewModel::setAvoidDuplicateSequence,
            onScanNextIndex = viewModel::scanTargetAndJumpToNextIndex,
            onExportDebug = { exportDebugLogLauncher.launch("maomaochong-debug-${System.currentTimeMillis()}.txt") },
            onClearDebug = viewModel::clearDebugLogs,
          )

          AppTab.Files -> FilesTab(
            state = state,
            enabled = !state.isBusy,
            onToggleSelect = viewModel::toggleSelectedFile,
            onSelectAll = viewModel::selectAllFiles,
            onClearSelection = viewModel::clearSelection,
            onBuildRenamePreview = viewModel::buildPreview,
            onApplyRename = viewModel::applyRename,
            onModeChange = viewModel::setMode,
            onRefreshTarget = viewModel::refreshTargetFiles,
            onOpenFile = { file ->
              runCatching {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                  setDataAndType(file.uri, context.contentResolver.getType(file.uri))
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
              }
            },
          )

          AppTab.Copy -> CopyTab(
            state = state,
            enabled = !state.isBusy,
            onOverrideStartIndexChange = viewModel::setCopyOverrideStartIndex,
            onBuildPreview = viewModel::buildCopyPreview,
            onApplyCopy = viewModel::applyCopy,
          )

          AppTab.Download -> DownloadTab(
            state = state,
            enabled = !state.isBusy,
            onUrlsTextChange = viewModel::setDownloadUrlsText,
            onOverrideStartIndexChange = viewModel::setCopyOverrideStartIndex,
            onBuildPreview = viewModel::buildDownloadPreview,
            onApplyDownload = viewModel::applyDownload,
          )
        }
      }
    }
  }
}
