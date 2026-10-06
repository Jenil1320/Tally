package com.tally.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.domain.model.Category
import com.tally.ui.theme.TallyTheme
import org.junit.Rule
import org.junit.Test

/**
 * Goldens for the Add sheet in its main states: empty, an ordinary entry, an
 * Eating out entry with a quick share, and one with a custom share being typed.
 * A tall device is used so the whole sheet, keypad included, is in the frame.
 */
class AddSheetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(screenHeight = 3000))

    private val categories =
        listOf(
            Category("gro", "Groceries", 0, true, 0),
            Category("eat", "Eating out", 1, true, 1),
            Category("trn", "Transport", 2, true, 2),
            Category("shp", "Shopping", 3, true, 3),
            Category("bil", "Bills", 4, true, 4),
            Category("hea", "Health", 5, true, 5),
        )

    @Test
    fun newEmpty() {
        paparazzi.snapshot { Sheet(AddUiState(categories = categories, selectedCategoryId = "gro")) }
    }

    @Test
    fun ordinaryEntry() {
        paparazzi.snapshot {
            Sheet(
                AddUiState(
                    amountPence = 1265,
                    merchant = "Sainsbury's Local",
                    categories = categories,
                    selectedCategoryId = "gro",
                    occurredAtLabel = "Today",
                ),
            )
        }
    }

    @Test
    fun eatingOutHalf() {
        paparazzi.snapshot {
            Sheet(
                AddUiState(
                    amountPence = 3840,
                    merchant = "Dishoom Shoreditch",
                    categories = categories,
                    selectedCategoryId = "eat",
                    shareChoice = ShareChoice.Half,
                    occurredAtLabel = "Today",
                ),
            )
        }
    }

    @Test
    fun eatingOutCustomBeingTyped() {
        paparazzi.snapshot {
            Sheet(
                AddUiState(
                    amountPence = 6000,
                    merchant = "Pizza Pilgrims",
                    categories = categories,
                    selectedCategoryId = "eat",
                    shareChoice = ShareChoice.Custom,
                    customSharePence = 1725,
                    activeField = AmountField.Share,
                    editingId = "t1",
                    occurredAtLabel = "Yesterday",
                ),
            )
        }
    }
}

@Composable
private fun Sheet(state: AddUiState) {
    TallyTheme(reduceMotion = true) {
        Box(modifier = Modifier.fillMaxSize().background(TallyTheme.colors.bg0).statusBarsPadding()) {
            AddSheetContent(state = state, actions = AddActions())
        }
    }
}
