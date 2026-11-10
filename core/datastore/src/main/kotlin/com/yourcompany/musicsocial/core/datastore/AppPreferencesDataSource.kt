package com.yourcompany.musicsocial.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { DARK, LIGHT, FOLLOW_SYSTEM }

/** Device-level app preferences. Not synced to the backend. */
@Singleton
class AppPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) {

    /** Dark is the product default until the user picks something else in Settings. */
    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { e ->
            if (e !is IOException) throw e
            logger.error(LogCategory.Database, "Reading preferences failed; using defaults", e)
            emit(emptyPreferences())
        }
        .map { prefs -> prefs[KEY_THEME]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.DARK }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
    }
}
