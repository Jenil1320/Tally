package com.tally.domain.category

/**
 * The shipped merchant→category rules. [BundledRule.needle] is matched
 * (uppercased) as a substring of the merchant; the category name resolves to one
 * of the seeded default categories. A starting point the learned rules refine.
 */
object BundledMerchantRules {
    val rules: List<BundledRule> =
        listOf(
            // Eating out
            rule("PRET", "Eating out"),
            rule("GREGGS", "Eating out"),
            rule("COSTA", "Eating out"),
            rule("STARBUCKS", "Eating out"),
            rule("MCDONALD", "Eating out"),
            rule("NANDO", "Eating out"),
            rule("DELIVEROO", "Eating out"),
            rule("UBER EATS", "Eating out"),
            rule("JUST EAT", "Eating out"),
            // Groceries
            rule("TESCO", "Groceries"),
            rule("SAINSBURY", "Groceries"),
            rule("ASDA", "Groceries"),
            rule("ALDI", "Groceries"),
            rule("LIDL", "Groceries"),
            rule("WAITROSE", "Groceries"),
            rule("MORRISON", "Groceries"),
            rule("CO-OP", "Groceries"),
            rule("MARKS & SPENCER", "Groceries"),
            // Transport
            rule("TRAINLINE", "Transport"),
            rule("TFL", "Transport"),
            rule("TRANSPORT FOR LONDON", "Transport"),
            rule("NATIONAL RAIL", "Transport"),
            rule("UBER", "Transport"),
            rule("BOLT", "Transport"),
            // Subscriptions — before Shopping so "AMAZON PRIME" beats "AMAZON".
            rule("NETFLIX", "Subscriptions"),
            rule("SPOTIFY", "Subscriptions"),
            rule("DISNEY", "Subscriptions"),
            rule("AMAZON PRIME", "Subscriptions"),
            rule("PRIME VIDEO", "Subscriptions"),
            rule("YOUTUBE PREMIUM", "Subscriptions"),
            rule("AUDIBLE", "Subscriptions"),
            rule("PATREON", "Subscriptions"),
            rule("ICLOUD", "Subscriptions"),
            // Shopping
            rule("AMAZON", "Shopping"),
            rule("EBAY", "Shopping"),
            rule("ARGOS", "Shopping"),
            rule("ASOS", "Shopping"),
            // Entertainment (cinemas; streaming is Subscriptions, above)
            rule("ODEON", "Entertainment"),
            rule("VUE", "Entertainment"),
            // Health
            rule("BOOTS", "Health"),
            rule("SUPERDRUG", "Health"),
            rule("PHARMACY", "Health"),
            // Home
            rule("IKEA", "Home"),
            rule("B&Q", "Home"),
            rule("HOMEBASE", "Home"),
            rule("DUNELM", "Home"),
            // Bills
            rule("BRITISH GAS", "Bills"),
            rule("OCTOPUS ENERGY", "Bills"),
            rule("EDF", "Bills"),
            rule("THAMES WATER", "Bills"),
            rule("VODAFONE", "Bills"),
            rule("COUNCIL TAX", "Bills"),
        )

    private fun rule(
        needle: String,
        categoryName: String,
    ) = BundledRule(needle = needle.uppercase(), categoryName = categoryName)
}
