package com.tally.data.repo

import com.tally.core.money.Pence
import com.tally.data.db.entity.CategoryEntity
import com.tally.data.db.entity.TransactionEntity
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant

/**
 * Thin entity <-> domain mapping (ARCHITECTURE §7). Lives here, in the data
 * layer, so the domain never sees a Room type and the Room layer never sees a
 * `Pence`/`Instant`.
 */

internal fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = id,
        amount = Pence(amount),
        merchant = merchant,
        note = note,
        categoryId = categoryId,
        occurredAt = Instant.fromEpochMilliseconds(occurredAt),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        source = TransactionSource.valueOf(source),
        myShare = myShare?.let(::Pence),
    )

internal fun NewTransaction.toEntity(
    id: String,
    createdAt: Instant,
): TransactionEntity =
    TransactionEntity(
        id = id,
        amount = amount.value,
        merchant = merchant,
        note = note,
        categoryId = categoryId,
        occurredAt = occurredAt.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        source = source.name,
        smsRaw = null,
        deletedAt = null,
        myShare = myShare?.value,
    )

internal fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        colorIndex = colorIndex,
        isSystem = isSystem,
        sortOrder = sortOrder,
    )

internal fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        colorIndex = colorIndex,
        isSystem = isSystem,
        sortOrder = sortOrder,
    )
