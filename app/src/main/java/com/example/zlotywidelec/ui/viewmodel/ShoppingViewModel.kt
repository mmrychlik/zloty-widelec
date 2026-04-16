package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.ShoppingDao
import com.example.zlotywidelec.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val shoppingDao: ShoppingDao) : ViewModel() {

    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = shoppingDao.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addItem(name: String, quantity: String) {
        viewModelScope.launch {
            shoppingDao.insertItem(
                ShoppingItemEntity(name = name, quantity = quantity)
            )
        }
    }

    fun toggleItemChecked(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingDao.updateItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingDao.deleteItem(item)
        }
    }
}

class ShoppingViewModelFactory(private val shoppingDao: ShoppingDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(shoppingDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}