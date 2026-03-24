package com.maomaochongapp.sequence

data class SequencePlan(
  val allocated: List<Int>,
  val nextAfter: Int,
)

object SequenceAllocator {
  fun plan(
    start: Int,
    count: Int,
    used: Set<Int>,
    avoidDuplicates: Boolean,
  ): SequencePlan {
    if (count <= 0) return SequencePlan(allocated = emptyList(), nextAfter = start)
    val allocated = ArrayList<Int>(count)
    var candidate = start.coerceAtLeast(1)
    while (allocated.size < count) {
      if (!avoidDuplicates || candidate !in used) {
        allocated.add(candidate)
      }
      candidate++
    }
    return SequencePlan(allocated = allocated, nextAfter = allocated.last() + 1)
  }

  fun parseLeadingInt(name: String?): Int? {
    if (name.isNullOrBlank()) return null
    val digits = buildString {
      for (c in name) {
        if (c in '0'..'9') append(c) else break
      }
    }
    if (digits.isBlank()) return null
    return digits.toIntOrNull()
  }

  fun parseIndexWithPrefix(name: String?, prefix: String): Int? {
    if (name.isNullOrBlank()) return null
    val p = prefix.trim()
    if (p.isBlank()) return parseLeadingInt(name)
    if (!name.startsWith(p)) return null
    val rest = name.substring(p.length)
    val digits = buildString {
      for (c in rest) {
        if (c in '0'..'9') append(c) else break
      }
    }
    if (digits.isBlank()) return null
    return digits.toIntOrNull()
  }
}
