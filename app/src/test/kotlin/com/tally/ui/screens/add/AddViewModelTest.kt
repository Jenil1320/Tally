package com.tally.ui.screens.add

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.fakes.FakeCategoryRepository
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

    private fun viewModel() = AddViewModel(transactions, categories, clock)

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

    @Test fun `auto-selects the first category`() {
        assertThat(viewModel().uiState.value.selectedCategoryId).isEqualTo("c1")
    }

    @Test fun `canSave requires a positive amount and a merchant`() {
        val vm = viewModel()
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.onDigit(5)
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.onMerchantChange("Coffee")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test fun `save writes the transaction and resets the form`() =
        runTest {
            val vm = viewModel()
            vm.onDigit(4)
            vm.onDigit(8)
            vm.onDigit(5)
            vm.onMerchantChange("  Coffee  ")
            vm.onCategorySelected("c2")
            var saved = false
            vm.save { saved = true }

            assertThat(saved).isTrue()
            val stored = transactions.observeAll().first()
            assertThat(stored).hasSize(1)
            assertThat(stored[0].amount).isEqualTo(Pence(485))
            assertThat(stored[0].merchant).isEqualTo("Coffee")
            assertThat(stored[0].categoryId).isEqualTo("c2")

            val reset = vm.uiState.value
            assertThat(reset.amountPence).isEqualTo(0)
            assertThat(reset.merchant).isEmpty()
            assertThat(reset.selectedCategoryId).isEqualTo("c1")
        }

    @Test fun `save is a no-op when the form is incomplete`() =
        runTest {
            val vm = viewModel()
            vm.onDigit(5) // amount but no merchant
            vm.save { }
            assertThat(transactions.observeAll().first()).isEmpty()
        }
}
