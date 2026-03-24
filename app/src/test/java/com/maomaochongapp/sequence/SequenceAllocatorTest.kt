package com.maomaochongapp.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SequenceAllocatorTest {
  @Test
  fun parseLeadingInt_handlesNullAndBlank() {
    assertNull(SequenceAllocator.parseLeadingInt(null))
    assertNull(SequenceAllocator.parseLeadingInt(""))
    assertNull(SequenceAllocator.parseLeadingInt("   "))
  }

  @Test
  fun parseLeadingInt_parsesLeadingDigitsOnly() {
    assertNull(SequenceAllocator.parseLeadingInt("abc"))
    assertEquals(12, SequenceAllocator.parseLeadingInt("12"))
    assertEquals(12, SequenceAllocator.parseLeadingInt("12.mp3"))
    assertEquals(12, SequenceAllocator.parseLeadingInt("0012_foo.mp3"))
    assertEquals(1, SequenceAllocator.parseLeadingInt("0001.mp3"))
  }

  @Test
  fun parseIndexWithPrefix_parsesDigitsAfterPrefix() {
    assertNull(SequenceAllocator.parseIndexWithPrefix("0001.mp3", "REC"))
    assertNull(SequenceAllocator.parseIndexWithPrefix("REC.mp3", "REC"))
    assertEquals(1201, SequenceAllocator.parseIndexWithPrefix("REC1201.mp3", "REC"))
    assertEquals(12, SequenceAllocator.parseIndexWithPrefix("REC0012_foo.mp3", "REC"))
    assertEquals(1, SequenceAllocator.parseIndexWithPrefix("0001.mp3", ""))
  }

  @Test
  fun plan_respectsAvoidDuplicates() {
    val used = setOf(1, 2, 4)
    val plan = SequenceAllocator.plan(start = 1, count = 4, used = used, avoidDuplicates = true)
    assertEquals(listOf(3, 5, 6, 7), plan.allocated)
    assertEquals(8, plan.nextAfter)
  }

  @Test
  fun plan_ignoresUsedWhenAvoidDuplicatesIsFalse() {
    val used = setOf(1, 2, 4)
    val plan = SequenceAllocator.plan(start = 1, count = 4, used = used, avoidDuplicates = false)
    assertEquals(listOf(1, 2, 3, 4), plan.allocated)
    assertEquals(5, plan.nextAfter)
  }

  @Test
  fun plan_coercesStartToAtLeastOne() {
    val plan = SequenceAllocator.plan(start = -10, count = 2, used = emptySet(), avoidDuplicates = true)
    assertEquals(listOf(1, 2), plan.allocated)
    assertEquals(3, plan.nextAfter)
  }
}
