package com.tally.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tally.core.money.Pence
import com.tally.domain.insight.CategoryLimitStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.categoryLimitsDataStore by preferencesDataStore(name = "category_limits")

/**
 * Category limits persisted as a JSON map of categoryId → pence in DataStore (no
 * DB migration needed). Corrupt or older JSON degrades to "no limits set" rather
 * than failing, so a bad write can never block the Insights screen.
 */
@Singleton
class DataStoreCategoryLimitStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CategoryLimitStore {
        override val limits: Flow<Map<String, Pence>> =
            context.categoryLimitsDataStore.data.map { prefs ->
                (prefs[LIMITS_JSON]?.let(::decode) ?: emptyMap()).mapValues { (_, pence) -> Pence(pence) }
            }

        override suspend fun setLimit(
            categoryId: String,
            limit: Pence?,
        ) {
            context.categoryLimitsDataStore.edit { prefs ->
                val current = prefs[LIMITS_JSON]?.let(::decode) ?: emptyMap()
                val updated =
                    if (limit == null || limit.value <= 0L) {
                        current - categoryId
                    } else {
                        current + (categoryId to limit.value)
                    }
                prefs[LIMITS_JSON] = Json.encodeToString(updated)
            }
        }

        private fun decode(json: String): Map<String, Long> =
            runCatching { Json.decodeFromString<Map<String, Long>>(json) }.getOrDefault(emptyMap())

        private companion object {
            val LIMITS_JSON = stringPreferencesKey("category_limits_json")
        }
    }
