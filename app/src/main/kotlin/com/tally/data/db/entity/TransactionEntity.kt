package com.tally.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a transaction. Primitives only (money as `Long` pence, time as
 * epoch-milli `Long`), so no type converters are needed; the repository maps to
 * `domain.model.Transaction`.
 *
 * - PK is a ULID (sorts by creation time).
 * - Indexed on `categoryId`, `occurredAt`, and `deletedAt` — the columns the
 *   list, date-range, and live-rows queries filter/sort on (ARCHITECTURE §1).
 * - `deletedAt` is the soft-delete tombstone (null = live).
 * - `smsRaw` holds the original bank SMS; it is encrypted at rest from M4 and is
 *   excluded from any future cloud-sync payload (PRIVACY). Null in v1.
 * - Deleting a category nulls the FK rather than cascading away spend history.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("occurredAt"),
        Index("deletedAt"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val merchant: String,
    val note: String?,
    val categoryId: String?,
    val occurredAt: Long,
    val createdAt: Long,
    val source: String,
    val smsRaw: String?,
    val deletedAt: Long?,
)
