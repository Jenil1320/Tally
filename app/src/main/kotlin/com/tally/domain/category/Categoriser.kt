package com.tally.domain.category

import com.tally.domain.model.Category

/**
 * A shipped rule: if [needle] (uppercased) is a substring of the merchant, the
 * transaction is suggested the category named [categoryName].
 */
data class BundledRule(
    val needle: String,
    val categoryName: String,
)

/** Normalises a merchant string for rule matching (trim + uppercase). */
fun normaliseMerchant(merchant: String): String = merchant.trim().uppercase()

/**
 * Suggests a category id for [merchant], deterministically (no AI — the app is
 * local-first). Order:
 *
 *  1. A **learned** rule for the exact normalised merchant (the user's own
 *     correction) — highest priority.
 *  2. A **bundled** rule whose needle is contained in the merchant (e.g.
 *     "PRET A MANGER" contains "PRET" → Eating out).
 *
 * Returns null when nothing matches or the resolved category no longer exists.
 * Pure and unit-tested; shared by manual entry and (later) SMS import.
 */
fun suggestCategory(
    merchant: String,
    categories: List<Category>,
    learnedRules: Map<String, String>,
    bundledRules: List<BundledRule> = BundledMerchantRules.rules,
): String? {
    val key = normaliseMerchant(merchant)
    if (key.isBlank()) return null

    learnedRules[key]?.let { id ->
        if (categories.any { it.id == id }) return id
    }

    val byName = categories.associateBy { it.name.lowercase() }
    for (rule in bundledRules) {
        if (key.contains(rule.needle)) {
            byName[rule.categoryName.lowercase()]?.let { return it.id }
        }
    }
    return null
}
