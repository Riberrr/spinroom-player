package io.github.riberrr.spinroom.core.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.riberrr.spinroom.core.maapi.FavoritesApi
import io.github.riberrr.spinroom.core.maapi.PlayerQueueApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The item a long-press menu is open for. [favorite] is null until MA answered. */
data class MenuTarget(
    val uri: String,
    val title: String,
    val favorite: Boolean? = null,
)

/**
 * Backs the long-press menu on every screen: "Odtwórz następny", "Do kolejki", "Ulubione".
 * Each screen asks for its own instance (`hiltViewModel()`), the commands go to the player chosen in settings.
 */
@HiltViewModel
class ItemMenuViewModel @Inject constructor(
    private val favorites: FavoritesApi,
    private val queues: PlayerQueueApi,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _target = MutableStateFlow<MenuTarget?>(null)
    val target: StateFlow<MenuTarget?> = _target

    /** Opens the menu and looks up the current favorite flag in the background. */
    fun open(uri: String, title: String, favorite: Boolean? = null) {
        _target.value = MenuTarget(uri, title, favorite)
        if (favorite == null) {
            viewModelScope.launch {
                val flag = runCatching { favorites.resolve(uri).favorite }.getOrNull() ?: return@launch
                _target.value = _target.value?.takeIf { it.uri == uri }?.copy(favorite = flag)
            }
        }
    }

    fun dismiss() {
        _target.value = null
    }

    fun playNext() = enqueue("next")

    fun addToQueue() = enqueue("add")

    private fun enqueue(option: String) {
        val uri = _target.value?.uri ?: return
        dismiss()
        viewModelScope.launch {
            val playerId = settings.settings.first().playerId
            if (playerId.isBlank()) return@launch
            runCatching { queues.playMedia(playerId, listOf(uri), option) }
        }
    }

    fun toggleFavorite() {
        val current = _target.value ?: return
        val wanted = !(current.favorite ?: false)
        dismiss()
        viewModelScope.launch { runCatching { favorites.set(current.uri, wanted) } }
    }
}
