package com.tally.domain.notify

import com.tally.domain.model.Category

/**
 * Chooses up to [count] categories to offer as one-tap buttons on the "pick a
 * category" notification, when auto-categorisation found no match.
 *
 * Ordering: the user's [mostUsedIds] first (their likeliest choices), then the
 * remaining categories in display order to fill the slots — so a fresh install
 * with no history still gets sensible buttons. Pure and unit-tested; the caller
 * supplies `mostUsedIds` from the repository and `all` from the category list.
 * Android shows at most three notification actions, hence the default [count].
 */
fun categoryCandidates(
    mostUsedIds: List<String>,
    all: List<Category>,
    count: Int = 3,
): List<Category> {
    if (all.isEmpty() || count <= 0) return emptyList()
    val byId = all.associateBy { it.id }
    val ordered = LinkedHashSet<Category>()
    mostUsedIds.forEach { id -> byId[id]?.let(ordered::add) }
    all.forEach(ordered::add)
    return ordered.take(count)
}
