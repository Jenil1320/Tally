package com.tally.ui.screens.add

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
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

    private fun AddViewModel.enter(
        pence: Int,
        merchant: String = "Dishoom",
    ) {
        pence.toString().forEach { onDigit(it.digitToInt()) }
        onMerchantChange(merchant)
    }

    @Test fun `eating out asks for a share and other categories do not`() {
        val vm = viewModel()
        vm.onCategorySelected("c1")
        assertThat(vm.uiState.value.asksForShare).isTrue()
        vm.onCategorySelected("c2")
        assertThat(vm.uiState.value.asksForShare).isFalse()
    }

    @Test fun `half and a third use remainder-safe pence`() {
        val vm = viewModel()
        vm.onCategorySelected("c1")
        vm.enter(3841) // 38.41
        vm.onShareChoice(ShareChoice.Half)
        assertThat(vm.uiState.value.myShare).isEqualTo(Pence(1921)) // the extra penny stays with the user
        vm.onShareChoice(ShareChoice.Third)
        assertThat(vm.uiState.value.myShare).isEqualTo(Pence(1281))
        vm.onShareChoice(ShareChoice.All)
        assertThat(vm.uiState.value.myShare).isNull()
    }

    @Test fun `custom share is typed on the keypad and capped at the amount paid`() {
        val vm = viewModel()
        vm.onCategorySelected("c1")
        vm.enter(4000)
        vm.onShareChoice(ShareChoice.Custom)
        assertThat(vm.uiState.value.activeField).isEqualTo(AmountField.Share)
        vm.onDigit(1)
        vm.onDigit(2)
        vm.onDigit(5)
        vm.onDigit(0)
        assertThat(vm.uiState.value.myShare).isEqualTo(Pence(1250))
        assertThat(vm.uiState.value.amountPence).isEqualTo(4000) // the total did not move
        repeat(3) { vm.onDigit(9) }
        assertThat(vm.uiState.value.myShare).isEqualTo(Pence(4000)) // capped at what was paid
    }

    @Test fun `custom share must be entered before saving`() {
        val vm = viewModel()
        vm.onCategorySelected("c1")
        vm.enter(4000)
        assertThat(vm.uiState.value.canSave).isTrue()
        vm.onShareChoice(ShareChoice.Custom)
        assertThat(vm.uiState.value.canSave).isFalse()
        vm.onDigit(0) // an explicit zero share is allowed
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test fun `saving an eating out entry stores the share and keeps the amount paid`() =
        runTest {
            val vm = viewModel()
            vm.onCategorySelected("c1")
            vm.enter(6000)
            vm.onShareChoice(ShareChoice.Half)
            vm.save { }
            val stored = transactions.observeAll().first().single()
            assertThat(stored.amount).isEqualTo(Pence(6000))
            assertThat(stored.myShare).isEqualTo(Pence(3000))
        }

    @Test fun `a share is dropped when the category is not eating out`() =
        runTest {
            val vm = viewModel()
            vm.onCategorySelected("c1")
            vm.enter(6000, merchant = "Taxi")
            vm.onShareChoice(ShareChoice.Half)
            vm.onCategorySelected("c2")
            vm.save { }
            assertThat(transactions.observeAll().first().single().myShare).isNull()
        }

    @Test fun `editing loads the saved share and can change it`() =
        runTest {
            val id =
                transactions.add(
                    NewTransaction(
                        Pence(6000),
                        "Dishoom",
                        occurredAt = Instant.fromEpochMilliseconds(900),
                        categoryId = "c1",
                        myShare = Pence(1700),
                    ),
                )
            val vm = viewModel()
            vm.start(id)
            assertThat(vm.uiState.value.shareChoice).isEqualTo(ShareChoice.Custom)
            assertThat(vm.uiState.value.myShare).isEqualTo(Pence(1700))
            vm.onShareChoice(ShareChoice.Half)
            vm.save { }
            assertThat(transactions.observeAll().first().single().myShare).isEqualTo(Pence(3000))
        }

    @Test fun `editing recognises an even split`() =
        runTest {
            val id =
                transactions.add(
                    NewTransaction(Pence(6000), "Dishoom", Instant.fromEpochMilliseconds(900), categoryId = "c1", myShare = Pence(3000)),
                )
            val vm = viewModel()
            vm.start(id)
            assertThat(vm.uiState.value.shareChoice).isEqualTo(ShareChoice.Half)
        }

    @Test fun `editing an auto-captured payment keeps it marked as auto-captured`() =
        runTest {
            val id =
                transactions.add(
                    NewTransaction(
                        Pence(485),
                        "Pret",
                        occurredAt = Instant.fromEpochMilliseconds(900),
                        categoryId = "c1",
                        source = TransactionSource.NOTIFICATION,
                    ),
                )
            val vm = viewModel()
            vm.start(id)
            vm.onNoteChange("with Sam")
            vm.save { }
            val stored = transactions.observeAll().first().single()
            assertThat(stored.source).isEqualTo(TransactionSource.NOTIFICATION)
            assertThat(stored.note).isEqualTo("with Sam")
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
