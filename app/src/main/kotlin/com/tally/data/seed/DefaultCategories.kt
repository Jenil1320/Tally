package com.tally.data.seed

import com.tally.core.id.UlidGenerator
import com.tally.domain.model.Category

/**
 * The system categories Tally seeds on first run. `colorIndex` maps into
 * `TallyColors.categories`; ids are minted as ULIDs at seed time. These can be
 * renamed by the user but not deleted (`isSystem = true`).
 */
object DefaultCategories {
    private val names =
        listOf(
            "Groceries",
            "Eating out",
            "Transport",
            "Shopping",
            "Bills",
            "Health",
            "Entertainment",
            "Home",
            "Other",
        )

    fun build(ulidGenerator: UlidGenerator): List<Category> =
        names.mapIndexed { index, name ->
            Category(
                id = ulidGenerator.next(),
                name = name,
                colorIndex = index,
                isSystem = true,
                sortOrder = index,
            )
        }
}
