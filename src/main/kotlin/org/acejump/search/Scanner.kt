package org.acejump.search

import org.acejump.view.Model.boundaries
import org.acejump.view.Model.editor
import org.acejump.view.Model.viewBounds

/**
 * Returns a list of indices where the query begins, within the given range.
 * These are full indices, ie. are not offset to the beginning of the range.
 */

internal object Scanner {
  fun findMatchingSites(searchText: String, model: AceFindModel, cache: Set<Int>) =
    searchText.run {
      val query = model.stringToFind
      if (searchText.isEmpty() || query.isEmpty()) return sortedSetOf<Int>()
      // If the cache is populated, filter it instead of redoing prior work
      if (cache.isEmpty()) findAll(model.toRegex(), boundaries.start)
      else cache.asSequence().filter { index ->
        regionMatches(
          thisOffset = index + query.length - 1,
          other = query.last().toString(),
          otherOffset = 0,
          length = 1,
          ignoreCase = query.last().isLowerCase()
        )
      }
    }.toSortedSet()

  private fun Set<Int>.isCacheValidForRange() =
    viewBounds.let { view -> first() < view.first && last() > view.last }

  private fun CharSequence.findAll(regex: Regex, startingFromIndex: Int) =
    generateSequence(
      seedFunction = { regex.find(this, startingFromIndex) },
      nextFunction = ::filterNextResult
    ).map { it.range.first }

  private tailrec fun filterNextResult(result: MatchResult): MatchResult? {
    val next = result.next() ?: return null
    val offset = next.range.first
    return if (offset !in boundaries) null
    else if (editor.isNotFolded(offset)) next
    else filterNextResult(next)
  }
}
