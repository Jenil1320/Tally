package com.tally.data.seed

import com.tally.core.id.UlidGenerator
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import javax.inject.Inject

/**
 * Seeds first-run data: the system categories and a single clearly-labelled
 * example transaction so the empty app has something to show and the end-to-end
 * data path (mint id -> insert -> observe) is exercised on day one.
 *
 * Idempotent: gated on the category count, so it runs exactly once.
 */
class DatabaseSeeder
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val clock: Clock,
        private val ulidGenerator: UlidGenerator,
    ) {
        suspend fun seedIfEmpty() {
            if (categoryRepository.count() > 0) return

            val categories = DefaultCategories.build(ulidGenerator)
            categoryRepository.addAll(categories)

            val eatingOut = categories.first { it.name == "Eating out" }
            transactionRepository.add(
                NewTransaction(
                    amount = Pence(485),
                    merchant = "Sample coffee",
                    occurredAt = clock.now(),
                    note = "First-run example — delete me",
                    categoryId = eatingOut.id,
                    source = TransactionSource.MANUAL,
                ),
            )
        }
    }
