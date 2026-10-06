package com.tally.ui.model

import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.ui.components.TransactionRowUi

/**
 * Builds the row a screen shows for this transaction. [category] is the joined
 * category (null when uncategorised) and [subtitle] is whatever the screen wants
 * under the merchant. The share footnote appears only for a shared meal.
 */
fun Transaction.toRowUi(
    category: Category?,
    subtitle: String,
): TransactionRowUi =
    TransactionRowUi(
        id = id,
        merchant = merchant,
        amount = amount,
        categoryName = category?.name,
        colorIndex = category?.colorIndex,
        subtitle = subtitle,
        share = if (isShared) myShare else null,
    )
