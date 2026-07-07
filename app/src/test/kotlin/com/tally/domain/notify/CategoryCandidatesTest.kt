package com.tally.domain.notify

import com.google.common.truth.Truth.assertThat
import com.tally.domain.model.Category
import org.junit.jupiter.api.Test

class CategoryCandidatesTest {
    private val all =
        listOf(
            Category("c1", "Eating out", 1, true, 0),
            Category("c2", "Transport", 2, true, 1),
            Category("c3", "Groceries", 0, true, 2),
            Category("c4", "Shopping", 3, true, 3),
        )

    @Test fun `most-used come first, then display order fills the rest`() {
        val result = categoryCandidates(mostUsedIds = listOf("c3", "c2"), all = all, count = 3)
        assertThat(result.map { it.id }).containsExactly("c3", "c2", "c1").inOrder()
    }

    @Test fun `no usage history falls back to display order`() {
        val result = categoryCandidates(mostUsedIds = emptyList(), all = all, count = 3)
        assertThat(result.map { it.id }).containsExactly("c1", "c2", "c3").inOrder()
    }

    @Test fun `unknown most-used ids are ignored`() {
        val result = categoryCandidates(mostUsedIds = listOf("gone", "c4"), all = all, count = 3)
        assertThat(result.map { it.id }).containsExactly("c4", "c1", "c2").inOrder()
    }

    @Test fun `duplicates across most-used and display order are de-duplicated`() {
        val result = categoryCandidates(mostUsedIds = listOf("c1", "c1"), all = all, count = 3)
        assertThat(result.map { it.id }).containsExactly("c1", "c2", "c3").inOrder()
    }

    @Test fun `count caps the result and empty categories yields nothing`() {
        assertThat(categoryCandidates(listOf("c2"), all, count = 1).map { it.id }).containsExactly("c2")
        assertThat(categoryCandidates(listOf("c2"), emptyList(), count = 3)).isEmpty()
    }
}
