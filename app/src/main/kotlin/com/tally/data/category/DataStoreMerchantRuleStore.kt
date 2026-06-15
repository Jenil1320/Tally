package com.tally.data.category

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tally.domain.category.MerchantRuleStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.merchantRulesDataStore by preferencesDataStore(name = "merchant_rules")

/**
 * Learned merchant rules persisted as a JSON map in DataStore (no DB migration
 * needed). Corrupt/old JSON degrades gracefully to an empty rule set.
 */
@Singleton
class DataStoreMerchantRuleStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MerchantRuleStore {
        override val rules: Flow<Map<String, String>> =
            context.merchantRulesDataStore.data.map { prefs ->
                prefs[RULES_JSON]?.let(::decode) ?: emptyMap()
            }

        override suspend fun learn(
            merchantKey: String,
            categoryId: String,
        ) {
            context.merchantRulesDataStore.edit { prefs ->
                val current = prefs[RULES_JSON]?.let(::decode) ?: emptyMap()
                prefs[RULES_JSON] = Json.encodeToString(current + (merchantKey to categoryId))
            }
        }

        private fun decode(json: String): Map<String, String> =
            runCatching { Json.decodeFromString<Map<String, String>>(json) }.getOrDefault(emptyMap())

        private companion object {
            val RULES_JSON = stringPreferencesKey("merchant_rules_json")
        }
    }
