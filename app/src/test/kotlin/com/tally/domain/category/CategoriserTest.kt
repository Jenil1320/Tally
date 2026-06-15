package com.tally.domain.category

import com.google.common.truth.Truth.assertThat
import com.tally.domain.model.Category
import org.junit.jupiter.api.Test

class CategoriserTest {
    private val categories =
        listOf(
            Category("c1", "Eating out", 1, true, 0),
            Category("c2", "Transport", 2, true, 1),
            Category("c3", "Groceries", 0, true, 2),
        )

    @Test fun `bundled rule matches as a substring`() {
        assertThat(suggestCategory("PRET A MANGER", categories, emptyMap())).isEqualTo("c1")
        assertThat(suggestCategory("Tesco Express", categories, emptyMap())).isEqualTo("c3")
        assertThat(suggestCategory("TRAINLINE.COM", categories, emptyMap())).isEqualTo("c2")
    }

    @Test fun `a learned rule overrides the bundled one`() {
        val learned = mapOf("TESCO EXPRESS" to "c2")
        assertThat(suggestCategory("Tesco Express", categories, learned)).isEqualTo("c2")
    }

    @Test fun `no match returns null`() {
        assertThat(suggestCategory("Random Corner Shop", categories, emptyMap())).isNull()
        assertThat(suggestCategory("   ", categories, emptyMap())).isNull()
    }

    @Test fun `streaming maps to Subscriptions and Amazon Prime beats Amazon`() {
        val cats =
            listOf(
                Category("sub", "Subscriptions", 10, true, 0),
                Category("shop", "Shopping", 3, true, 1),
            )
        assertThat(suggestCategory("NETFLIX", cats, emptyMap())).isEqualTo("sub")
        assertThat(suggestCategory("Amazon Prime Video", cats, emptyMap())).isEqualTo("sub")
        assertThat(suggestCategory("Amazon", cats, emptyMap())).isEqualTo("shop")
    }

    @Test fun `a learned rule for a deleted category falls through`() {
        val learned = mapOf("PRET A MANGER" to "gone")
        // Learned target no longer exists -> fall back to the bundled Eating out.
        assertThat(suggestCategory("Pret a Manger", categories, learned)).isEqualTo("c1")
    }
}
