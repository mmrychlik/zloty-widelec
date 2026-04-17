package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FridgeViewModel(private val ingredientDao: IngredientDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val fridgeItems: StateFlow<List<IngredientEntity>> = ingredientDao.getFridgeItems()
        .combine(_searchQuery) { items, query ->
            if (query.isBlank()) {
                items
            } else {
                items.filter { it.name.contains(query, ignoreCase = true) }
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

    fun addItem(name: String, amount: Double, unit: String) {
        viewModelScope.launch {
            ingredientDao.insertIngredient(
                IngredientEntity(
                    name = name,
                    amount = amount,
                    unit = unit,
                    isInFridge = true
                )
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
