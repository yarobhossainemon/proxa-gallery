package com.emon.proxagallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_keys")

    val favoriteKeys: Flow<Set<String>> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }

    suspend fun toggle(id: Long, isVideo: Boolean) {
        val key = if (isVideo) "v:$id" else "i:$id"
        appContext.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(key)) {
                current.remove(key)
            }
            preferences[FAVORITES_KEY] = current
        }
    }
}
