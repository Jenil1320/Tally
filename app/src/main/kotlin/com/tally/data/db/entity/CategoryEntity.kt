package com.tally.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a category. Primitives only, so no type converters are needed.
 * Maps to/from `domain.model.Category` in the repository.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int,
    val isSystem: Boolean,
    val sortOrder: Int,
)
