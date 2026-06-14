package com.tally.core.money

import kotlin.math.absoluteValue

/**
 * Money in Tally. Always GBP, always integer pence — never `Float`/`Double`.
 *
 * A `@JvmInline value class` so it costs nothing at runtime (it is a bare
 * `Long` once compiled) while still being a distinct type the compiler can
 * police. `detekt`'s `ForbiddenImport`/no-float rules (M2) keep floating-point
 * arithmetic out of `core/money/`; this type is the reason that rule exists.
 *
 * Arithmetic operators are provided deliberately (see ADR-0002): a money type
 * that you cannot add is hostile to call sites, and the value class makes the
 * operators free. Division is *not* an operator — splitting money is lossy and
 * must go through [splitEvenly] so remainder pennies are accounted for, never
 * silently dropped.
 *
 * @property value the amount in whole pence. `1234` is £12.34; `-500` is -£5.00.
 */
@JvmInline
value class Pence(val value: Long) : Comparable<Pence> {
    operator fun plus(other: Pence): Pence = Pence(value + other.value)

    operator fun minus(other: Pence): Pence = Pence(value - other.value)

    operator fun unaryMinus(): Pence = Pence(-value)

    operator fun times(scalar: Long): Pence = Pence(value * scalar)

    operator fun times(scalar: Int): Pence = Pence(value * scalar)

    override fun compareTo(other: Pence): Int = value.compareTo(other.value)

    val isZero: Boolean get() = value == 0L
    val isNegative: Boolean get() = value < 0L
    val isPositive: Boolean get() = value > 0L

    val absoluteValue: Pence get() = Pence(value.absoluteValue)

    /**
     * Splits this amount into [parts] shares that sum back to exactly this
     * amount. The remainder pennies (at most `parts - 1` of them) are spread
     * one-per-share across the leading shares, so totals always reconcile —
     * this is what the Add-sheet split feature (M2) is built on.
     *
     * @throws IllegalArgumentException if [parts] is not positive.
     */
    fun splitEvenly(parts: Int): List<Pence> {
        require(parts > 0) { "Cannot split into $parts parts" }
        val base = value / parts
        val remainder = (value % parts).toInt()
        val extra = remainder.absoluteValue
        val step = if (value < 0) -1L else 1L
        return List(parts) { index ->
            Pence(base + if (index < extra) step else 0L)
        }
    }

    companion object {
        val ZERO = Pence(0)

        /** `Pence.ofPounds(12, 34)` → £12.34. [pennies] must be in `0..99`. */
        fun ofPounds(
            pounds: Long,
            pennies: Int = 0,
        ): Pence {
            require(pennies in 0..99) { "pennies must be 0..99, was $pennies" }
            val sign = if (pounds < 0) -1 else 1
            return Pence(pounds * 100 + sign * pennies)
        }
    }
}
