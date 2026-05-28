package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.dao.IngredientNameAndUnit
import com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity
import com.example.zlotywidelec.data.sync.DriveSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Enum defining sort orders for the fridge list.
 */
enum class FridgeSortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    CATEGORY_ASC,
    CATEGORY_DESC
}

/**
 * ViewModel for managing the fridge screen state and operations.
 * Handles searching, sorting, filtering, adding, and deleting ingredients in the fridge.
 */
class FridgeViewModel(
    private val ingredientDao: IngredientDao,
    private val syncManager: DriveSyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(FridgeSortOrder.DATE_DESC)
    val sortOrder: StateFlow<FridgeSortOrder> = _sortOrder

    private val _filterTags = MutableStateFlow<Set<String>>(emptySet())
    val filterTags: StateFlow<Set<String>> = _filterTags

    val suggestions: StateFlow<List<IngredientNameAndUnit>> = ingredientDao.getAllProductSuggestions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fridgeItems: StateFlow<List<IngredientEntity>> = combine(
        ingredientDao.getFridgeItems(),
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

        when (sortOrder) {
            FridgeSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            FridgeSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            FridgeSortOrder.DATE_ASC -> filtered.sortedBy { it.addedAt }
            FridgeSortOrder.DATE_DESC -> filtered.sortedByDescending { it.addedAt }
            FridgeSortOrder.CATEGORY_ASC -> filtered.sortedWith(compareBy({ it.tag.lowercase() }, { it.name.lowercase() }))
            FridgeSortOrder.CATEGORY_DESC -> filtered.sortedWith(compareByDescending<IngredientEntity> { it.tag.lowercase() }.thenBy { it.name.lowercase() })
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
            "NAME" -> if (_sortOrder.value == FridgeSortOrder.NAME_ASC) FridgeSortOrder.NAME_DESC else FridgeSortOrder.NAME_ASC
            "DATE" -> if (_sortOrder.value == FridgeSortOrder.DATE_ASC) FridgeSortOrder.DATE_DESC else FridgeSortOrder.DATE_ASC
            "CATEGORY" -> if (_sortOrder.value == FridgeSortOrder.CATEGORY_ASC) FridgeSortOrder.CATEGORY_DESC else FridgeSortOrder.CATEGORY_ASC
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
            
            val fridgeItems = ingredientDao.getAllFridgeItemsSync()
            val normalizedName = capitalizedName.normalize()
            val existing = fridgeItems.find { it.name.normalize() == normalizedName }

            if (existing != null) {
                val existingAmountBase = convertAmountToBase(existing.amount, existing.unit)
                val addedAmountBase = convertAmountToBase(amount, unit)
                val totalAmountBase = existingAmountBase + addedAmountBase
                val (newAmount, newUnit) = normalizeBaseToBestUnit(totalAmountBase, existing.unit)
                ingredientDao.updateIngredient(existing.copy(amount = newAmount, unit = newUnit))
            } else {
                val (normalizedAmount, normalizedUnit) = normalizeBaseToBestUnit(convertAmountToBase(amount, unit), unit)
                ingredientDao.insertIngredient(
                    IngredientEntity(
                        name = capitalizedName,
                        amount = normalizedAmount,
                        unit = normalizedUnit,
                        tag = tag,
                        isInFridge = true,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
            // Also save as suggestion
            ingredientDao.insertProductSuggestion(
                ProductSuggestionEntity(name = capitalizedName, defaultUnit = unit, tag = tag)
            )

            // Auto-sync
            try {
                val allFridgeItems = ingredientDao.getAllFridgeItemsSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.FRIDGE,
                    allFridgeItems,
                    kotlinx.serialization.builtins.ListSerializer(IngredientEntity.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun normalizeBaseToBestUnit(baseAmount: Double, originalUnit: String): Pair<Double, String> {
        val unit = originalUnit.lowercase().trim()
        return when {
            unit.endsWith("g") || unit.endsWith("kg") || unit.endsWith("dag") -> {
                if (baseAmount >= 1000.0) (baseAmount / 1000.0) to "kg"
                else if (baseAmount >= 100.0 && unit.endsWith("dag")) (baseAmount / 10.0) to "dag"
                else baseAmount to "g"
            }
            unit.endsWith("l") || unit.endsWith("ml") -> {
                if (baseAmount >= 1.0) baseAmount to "l"
                else (baseAmount * 1000.0) to "ml"
            }
            else -> baseAmount to originalUnit
        }
    }

    private fun convertAmountToBase(amount: Double, unit: String): Double {
        return when {
            unit.endsWith("kg") -> amount * 1000.0
            unit.endsWith("dag") -> amount * 10.0
            unit.endsWith("g") && !unit.endsWith("dag") && !unit.endsWith("kg") -> amount
            unit.endsWith("ml") -> amount / 1000.0
            unit.endsWith("l") && !unit.endsWith("ml") -> amount
            else -> amount
        }
    }

    private fun String.normalize(): String {
        val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(temp, "")
            .lowercase()
            .trim()
    }

    fun deleteItem(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.deleteIngredient(item)

            // Auto-sync
            try {
                val allFridgeItems = ingredientDao.getAllFridgeItemsSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.FRIDGE,
                    allFridgeItems,
                    kotlinx.serialization.builtins.ListSerializer(IngredientEntity.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Factory for creating [FridgeViewModel] with required dependencies.
 */
class FridgeViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val syncManager: DriveSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FridgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FridgeViewModel(ingredientDao, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
