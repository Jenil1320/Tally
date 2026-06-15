package com.tally.data.seed

import com.tally.core.id.UlidGenerator
import com.tally.domain.model.Category

/**
 * The system categories Tally ships. `colorIndex` maps into
 * `TallyColors.categories` and is **stable per category** (so colours never
 * shuffle when the set grows). `sortOrder` is the list position. Rent and
 * Subscriptions were added after launch and sit at the end so existing installs
 * gain them without renumbering the originals (see [DatabaseSeeder]).
 */
object DefaultCategories {
    private data class Spec(val name: String, val colorIndex: Int)

    private val specs =
        listOf(
            Spec("Groceries", 0),
            Spec("Eating out", 1),
            Spec("Transport", 2),
            Spec("Shopping", 3),
            Spec("Bills", 4),
            Spec("Health", 5),
            Spec("Entertainment", 6),
            Spec("Home", 7),
            Spec("Other", 8),
            Spec("Rent", 9),
            Spec("Subscriptions", 10),
        )

    /** The full default set, with fresh ULIDs and `sortOrder` = list position. */
    fun all(ulidGenerator: UlidGenerator): List<Category> =
        specs.mapIndexed { index, spec ->
            Category(
                id = ulidGenerator.next(),
                name = spec.name,
                colorIndex = spec.colorIndex,
                isSystem = true,
                sortOrder = index,
            )
        }
}
