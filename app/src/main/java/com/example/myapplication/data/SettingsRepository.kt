package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.ai.CachedAdvice
import com.example.myapplication.domain.PeriodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Monthly spending limit in so'm; 0 means "no limit". */
    val monthlyBudget: Flow<Long> = context.dataStore.data.map { it[KEY_BUDGET] ?: 0L }

    suspend fun setMonthlyBudget(value: Long) {
        context.dataStore.edit { it[KEY_BUDGET] = value.coerceAtLeast(0) }
    }

    /** 4-digit PIN code; empty string means no PIN lock. */
    val pinCode: Flow<String> = context.dataStore.data.map { it[KEY_PIN_CODE] ?: "" }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { it[KEY_PIN_CODE] = pin }
    }

    /** Whether biometric (fingerprint/face unlock) is enabled. */
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: true }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    /** Theme mode (SYSTEM, LIGHT, DARK). */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    /** Epoch day of the last manual AI request (enforces 1 request per day limit). */
    val lastAiRequestEpochDay: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_AI_DAY] ?: -1L }

    suspend fun setLastAiRequestEpochDay(epochDay: Long) {
        context.dataStore.edit { it[KEY_LAST_AI_DAY] = epochDay }
    }

    /** Last completed YearMonth string (e.g. "2026-10") that was automatically analyzed by AI. */
    val lastAutoAnalyzedMonth: Flow<String> = context.dataStore.data.map { it[KEY_LAST_AUTO_MONTH] ?: "" }

    suspend fun setLastAutoAnalyzedMonth(yearMonth: String) {
        context.dataStore.edit { it[KEY_LAST_AUTO_MONTH] = yearMonth }
    }

    /** Custom OpenAI API key override if set by user; falls back to built-in key. */
    val customOpenAiKey: Flow<String> = context.dataStore.data.map { it[KEY_OPENAI_KEY] ?: "" }

    suspend fun setCustomOpenAiKey(key: String) {
        context.dataStore.edit { it[KEY_OPENAI_KEY] = key.trim() }
    }

    fun cachedAdvice(period: PeriodType): Flow<CachedAdvice?> = context.dataStore.data.map { prefs ->
        prefs[adviceKey(period)]?.let { raw ->
            runCatching { json.decodeFromString(CachedAdvice.serializer(), raw) }.getOrNull()
        }
    }

    suspend fun saveAdvice(period: PeriodType, advice: CachedAdvice) {
        context.dataStore.edit {
            it[adviceKey(period)] = json.encodeToString(CachedAdvice.serializer(), advice)
        }
    }

    suspend fun clearAdvice() {
        context.dataStore.edit { prefs ->
            PeriodType.entries.forEach { prefs.remove(adviceKey(it)) }
            prefs.remove(KEY_LAST_AI_DAY)
            prefs.remove(KEY_LAST_AUTO_MONTH)
        }
    }

    private fun adviceKey(period: PeriodType) = stringPreferencesKey("advice_${period.name.lowercase()}")

    private companion object {
        val KEY_BUDGET = longPreferencesKey("monthly_budget")
        val KEY_PIN_CODE = stringPreferencesKey("app_pin_code")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val KEY_LAST_AI_DAY = longPreferencesKey("last_ai_request_epoch_day")
        val KEY_LAST_AUTO_MONTH = stringPreferencesKey("last_auto_analyzed_month")
        val KEY_OPENAI_KEY = stringPreferencesKey("custom_openai_key")
    }
}
