package com.maomaochongapp.renamer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RenamePlannerTest {

  @Test
  fun recSticker_generatesCorrectNames() {
    val names = listOf("story1.mp3", "story2.mp3", "story3.mp3")
    val mode = RenameMode.RecSticker(startCode = 1201, prefix = "REC", digits = 4)
    val result = RenamePlanner.plan(names, mode)

    assertEquals(3, result.size)
    assertEquals("REC1201.mp3", result[0].newName)
    assertEquals("REC1202.mp3", result[1].newName)
    assertEquals("REC1203.mp3", result[2].newName)
  }

  @Test
  fun indexPrefix_withKeepOriginal() {
    val names = listOf("hello.mp3", "world.mp3")
    val mode = RenameMode.IndexPrefix(startIndex = 1, width = 4, separator = "_", keepOriginal = true)
    val result = RenamePlanner.plan(names, mode)

    assertEquals(2, result.size)
    assertEquals("0001_hello.mp3", result[0].newName)
    assertEquals("0002_world.mp3", result[1].newName)
  }

  @Test
  fun indexPrefix_withoutKeepOriginal() {
    val names = listOf("hello.mp3", "world.mp3")
    val mode = RenameMode.IndexPrefix(startIndex = 1, width = 4, keepOriginal = false)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("0001.mp3", result[0].newName)
    assertEquals("0002.mp3", result[1].newName)
  }

  @Test
  fun regexReplace_replacesPatternInBaseName() {
    val names = listOf("hello world.mp3", "foo bar.wav")
    val mode = RenameMode.RegexReplace(pattern = " ", replacement = "_")
    val result = RenamePlanner.plan(names, mode)

    assertEquals("hello_world.mp3", result[0].newName)
    assertEquals("foo_bar.wav", result[1].newName)
  }

  @Test
  fun regexReplace_invalidPattern_marksConflict() {
    val names = listOf("test.mp3")
    val mode = RenameMode.RegexReplace(pattern = "[invalid", replacement = "_")
    val result = RenamePlanner.plan(names, mode)

    assertNotNull(result[0].conflict)
  }

  @Test
  fun plan_detectsDuplicateNewNames() {
    val names = listOf("a.mp3", "b.mp3")
    val mode = RenameMode.RegexReplace(pattern = ".+", replacement = "same")
    val result = RenamePlanner.plan(names, mode)

    assertEquals("same.mp3", result[0].newName)
    assertEquals("same.mp3", result[1].newName)
    assertNotNull(result[0].conflict)
    assertNotNull(result[1].conflict)
  }

  @Test
  fun plan_detectsUnchangedNames() {
    val names = listOf("nochange.mp3")
    val mode = RenameMode.RegexReplace(pattern = "xyz", replacement = "abc")
    val result = RenamePlanner.plan(names, mode)

    assertEquals("nochange.mp3", result[0].newName)
    assertNotNull(result[0].conflict)
  }

  @Test
  fun recSticker_sortsByNameDesc() {
    val names = listOf("a.mp3", "c.mp3", "b.mp3")
    val mode = RenameMode.RecSticker(startCode = 1, digits = 4, sort = Sort.ByNameDesc)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("c.mp3", result[0].oldName)
    assertEquals("b.mp3", result[1].oldName)
    assertEquals("a.mp3", result[2].oldName)
  }

  @Test
  fun indexPrefix_startsFromCustomIndex() {
    val names = listOf("x.mp3")
    val mode = RenameMode.IndexPrefix(startIndex = 100, width = 3, keepOriginal = false)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("100.mp3", result[0].newName)
    assertNull(result[0].conflict)
  }

  @Test
  fun plan_handlesFileWithNoExtension() {
    val names = listOf("README")
    val mode = RenameMode.IndexPrefix(startIndex = 1, width = 4, keepOriginal = true)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("0001_README", result[0].newName)
  }

  @Test
  fun regexReplace_blankResult_marksConflict() {
    val names = listOf("README")
    val mode = RenameMode.RegexReplace(pattern = ".*", replacement = "")
    val result = RenamePlanner.plan(names, mode)

    assertEquals("", result[0].newName)
    assertEquals("新名称为空", result[0].conflict)
  }

  @Test
  fun regexReplace_invalidReplacement_marksUnchangedConflict() {
    val names = listOf("a.mp3")
    val mode = RenameMode.RegexReplace(pattern = "(a)", replacement = "$2")
    val result = RenamePlanner.plan(names, mode)

    assertEquals("a.mp3", result[0].newName)
    assertEquals("名称未变化", result[0].conflict)
  }

  @Test
  fun indexPrefix_handlesTrailingDotName() {
    val names = listOf("draft.")
    val mode = RenameMode.IndexPrefix(startIndex = 1, width = 4, keepOriginal = true)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("0001_draft.", result[0].newName)
    assertNull(result[0].conflict)
  }

  @Test
  fun recSticker_sortsByNameAsc_caseInsensitive() {
    val names = listOf("b.mp3", "A.mp3", "c.mp3")
    val mode = RenameMode.RecSticker(startCode = 1, digits = 4, sort = Sort.ByNameAsc)
    val result = RenamePlanner.plan(names, mode)

    assertEquals("A.mp3", result[0].oldName)
    assertEquals("b.mp3", result[1].oldName)
    assertEquals("c.mp3", result[2].oldName)
  }
}
