package com.tally.data.seed

import com.tally.core.id.UlidGenerator
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Sets up baseline data. Runs on every launch and is idempotent:
 *
 *  - **Categories** — inserts any default category not already present (matched
 *    by name), so installs that predate a new default (e.g. Rent, Subscriptions)
 *    gain it without disturbing the user's existing categories.
 *  - **Example transaction** — only on a genuinely fresh install (no categories
 *    yet), so it isn't re-added later.
 *
 * Name-matching assumes system categories aren't renamed; there is no rename UI
 * today, and this becomes a stable-key lookup if one is ever added.
 */
class DatabaseSeeder
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val clock: Clock,
        private val ulidGenerator: UlidGenerator,
    ) {
        suspend fun ensureSetup() {
            val existing = categoryRepository.observeAll().first()
            val freshInstall = existing.isEmpty()

            val existingNames = existing.map { it.name }.toSet()
            val toInsert = DefaultCategories.all(ulidGenerator).filter { it.name !in existingNames }
            if (toInsert.isNotEmpty()) {
                categoryRepository.addAll(toInsert)
            }

            if (freshInstall && transactionRepository.countLive() == 0) {
                val eatingOut = toInsert.first { it.name == "Eating out" }
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
    }
