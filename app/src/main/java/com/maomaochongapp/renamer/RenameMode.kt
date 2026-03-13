package com.maomaochongapp.renamer

sealed interface RenameMode {
  data class RecSticker(
    val startCode: Int,
    val prefix: String = "REC",
    val digits: Int = 4,
    val sort: Sort = Sort.ByNameAsc,
  ) : RenameMode

  data class IndexPrefix(
    val startIndex: Int = 1,
    val width: Int = 4,
    val separator: String = "_",
    val keepOriginal: Boolean = false,
    val sort: Sort = Sort.ByNameAsc,
  ) : RenameMode

  data class RegexReplace(
    val pattern: String,
    val replacement: String,
  ) : RenameMode
}

enum class Sort {
  ByNameAsc,
  ByNameDesc,
}
