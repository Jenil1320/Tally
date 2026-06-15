package com.tally.ui.screens.add

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeMerchantRuleStore
import com.tally.fakes.FakeTransactionRepository
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
}
