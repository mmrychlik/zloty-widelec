package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class ShoppingSortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    CATEGORY_ASC,
    CATEGORY_DESC
}

class ShoppingViewModel(private val ingredientDao: IngredientDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(ShoppingSortOrder.DATE_DESC)
    val sortOrder: StateFlow<ShoppingSortOrder> = _sortOrder

    private val _filterTags = MutableStateFlow<Set<String>>(emptySet())
    val filterTags: StateFlow<Set<String>> = _filterTags

    val suggestions: StateFlow<List<IngredientNameAndUnit>> = ingredientDao.getAllProductSuggestions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shoppingItems: StateFlow<List<IngredientEntity>> = combine(
        ingredientDao.getShoppingListItems(),
        _searchQuery,
        _sortOrder,
        _filterTags
    ) { items, query, sortOrder, tags ->
        var filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (tags.isNotEmpty()) {
            filtered = filtered.filter { it.tag in tags }
        }

        // We keep checked items at the bottom, and sort within groups
        filtered.sortedWith(
            compareBy<IngredientEntity> { it.isChecked }
                .thenBy {
                    when (sortOrder) {
                        ShoppingSortOrder.NAME_ASC -> it.name.lowercase()
                        ShoppingSortOrder.NAME_DESC -> "" // Placeholder for descending below
                        ShoppingSortOrder.DATE_ASC -> it.addedAt.toString()
                        ShoppingSortOrder.DATE_DESC -> "" 
                        ShoppingSortOrder.CATEGORY_ASC -> it.tag.lowercase()
                        ShoppingSortOrder.CATEGORY_DESC -> ""
                    }
                }
        ).let { sorted ->
            // Handle descending orders which are tricky with thenBy
            if (sortOrder == ShoppingSortOrder.NAME_DESC) {
                filtered.sortedWith(compareBy<IngredientEntity> { it.isChecked }.thenByDescending { it.name.lowercase() })
            } else if (sortOrder == ShoppingSortOrder.DATE_DESC) {
                filtered.sortedWith(compareBy<IngredientEntity> { it.isChecked }.thenByDescending { it.addedAt })
            } else if (sortOrder == ShoppingSortOrder.CATEGORY_DESC) {
                filtered.sortedWith(compareBy<IngredientEntity> { it.isChecked }.thenByDescending { it.tag.lowercase() }.thenBy { it.name.lowercase() })
            } else {
                sorted
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSortOrder(type: String) {
        _sortOrder.value = when (type) {
            "NAME" -> if (_sortOrder.value == ShoppingSortOrder.NAME_ASC) ShoppingSortOrder.NAME_DESC else ShoppingSortOrder.NAME_ASC
            "DATE" -> if (_sortOrder.value == ShoppingSortOrder.DATE_ASC) ShoppingSortOrder.DATE_DESC else ShoppingSortOrder.DATE_ASC
            "CATEGORY" -> if (_sortOrder.value == ShoppingSortOrder.CATEGORY_ASC) ShoppingSortOrder.CATEGORY_DESC else ShoppingSortOrder.CATEGORY_ASC
            else -> _sortOrder.value
        }
    }

    fun toggleFilterTag(tag: String) {
        val current = _filterTags.value
        if (tag.isEmpty()) {
            _filterTags.value = emptySet()
        } else {
            _filterTags.value = if (current.contains(tag)) {
                current - tag
            } else {
                current + tag
            }
        }
    }

    fun addItem(name: String, amount: Double, unit: String, tag: String = "") {
        viewModelScope.launch {
            val capitalizedName = name.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            ingredientDao.insertIngredient(
                IngredientEntity(name = capitalizedName, amount = amount, unit = unit, tag = tag)
            )
            // Also save as suggestion
            ingredientDao.insertProductSuggestion(
                ProductSuggestionEntity(name = capitalizedName, defaultUnit = unit, tag = tag)
            )
        }
    }

    fun toggleItemChecked(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.updateIngredient(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteItem(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.deleteIngredient(item)
        }
    }

    fun moveCheckedToFridge() {
        viewModelScope.launch {
            ingredientDao.moveCheckedToFridge(System.currentTimeMillis())
        }
    }
}

class ShoppingViewModelFactory(private val ingredientDao: IngredientDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(ingredientDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
