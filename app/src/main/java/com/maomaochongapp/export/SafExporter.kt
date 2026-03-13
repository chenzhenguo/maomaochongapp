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
    contentResolver.openInputStream(source.uri).use { input ->
      if (input == null) return false
      contentResolver.openOutputStream(created.uri, "w").use { output ->
        if (output == null) return false
        input.copyTo(output)
      }
    }
    return true
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
  ): ExportResult {
    val rel = buildDestRelativePath(collectionName, bookName, targetSubdir)
    val segments = rel.split('/').filter { it.isNotBlank() }
    val destDir = ensureDirectory(destRoot, segments) ?: throw IOException("无法创建目标目录：$rel")

    val byName = sourceFolder.listFiles().filter { it.isFile }.associateBy { it.name }
    val cr = context.contentResolver

    var ok = 0
    var skipped = 0
    var failed = 0

    for ((oldName, newName) in plan) {
      val src = byName[oldName]
      if (src == null) {
        failed++
        continue
      }
      val copied = try {
        copyFile(cr, src, destDir, newName, overwrite)
      } catch (_: Throwable) {
        false
      }
      if (!copied) {
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
          failed++
        } else {
          ok++
        }
      } else {
        ok++
      }
    }
    return ExportResult(ok = ok, skipped = skipped, failed = failed)
  }
}
