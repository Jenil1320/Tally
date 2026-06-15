package com.tally.ui.screens.add

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeMerchantRuleStore
import com.tally.fakes.FakeTransactionRepository
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AddViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = FakeClock(1_000)
    private val transactions = FakeTransactionRepository(clock)
    private val categories =
        FakeCategoryRepository(
            listOf(
                Category("c1", "Eating out", 1, true, 0),
                Category("c2", "Transport", 2, true, 1),
            ),
        )
    private val ruleStore = FakeMerchantRuleStore()

    private fun viewModel() = AddViewModel(transactions, categories, ruleStore, clock)

    @Test fun `keypad builds pence from the right`() {
        val vm = viewModel()
        vm.onDigit(4)
        vm.onDigit(8)
        vm.onDigit(5)
        assertThat(vm.uiState.value.amountPence).isEqualTo(485)
        vm.onBackspace()
        assertThat(vm.uiState.value.amountPence).isEqualTo(48)
        vm.onDoubleZero()
        assertThat(vm.uiState.value.amountPence).isEqualTo(4800)
    }

    @Test fun `canSave requires a positive amount and a merchant`() {
        val vm = viewModel()
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.onDigit(5)
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.onMerchantChange("Coffee")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test fun `merchant change auto-suggests a category from bundled rules`() {
        val vm = viewModel()
        vm.onMerchantChange("Pret a Manger")
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo("c1") // Eating out
    }

    @Test fun `a manual category choice is not clobbered by later merchant edits`() {
        val vm = viewModel()
        vm.onCategorySelected("c2")
        vm.onMerchantChange("Pret a Manger")
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo("c2")
    }

    @Test fun `save writes the transaction, learns the rule, and resets`() =
        runTest {
            val vm = viewModel()
            vm.onDigit(4)
            vm.onDigit(8)
            vm.onDigit(5)
            vm.onMerchantChange("  Pret  ")
            var saved = false
            vm.save { saved = true }

            assertThat(saved).isTrue()
            val stored = transactions.observeAll().first()
            assertThat(stored).hasSize(1)
            assertThat(stored[0].amount).isEqualTo(Pence(485))
            assertThat(stored[0].categoryId).isEqualTo("c1")
            assertThat(ruleStore.learned["PRET"]).isEqualTo("c1")
            assertThat(vm.uiState.value.amountPence).isEqualTo(0)
            assertThat(vm.uiState.value.merchant).isEmpty()
        }

    @Test fun `save is a no-op when the form is incomplete`() =
        runTest {
            val vm = viewModel()
            vm.onDigit(5)
            vm.save { }
            assertThat(transactions.observeAll().first()).isEmpty()
        }

    @Test fun `editing loads the transaction and updates it in place`() =
        runTest {
            val id =
                transactions.add(
                    NewTransaction(Pence(485), "Pret", occurredAt = Instant.fromEpochMilliseconds(900), categoryId = "c1"),
                )
            val vm = viewModel()
            vm.start(id)
            assertThat(vm.uiState.value.isEditing).isTrue()
            assertThat(vm.uiState.value.amountPence).isEqualTo(485)
            assertThat(vm.uiState.value.merchant).isEqualTo("Pret")

            vm.onDigit(0) // 485 -> 4850
            vm.save { }

            val all = transactions.observeAll().first()
            assertThat(all).hasSize(1) // updated in place, not a new row
            assertThat(all[0].id).isEqualTo(id)
            assertThat(all[0].amount).isEqualTo(Pence(4850))
            assertThat(all[0].occurredAt).isEqualTo(Instant.fromEpochMilliseconds(900)) // original date kept
        }

    @Test fun `delete removes the edited transaction`() =
        runTest {
            val id = transactions.add(NewTransaction(Pence(485), "Pret", occurredAt = Instant.fromEpochMilliseconds(900)))
            val vm = viewModel()
            vm.start(id)
            vm.delete { }
            assertThat(transactions.observeAll().first()).isEmpty()
        }

    @Test fun `duplicate creates a second copy`() =
        runTest {
            val id = transactions.add(NewTransaction(Pence(485), "Pret", occurredAt = Instant.fromEpochMilliseconds(900)))
            val vm = viewModel()
            vm.start(id)
            vm.duplicate { }
            val all = transactions.observeAll().first()
            assertThat(all).hasSize(2)
            assertThat(all.map { it.merchant }.toSet()).containsExactly("Pret")
        }

    @Test fun `a selected date is applied on save`() =
        runTest {
            val vm = viewModel()
            vm.onDigit(5)
            vm.onMerchantChange("Thing")
            vm.onDateSelected(Instant.parse("2020-01-15T00:00:00Z").toEpochMilliseconds())
            vm.save { }
            val stored = transactions.observeAll().first().first()
            val year = stored.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).year
            assertThat(year).isEqualTo(2020)
        }
}
