package com.maomaochongapp.renamer

data class RenamePreviewItem(
  val oldName: String,
  val newName: String,
  val conflict: String? = null,
)

object RenamePlanner {
  fun plan(originalNames: List<String>, mode: RenameMode): List<RenamePreviewItem> {
    val sorted = when (mode) {
      is RenameMode.RecSticker -> sortNames(originalNames, mode.sort)
      is RenameMode.IndexPrefix -> sortNames(originalNames, mode.sort)
      is RenameMode.RegexReplace -> originalNames
    }

    val proposed = when (mode) {
      is RenameMode.RecSticker -> planRec(sorted, mode)
      is RenameMode.IndexPrefix -> planIndex(sorted, mode)
      is RenameMode.RegexReplace -> planRegex(sorted, mode)
    }

    return markConflicts(proposed)
  }

  private fun planRec(names: List<String>, mode: RenameMode.RecSticker): List<RenamePreviewItem> {
    return names.mapIndexed { i, oldName ->
      val (_, extWithDot) = splitName(oldName)
      val code = mode.startCode + i
      val padded = code.toString().padStart(mode.digits, '0')
      val newName = mode.prefix + padded + extWithDot
      RenamePreviewItem(oldName = oldName, newName = newName)
    }
  }

  private fun planIndex(names: List<String>, mode: RenameMode.IndexPrefix): List<RenamePreviewItem> {
    return names.mapIndexed { i, oldName ->
      val (base, extWithDot) = splitName(oldName)
      val index = mode.startIndex + i
      val prefix = index.toString().padStart(mode.width, '0')
      val newName = if (mode.keepOriginal) {
        prefix + mode.separator + base + extWithDot
      } else {
        prefix + extWithDot
      }
      RenamePreviewItem(oldName = oldName, newName = newName)
    }
  }

  private fun planRegex(names: List<String>, mode: RenameMode.RegexReplace): List<RenamePreviewItem> {
    val regex = try {
      mode.pattern.toRegex()
    } catch (_: Throwable) {
      return names.map { RenamePreviewItem(oldName = it, newName = it, conflict = "正则表达式无效") }
    }
    return names.map { oldName ->
      val (base, extWithDot) = splitName(oldName)
      val newBase = try {
        base.replace(regex, mode.replacement)
      } catch (_: Throwable) {
        base
      }
      RenamePreviewItem(oldName = oldName, newName = newBase + extWithDot)
    }
  }

  private fun sortNames(names: List<String>, sort: Sort): List<String> {
    return when (sort) {
      Sort.ByNameAsc -> names.sortedWith(String.CASE_INSENSITIVE_ORDER)
      Sort.ByNameDesc -> names.sortedWith(String.CASE_INSENSITIVE_ORDER).asReversed()
    }
  }

  private fun splitName(name: String): Pair<String, String> {
    val dot = name.lastIndexOf('.')
    if (dot <= 0 || dot == name.length - 1) return name to ""
    return name.substring(0, dot) to name.substring(dot)
  }

  private fun markConflicts(items: List<RenamePreviewItem>): List<RenamePreviewItem> {
    val duplicates = items.groupBy { it.newName }.filterValues { it.size > 1 }.keys
    return items.map { item ->
      val conflict = when {
        item.conflict != null -> item.conflict
        item.newName == item.oldName -> "名称未变化"
        item.newName.isBlank() -> "新名称为空"
        item.newName in duplicates -> "新名称重复"
        else -> null
      }
      item.copy(conflict = conflict)
    }
  }
}
