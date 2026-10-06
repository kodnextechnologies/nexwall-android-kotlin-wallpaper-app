package com.kodnex.nexwall.sample.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kodnex.nexwall.sample.data.Category
import com.kodnex.nexwall.sample.data.NexWallException
import com.kodnex.nexwall.sample.data.NexWallRepository
import com.kodnex.nexwall.sample.data.Wallpaper
import com.kodnex.nexwall.sample.data.WallpaperSetter
import com.kodnex.nexwall.sample.data.WallpaperTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Simple back stack without a navigation library. */
sealed interface Screen {
    data object Categories : Screen
    data class Wallpapers(val category: Category?) : Screen
    data class Preview(val wallpaper: Wallpaper) : Screen
}

data class CategoriesState(
    val loading: Boolean = false,
    val items: List<Category> = emptyList(),
    val error: String? = null,
)

data class GridState(
    val items: List<Wallpaper> = emptyList(),
    val page: Int = 0,
    val lastPage: Int = 1,
    val loading: Boolean = false,
    val error: String? = null,
    val sort: String = "newest",
) {
    val hasMore: Boolean get() = page < lastPage
}

class WallpaperViewModel(app: Application) : AndroidViewModel(app) {
    val repository = NexWallRepository()
    private val setter = WallpaperSetter(app)

    val remainingToday: StateFlow<Int?> = repository.remainingToday

    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.Categories))
    val backStack: StateFlow<List<Screen>> = _backStack.asStateFlow()

    private val _categories = MutableStateFlow(CategoriesState())
    val categories: StateFlow<CategoriesState> = _categories.asStateFlow()

    private val _grid = MutableStateFlow(GridState())
    val grid: StateFlow<GridState> = _grid.asStateFlow()

    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages.asStateFlow()

    private var gridCategoryId: Int? = null
    private var loadJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        _categories.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = repository.categories()
                _categories.value = CategoriesState(items = items)
            } catch (e: Exception) {
                _categories.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun openCategory(category: Category?) {
        gridCategoryId = category?.id
        resetGrid(GridState())
        push(Screen.Wallpapers(category))
        loadMore()
    }

    fun openPreview(wallpaper: Wallpaper) = push(Screen.Preview(wallpaper))

    fun changeSort(sort: String) {
        resetGrid(GridState(sort = sort))
        loadMore()
    }

    private fun resetGrid(state: GridState) {
        loadJob?.cancel()
        _grid.value = state
    }

    /** Loads the next page. Stops after an error (e.g. 429) until [retry]. */
    fun loadMore() {
        val state = _grid.value
        if (state.loading || !state.hasMore || state.error != null) return
        _grid.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            try {
                val page = repository.wallpapers(state.page + 1, gridCategoryId, state.sort)
                _grid.update {
                    it.copy(
                        items = it.items + page.data,
                        page = page.currentPage,
                        lastPage = if (page.data.isEmpty()) page.currentPage else page.lastPage,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e // grid was reset (new category or sort); drop this result
            } catch (e: Exception) {
                _grid.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun retry() {
        _grid.update { it.copy(error = null) }
        loadMore()
    }

    fun setWallpaper(wallpaper: Wallpaper, target: WallpaperTarget) {
        viewModelScope.launch {
            _messages.value = try {
                setter.set(wallpaper.imageUrl, target)
                "Wallpaper set"
            } catch (e: Exception) {
                "Could not set wallpaper: ${e.message}"
            }
        }
    }

    fun messageShown() {
        _messages.value = null
    }

    fun back(): Boolean {
        val stack = _backStack.value
        if (stack.size <= 1) return false
        _backStack.value = stack.dropLast(1)
        return true
    }

    private fun push(screen: Screen) {
        _backStack.update { it + screen }
    }

    private fun Exception.toUserMessage(): String =
        (this as? NexWallException)?.userMessage ?: (message ?: "Network error")
}
