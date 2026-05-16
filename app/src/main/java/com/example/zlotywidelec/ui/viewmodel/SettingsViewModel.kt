package com.example.zlotywidelec.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.io.DataBackupManager
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val backupManager: DataBackupManager
) : ViewModel() {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _photoStorageUri = MutableStateFlow(backupManager.getPhotoStorageUri())
    val photoStorageUri: StateFlow<String?> = _photoStorageUri.asStateFlow()

    private var pendingExportSelection: Triple<Boolean, Boolean, Boolean>? = null

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun clearShoppingList() {
        viewModelScope.launch {
            ingredientDao.deleteAllShoppingItems()
        }
    }

    fun clearFridge() {
        viewModelScope.launch {
            ingredientDao.deleteAllFridgeItems()
        }
    }

    fun clearProductSuggestions() {
        viewModelScope.launch {
            ingredientDao.deleteAllProductSuggestions()
        }
    }

    fun clearRecipes() {
        viewModelScope.launch {
            recipeDao.deleteAllUserRecipes()
        }
    }

    fun exportData(uri: Uri, shopping: Boolean, fridge: Boolean, recipes: Boolean) {
        viewModelScope.launch {
            try {
                backupManager.exportData(uri, shopping, fridge, recipes)
                _message.value = "Dane zostały pomyślnie wyeksportowane"
            } catch (e: Exception) {
                _message.value = "Błąd podczas eksportu: ${e.message}"
            }
        }
    }

    fun exportDataAfterSelection(shopping: Boolean, fridge: Boolean, recipes: Boolean) {
        pendingExportSelection = Triple(shopping, fridge, recipes)
    }

    fun handleExportUri(uri: Uri) {
        pendingExportSelection?.let { (shopping, fridge, recipes) ->
            exportData(uri, shopping, fridge, recipes)
        }
        pendingExportSelection = null
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                backupManager.importData(uri)
                _message.value = "Dane zostały pomyślnie zaimportowane"
            } catch (e: Exception) {
                _message.value = "Błąd podczas importu: ${e.message}"
            }
        }
    }

    fun setPhotoStorageUri(uri: Uri) {
        backupManager.takePersistablePermission(uri)
        val uriString = uri.toString()
        backupManager.setPhotoStorageUri(uriString)
        _photoStorageUri.value = uriString
    }

    fun clearMessage() {
        _message.value = null
    }
}

class SettingsViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val backupManager: DataBackupManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(ingredientDao, recipeDao, backupManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
