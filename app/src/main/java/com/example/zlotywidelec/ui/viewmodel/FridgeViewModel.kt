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

enum class FridgeSortOrder {
    ALPHABETICAL,
    DATE_ADDED
}

class FridgeViewModel(private val ingredientDao: IngredientDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(FridgeSortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<FridgeSortOrder> = _sortOrder

    val suggestions: StateFlow<List<IngredientNameAndUnit>> = ingredientDao.getAllProductSuggestions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fridgeItems: StateFlow<List<IngredientEntity>> = combine(
        ingredientDao.getFridgeItems(),
        _searchQuery,
        _sortOrder
    ) { items, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }

        when (sortOrder) {
            FridgeSortOrder.ALPHABETICAL -> filtered.sortedBy { it.name }
            FridgeSortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
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

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == FridgeSortOrder.ALPHABETICAL) {
            FridgeSortOrder.DATE_ADDED
        } else {
            FridgeSortOrder.ALPHABETICAL
        }
    }

    fun addItem(name: String, amount: Double, unit: String) {
        viewModelScope.launch {
            val capitalizedName = name.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            ingredientDao.insertIngredient(
                IngredientEntity(
                    name = capitalizedName,
                    amount = amount,
                    unit = unit,
                    isInFridge = true,
                    addedAt = System.currentTimeMillis()
                )
            )
            // Also save as suggestion
            ingredientDao.insertProductSuggestion(
                ProductSuggestionEntity(name = capitalizedName, defaultUnit = unit)
            )
        }
    }

    fun deleteItem(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.deleteIngredient(item)
        }
    }
}

class FridgeViewModelFactory(private val ingredientDao: IngredientDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FridgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FridgeViewModel(ingredientDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
