package com.maomaochongapp.download

data class DownloadPreviewItem(
  val url: String,
  val destName: String,
  val destRelativePath: String,
  val conflict: String? = null,
)

data class DownloadResult(
  val ok: Int,
  val skipped: Int,
  val failed: Int,
)

