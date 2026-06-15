package com.tally.data.seed

import com.google.common.truth.Truth.assertThat
import com.tally.core.id.UlidGenerator
import org.junit.jupiter.api.Test

class DefaultCategoriesTest {
    @Test fun `builds system categories with unique ids and stable order`() {
        var counter = 0
        val categories = DefaultCategories.all(UlidGenerator { "ID${counter++}" })

        assertThat(categories.all { it.isSystem }).isTrue()
        assertThat(categories.map { it.id }.toSet()).hasSize(categories.size)
        assertThat(categories.map { it.sortOrder }).isEqualTo(categories.indices.toList())
        assertThat(categories.map { it.colorIndex }).isEqualTo(categories.indices.toList())
        assertThat(categories.map { it.name }).containsAtLeast("Rent", "Subscriptions").inOrder()
    }
}
