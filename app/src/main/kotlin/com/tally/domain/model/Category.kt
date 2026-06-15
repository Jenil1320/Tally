package com.tally.domain.model

/**
 * A spending category, in domain terms.
 *
 * @property colorIndex index into `TallyColors.categories` — stored, not a raw
 *   colour, so a palette change re-tints every category consistently.
 * @property isSystem true for the categories Tally seeds on first run; these
 *   can be renamed but not deleted.
 * @property sortOrder display order in lists and the picker.
 */
data class Category(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val isSystem: Boolean,
    val sortOrder: Int,
)
