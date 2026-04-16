package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.FridgeDao
import com.example.zlotywidelec.data.local.entity.FridgeItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FridgeViewModel(private val fridgeDao: FridgeDao) : ViewModel() {

    val fridgeItems: StateFlow<List<FridgeItemEntity>> = fridgeDao.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addItem(name: String, quantity: String, expirationDate: Long? = null) {
        viewModelScope.launch {
            fridgeDao.insertItem(
                FridgeItemEntity(
                    name = name,
                    quantity = quantity,
                    expirationDate = expirationDate
                )
            )
        }
    }

    fun deleteItem(item: FridgeItemEntity) {
        viewModelScope.launch {
            fridgeDao.deleteItem(item)
        }
    }

    fun updateItem(item: FridgeItemEntity) {
        viewModelScope.launch {
            fridgeDao.updateItem(item)
        }
    }

    fun removeIngredients(ingredients: List<Pair<String, String>>) {
        viewModelScope.launch {
            ingredients.forEach { (name, _) ->
                // Basic implementation: delete items by name
                // In a real app, you'd probably want to subtract quantities
                fridgeDao.deleteByName(name)
            }
        }
    }

    fun syncWithSmartFridge() {
        viewModelScope.launch {
            // Mocking an API call with delay
            kotlinx.coroutines.delay(1500)
            val smartItems = listOf(
                FridgeItemEntity(name = "Mleko (Smart)", quantity = "1L", isFromSmartFridge = true),
                FridgeItemEntity(name = "Jajka (Smart)", quantity = "6 szt", isFromSmartFridge = true),
                FridgeItemEntity(name = "Ser żółty (Smart)", quantity = "200g", isFromSmartFridge = true)
            )
            smartItems.forEach { fridgeDao.insertItem(it) }
        }
    }
}

class FridgeViewModelFactory(private val fridgeDao: FridgeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FridgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FridgeViewModel(fridgeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
