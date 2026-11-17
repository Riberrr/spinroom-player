package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val LIBRARY_LIST = booleanPreferencesKey("library_list_mode")

/** Library view mode (grid or list), remembered between launches. */
class ViewModePrefs(private val store: DataStore<Preferences>) {
    /** true = list with preview, false (default) = cover grid. */
    val libraryList: Flow<Boolean> = store.data.map { it[LIBRARY_LIST] ?: false }.distinctUntilChanged()

    suspend fun setLibraryList(value: Boolean) { store.edit { it[LIBRARY_LIST] = value } }
}
