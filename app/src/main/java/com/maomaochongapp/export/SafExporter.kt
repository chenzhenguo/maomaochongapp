package com.maomaochongapp.export

import android.content.ContentResolver
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.net.URLConnection

data class ExportPreviewItem(
  val oldName: String,
  val newName: String,
  val destRelativePath: String,
  val conflict: String? = null,
)

data class ExportResult(
  val ok: Int,
  val skipped: Int,
  val failed: Int,
)

sealed class EnsureDirectoryError {
  data object RootNotDirectory : EnsureDirectoryError()
  data class SegmentIsFile(val segment: String) : EnsureDirectoryError()
  data class CreateFailed(val segment: String) : EnsureDirectoryError()

  fun message(): String = when (this) {
    is RootNotDirectory -> "目标根目录不可用（不是目录）"
    is SegmentIsFile -> "路径中存在同名文件，无法创建子目录：$segment"
    is CreateFailed -> "无法创建子目录：$segment（可能是权限不足或磁盘已满）"
  }
}

object SafExporter {
  fun buildDestRelativePath(collection: String, book: String, targetSubdir: String): String {
    val c = collection.trim().trim('/').trim()
    val b = book.trim().trim('/').trim()
    val t = targetSubdir.trim().trim('/').trim()
    return listOf(c, b, t).filter { it.isNotBlank() }.joinToString("/")
  }

  fun guessMimeType(fileName: String): String {
    return URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
  }

  fun ensureDirectory(root: DocumentFile, segments: List<String>): DocumentFile? {
    if (!root.isDirectory) return null
    var current: DocumentFile = root
    for (seg in segments) {
      val name = seg.trim().trim('/').trim()
      if (name.isBlank()) continue
      val existing = current.findFile(name)
      current = when {
        existing == null -> current.createDirectory(name) ?: return null
        existing.isDirectory -> existing
        else -> return null
      }
    }
    return current
  }

  fun ensureDirectoryWithError(root: DocumentFile, segments: List<String>): Pair<DocumentFile?, EnsureDirectoryError?> {
    if (!root.isDirectory) return null to EnsureDirectoryError.RootNotDirectory
    var current: DocumentFile = root
    for (seg in segments) {
      val name = seg.trim().trim('/').trim()
      if (name.isBlank()) continue
      val existing = current.findFile(name)
      current = when {
        existing == null -> {
          val created = current.createDirectory(name)
            ?: return null to EnsureDirectoryError.CreateFailed(name)
          created
        }
        existing.isDirectory -> existing
        else -> return null to EnsureDirectoryError.SegmentIsFile(name)
      }
    }
    return current to null
  }

  fun copyFile(
    contentResolver: ContentResolver,
    source: DocumentFile,
    destDir: DocumentFile,
    destName: String,
    overwrite: Boolean,
  ): Boolean {
    val existing = destDir.findFile(destName)
    if (existing != null) {
      if (!overwrite) return false
      if (!existing.delete()) return false
    }

    val created = destDir.createFile(guessMimeType(destName), destName) ?: return false
    return try {
      contentResolver.openInputStream(source.uri).use { input ->
        if (input == null) {
          created.delete()
          return false
        }
        contentResolver.openOutputStream(created.uri, "w").use { output ->
          if (output == null) {
            created.delete()
            return false
          }
          input.copyTo(output)
        }
      }
      true
    } catch (t: Throwable) {
      created.delete()
      throw t
    }
  }

  @Throws(IOException::class)
  fun export(
    context: Context,
    sourceFolder: DocumentFile,
    destRoot: DocumentFile,
    collectionName: String,
    bookName: String,
    targetSubdir: String,
    plan: List<Pair<String, String>>, // (oldName -> newName)
    move: Boolean,
    overwrite: Boolean,
    logger: ((String) -> Unit)? = null,
  ): ExportResult {
    val rel = buildDestRelativePath(collectionName, bookName, targetSubdir)
    logger?.invoke("SafExporter 开始处理：dest=$rel items=${plan.size} move=$move overwrite=$overwrite")
    val segments = rel.split('/').filter { it.isNotBlank() }
    val (destDir, dirError) = ensureDirectoryWithError(destRoot, segments)
    if (destDir == null) {
      val reason = dirError?.message() ?: "无法创建目标目录：$rel"
      throw IOException(reason)
    }

    val byName = sourceFolder.listFiles().filter { it.isFile }.associateBy { it.name }
    val cr = context.contentResolver

    var ok = 0
    var skipped = 0
    var failed = 0

    for ((oldName, newName) in plan) {
      val src = byName[oldName]
      if (src == null) {
        logger?.invoke("导出失败：找不到源文件 $oldName")
        failed++
        continue
      }
      val copied = try {
        copyFile(cr, src, destDir, newName, overwrite)
      } catch (t: Throwable) {
        logger?.invoke("导出复制异常：$oldName -> $newName | ${t.message ?: t::class.java.simpleName}")
        false
      }
      if (!copied) {
        logger?.invoke("导出跳过：$oldName -> $newName")
        skipped++
        continue
      }
      if (move) {
        val deleted = try {
          src.delete()
        } catch (_: Throwable) {
          false
        }
        if (!deleted) {
          logger?.invoke("导出复制成功但删除源文件失败：$oldName -> $newName")
          failed++
        } else {
          logger?.invoke("导出移动成功：$oldName -> $newName")
          ok++
        }
      } else {
        logger?.invoke("导出复制成功：$oldName -> $newName")
        ok++
      }
    }
    logger?.invoke("SafExporter 完成：ok=$ok skipped=$skipped failed=$failed")
    return ExportResult(ok = ok, skipped = skipped, failed = failed)
  }
}
