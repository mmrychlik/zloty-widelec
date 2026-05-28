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
 * Enum defining sort orders for the shopping list.
 */
enum class ShoppingSortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    CATEGORY_ASC,
    CATEGORY_DESC
}

/**
 * ViewModel for managing the shopping list screen state and operations.
 * Handles adding, updating, deleting, and checking items, as well as syncing with Google Drive.
 * Includes logic for calculating missing ingredients from recipes based on fridge contents.
 */
class ShoppingViewModel(
    private val ingredientDao: IngredientDao,
    private val syncManager: DriveSyncManager
) : ViewModel() {

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
            
            val shoppingItems = ingredientDao.getAllShoppingItemsSync()
            val normalizedName = capitalizedName.normalize()
            val existing = shoppingItems.find { it.name.normalize() == normalizedName && !it.isChecked }

            if (existing != null) {
                val existingAmountBase = convertAmountToBase(existing.amount, existing.unit)
                val addedAmountBase = convertAmountToBase(amount, unit)
                val totalAmountBase = existingAmountBase + addedAmountBase
                val (newAmount, newUnit) = normalizeBaseToBestUnit(totalAmountBase, existing.unit)
                ingredientDao.updateIngredient(existing.copy(amount = newAmount, unit = newUnit))
            } else {
                val (normalizedAmount, normalizedUnit) = normalizeBaseToBestUnit(convertAmountToBase(amount, unit), unit)
                ingredientDao.insertIngredient(
                    IngredientEntity(name = capitalizedName, amount = normalizedAmount, unit = normalizedUnit, tag = tag)
                )
            }
            
            // Also save as suggestion
            ingredientDao.insertProductSuggestion(
                ProductSuggestionEntity(name = capitalizedName, defaultUnit = unit, tag = tag)
            )
            
            // Auto-sync
            syncShopping()
        }
    }

    fun updateItem(item: IngredientEntity, name: String, amount: Double, unit: String, tag: String) {
        viewModelScope.launch {
            val capitalizedName = name.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            ingredientDao.updateIngredient(
                item.copy(name = capitalizedName, amount = amount, unit = unit, tag = tag)
            )
            
            syncShopping()
        }
    }

    private suspend fun syncShopping() {
        try {
            val allShoppingItems = ingredientDao.getAllShoppingItemsSync()
            syncManager.uploadCategoryData(
                com.example.zlotywidelec.data.sync.DriveSyncManager.Category.SHOPPING,
                allShoppingItems,
                kotlinx.serialization.builtins.ListSerializer(IngredientEntity.serializer())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleItemChecked(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.updateIngredient(item.copy(isChecked = !item.isChecked))
            
            // Auto-sync
            syncShopping()
        }
    }

    fun deleteItem(item: IngredientEntity) {
        viewModelScope.launch {
            ingredientDao.deleteIngredient(item)
            
            // Auto-sync
            syncShopping()
        }
    }

    fun deleteCheckedItems() {
        viewModelScope.launch {
            ingredientDao.deleteCheckedShoppingItems()
            syncShopping()
        }
    }

    fun moveCheckedToFridge() {
        viewModelScope.launch {
            val checkedItems = ingredientDao.getAllShoppingItemsSync().filter { it.isChecked }
            val fridgeItems = ingredientDao.getAllFridgeItemsSync()
            val timestamp = System.currentTimeMillis()

            checkedItems.forEach { shoppingItem ->
                val normalizedName = shoppingItem.name.normalize()
                // Find any matching item in fridge by name, we'll merge and normalize units
                val existingInFridge = fridgeItems.find { 
                    it.name.normalize() == normalizedName 
                }

                if (existingInFridge != null) {
                    val baseExisting = convertAmountToBase(existingInFridge.amount, existingInFridge.unit)
                    val baseAdded = convertAmountToBase(shoppingItem.amount, shoppingItem.unit)
                    val (newAmount, newUnit) = normalizeBaseToBestUnit(baseExisting + baseAdded, existingInFridge.unit)
                    
                    ingredientDao.updateIngredient(existingInFridge.copy(
                        amount = newAmount,
                        unit = newUnit,
                        addedAt = timestamp
                    ))
                    ingredientDao.deleteIngredient(shoppingItem)
                } else {
                    val (normalizedAmount, normalizedUnit) = normalizeBaseToBestUnit(
                        convertAmountToBase(shoppingItem.amount, shoppingItem.unit), 
                        shoppingItem.unit
                    )
                    ingredientDao.updateIngredient(shoppingItem.copy(
                        amount = normalizedAmount,
                        unit = normalizedUnit,
                        isInFridge = true,
                        isChecked = false,
                        addedAt = timestamp
                    ))
                }
            }
            
            // Auto-sync
            try {
                // Sync both since moving affects both
                val allShoppingItems = ingredientDao.getAllShoppingItemsSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.SHOPPING,
                    allShoppingItems,
                    kotlinx.serialization.builtins.ListSerializer(IngredientEntity.serializer())
                )
                
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

    fun addIngredientsFromRecipe(ingredients: List<com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity>) {
        viewModelScope.launch {
            val fridgeItems = ingredientDao.getAllFridgeItemsSync()
            val shoppingItems = ingredientDao.getAllShoppingItemsSync()
            
            ingredients.forEach { ri ->
                val normalizedName = ri.name.normalize()
                val matchesFridge = fridgeItems.filter { it.name.normalize() == normalizedName }
                
                val amountInFridgeBase = matchesFridge.sumOf { convertAmountToBase(it.amount, it.unit) }
                val requiredAmountBase = convertAmountToBase(ri.amount, ri.unit)
                
                val missingAmountBase = requiredAmountBase - amountInFridgeBase
                
                if (missingAmountBase > 0) {
                    val matchShopping = shoppingItems.find { it.name.normalize() == normalizedName && !it.isChecked }
                    
                    if (matchShopping != null) {
                        val existingAmountBase = convertAmountToBase(matchShopping.amount, matchShopping.unit)
                        val totalAmountBase = existingAmountBase + missingAmountBase
                        val (newAmount, newUnit) = normalizeBaseToBestUnit(totalAmountBase, matchShopping.unit)
                        ingredientDao.updateIngredient(matchShopping.copy(amount = newAmount, unit = newUnit))
                    } else {
                        val (normalizedAmount, normalizedUnit) = normalizeBaseToBestUnit(missingAmountBase, ri.unit)
                        ingredientDao.insertIngredient(
                            com.example.zlotywidelec.data.local.entity.IngredientEntity(
                                name = ri.name,
                                amount = normalizedAmount,
                                unit = normalizedUnit,
                                tag = "",
                                isInFridge = false
                            )
                        )
                    }
                }
            }

            // Auto-sync
            syncShopping()
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

    private fun convertBaseToUnit(baseAmount: Double, targetUnit: String): Double {
        return when {
            targetUnit.endsWith("kg") -> baseAmount / 1000.0
            targetUnit.endsWith("dag") -> baseAmount / 10.0
            targetUnit.endsWith("g") && !targetUnit.endsWith("dag") && !targetUnit.endsWith("kg") -> baseAmount
            targetUnit.endsWith("ml") -> baseAmount * 1000.0
            targetUnit.endsWith("l") && !targetUnit.endsWith("ml") -> baseAmount
            else -> baseAmount
        }
    }

    private fun String.normalize(): String {
        val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(temp, "")
            .lowercase()
            .trim()
    }
}

/**
 * Factory for creating [ShoppingViewModel] with required dependencies.
 */
class ShoppingViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val syncManager: DriveSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(ingredientDao, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
