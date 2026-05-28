package com.example.zlotywidelec.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.dao.RecipeWithIngredients
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enum defining sort orders for recipes.
 */
enum class RecipeSortOrder {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, AVAILABILITY_ASC, AVAILABILITY_DESC
}

/**
 * ViewModel for managing recipes.
 * Handles recipe CRUD operations, filtering, sorting, and availability calculation based on fridge contents.
 */
class RecipeViewModel(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val backupManager: com.example.zlotywidelec.data.io.DataBackupManager,
    private val syncManager: com.example.zlotywidelec.data.sync.DriveSyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(RecipeSortOrder.NAME_ASC)
    val sortOrder: StateFlow<RecipeSortOrder> = _sortOrder

    private val _filterTags = MutableStateFlow<Set<String>>(emptySet())
    val filterTags: StateFlow<Set<String>> = _filterTags

    private val _filterOwner = MutableStateFlow<String?>(null) // null = Wszystkie, "WŁASNE" = Moje, else = Friend name
    val filterOwner: StateFlow<String?> = _filterOwner

    val availableOwners = recipeDao.getAllRecipes().map { recipes ->
        recipes.filter { !it.recipe.isUserCreated && it.recipe.ownerName.isNotBlank() }
            .map { it.recipe.ownerName }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fridgeItems = ingredientDao.getFridgeItems().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val allRecipes = recipeDao.getAllRecipes()
    
    val suggestions = combine(
        ingredientDao.getAllUniqueIngredients(),
        ingredientDao.getAllProductSuggestions()
    ) { unique, suggested ->
        (unique + suggested).distinctBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFilteredRecipes = combine(
        recipeDao.getAllRecipes(),
        _searchQuery,
        _sortOrder,
        _filterTags,
        _filterOwner,
        fridgeItems
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        filterAndSortRecipes(
            args[0] as List<RecipeWithIngredients>,
            args[1] as String,
            args[2] as RecipeSortOrder,
            args[3] as Set<String>,
            args[4] as String?,
            args[5] as List<IngredientEntity>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredUserRecipes = combine(
        recipeDao.getUserRecipes(),
        _searchQuery,
        _sortOrder,
        _filterTags,
        fridgeItems
    ) { recipes, query, sort, tags, fridge ->
        filterAndSortRecipes(recipes, query, sort, tags, "WŁASNE", fridge)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFriendsRecipes = combine(
        recipeDao.getFriendsRecipes(),
        _searchQuery,
        _sortOrder,
        _filterTags,
        fridgeItems
    ) { recipes, query, sort, tags, fridge ->
        filterAndSortRecipes(recipes, query, sort, tags, null, fridge)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipeAvailability = combine(allRecipes, fridgeItems) { recipes, fridge ->
        recipes.associate { rwI ->
            rwI.recipe.id.toString() to calculateAvailability(rwI, fridge)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: RecipeSortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortOrder(type: String) {
        _sortOrder.value = when (type) {
            "NAME" -> if (_sortOrder.value == RecipeSortOrder.NAME_ASC) RecipeSortOrder.NAME_DESC else RecipeSortOrder.NAME_ASC
            "DATE" -> if (_sortOrder.value == RecipeSortOrder.DATE_ASC) RecipeSortOrder.DATE_DESC else RecipeSortOrder.DATE_ASC
            "AVAILABILITY" -> if (_sortOrder.value == RecipeSortOrder.AVAILABILITY_DESC) RecipeSortOrder.AVAILABILITY_ASC else RecipeSortOrder.AVAILABILITY_DESC
            else -> _sortOrder.value
        }
    }

    fun toggleFilterTag(tag: String) {
        if (tag.isEmpty()) {
            _filterTags.value = emptySet()
            return
        }
        val current = _filterTags.value
        if (current.contains(tag)) {
            _filterTags.value = current - tag
        } else {
            _filterTags.value = current + tag
        }
    }

    fun setFilterOwner(owner: String?) {
        _filterOwner.value = owner
    }

    fun getFridgeAmountForIngredient(reqName: String, reqUnit: String): Double {
        val fridge = fridgeItems.value
        val normalizedReqName = reqName.normalize()
        
        val matches = fridge.filter { it.name.normalize() == normalizedReqName }
        if (matches.isEmpty()) return 0.0

        val totalAmountBase = matches.sumOf { convertAmountToBase(it.amount, it.unit) }
        return convertBaseToUnit(totalAmountBase, reqUnit)
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

    fun addRecipe(name: String, instructions: String, imageUrl: String, videoUrl: String, tag: String, ingredients: List<Triple<String, Double, String>>) {
        viewModelScope.launch {
            val finalImageUrl = if (imageUrl.startsWith("content://")) {
                backupManager.copyImageToInternalStorage(Uri.parse(imageUrl))?.toString() ?: imageUrl
            } else {
                imageUrl
            }
            val finalVideoUrl = if (videoUrl.startsWith("content://")) {
                backupManager.copyVideoToInternalStorage(Uri.parse(videoUrl))?.toString() ?: videoUrl
            } else {
                videoUrl
            }
            val recipe = RecipeEntity(
                name = name,
                instructions = instructions,
                imageUrl = finalImageUrl,
                videoUrl = finalVideoUrl,
                tag = tag,
                isUserCreated = true,
                lastUpdated = System.currentTimeMillis()
            )
            val ingredientEntities = ingredients.map { (iName, amount, iUnit) ->
                RecipeIngredientEntity(
                    recipeId = 0,
                    name = iName,
                    amount = amount,
                    unit = iUnit
                )
            }
            recipeDao.insertRecipeWithIngredients(recipe, ingredientEntities)
            
            // Auto-sync after adding
            try {
                // 1. Upload image if it's local
                if (finalImageUrl.startsWith("content://")) {
                    val uri = Uri.parse(finalImageUrl)
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "img_${recipe.uuid}.jpg"
                    backupManager.context.contentResolver.openInputStream(uri)?.use { input ->
                        syncManager.uploadImage(fileName, input.readBytes())
                    }
                }

                // 2. Upload video if it's local
                if (finalVideoUrl.startsWith("content://")) {
                    val uri = Uri.parse(finalVideoUrl)
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "vid_${recipe.uuid}.mp4"
                    backupManager.context.contentResolver.openInputStream(uri)?.use { input ->
                        syncManager.uploadFile(fileName, input.readBytes(), "video/mp4")
                    }
                }

                // 3. Upload recipe list
                val allUserRecipes = recipeDao.getAllUserRecipesSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.RECIPES,
                    allUserRecipes,
                    kotlinx.serialization.builtins.ListSerializer(RecipeWithIngredients.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            recipeDao.deleteRecipe(recipe)
            
            // Auto-sync after deleting
            try {
                val allUserRecipes = recipeDao.getAllUserRecipesSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.RECIPES,
                    allUserRecipes,
                    kotlinx.serialization.builtins.ListSerializer(RecipeWithIngredients.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRecipe(recipe: RecipeEntity, ingredients: List<Triple<String, Double, String>>) {
        viewModelScope.launch {
            val finalImageUrl = if (recipe.imageUrl.startsWith("content://")) {
                backupManager.copyImageToInternalStorage(Uri.parse(recipe.imageUrl))?.toString() ?: recipe.imageUrl
            } else {
                recipe.imageUrl
            }
            val finalVideoUrl = if (recipe.videoUrl.startsWith("content://")) {
                backupManager.copyVideoToInternalStorage(Uri.parse(recipe.videoUrl))?.toString() ?: recipe.videoUrl
            } else {
                recipe.videoUrl
            }
            val updatedRecipe = recipe.copy(
                imageUrl = finalImageUrl,
                videoUrl = finalVideoUrl,
                lastUpdated = System.currentTimeMillis()
            )
            val ingredientEntities = ingredients.map { (iName, amount, iUnit) ->
                RecipeIngredientEntity(
                    recipeId = recipe.id,
                    name = iName,
                    amount = amount,
                    unit = iUnit
                )
            }
            recipeDao.updateRecipeWithIngredients(updatedRecipe, ingredientEntities)

            // Auto-sync after updating
            try {
                // 1. Upload image if it's local
                if (finalImageUrl.startsWith("content://")) {
                    val uri = Uri.parse(finalImageUrl)
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "img_${updatedRecipe.uuid}.jpg"
                    backupManager.context.contentResolver.openInputStream(uri)?.use { input ->
                        syncManager.uploadImage(fileName, input.readBytes())
                    }
                }

                // 2. Upload video if it's local
                if (finalVideoUrl.startsWith("content://")) {
                    val uri = Uri.parse(finalVideoUrl)
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "vid_${updatedRecipe.uuid}.mp4"
                    backupManager.context.contentResolver.openInputStream(uri)?.use { input ->
                        syncManager.uploadFile(fileName, input.readBytes(), "video/mp4")
                    }
                }

                // 3. Upload recipe list
                val allUserRecipes = recipeDao.getAllUserRecipesSync()
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.RECIPES,
                    allUserRecipes,
                    kotlinx.serialization.builtins.ListSerializer(RecipeWithIngredients.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun useRecipeIngredients(ingredients: List<RecipeIngredientEntity>) {
        viewModelScope.launch {
            val currentFridge = ingredientDao.getAllFridgeItemsSync()
            ingredients.forEach { req ->
                val normalizedReqName = req.name.normalize()
                var remainingToSubtract = convertAmountToBase(req.amount, req.unit)
                
                // Find all fridge items that match the name
                val matches = currentFridge.filter { it.name.normalize() == normalizedReqName }
                
                for (item in matches) {
                    if (remainingToSubtract <= 0) break
                    
                    val itemAmountBase = convertAmountToBase(item.amount, item.unit)
                    
                    if (itemAmountBase <= remainingToSubtract + 0.001) { // Add small epsilon for floating point
                        // Use up this item completely
                        remainingToSubtract -= itemAmountBase
                        ingredientDao.deleteIngredient(item)
                    } else {
                        // Use part of this item
                        val newItemAmountBase = itemAmountBase - remainingToSubtract
                        val (newAmount, newUnit) = normalizeBaseToBestUnit(newItemAmountBase, item.unit)
                        ingredientDao.updateIngredient(item.copy(amount = newAmount, unit = newUnit))
                        remainingToSubtract = 0.0
                    }
                }
            }
            
            // Auto-sync after updating fridge
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

    private fun filterAndSortRecipes(
        recipes: List<RecipeWithIngredients>,
        query: String,
        sort: RecipeSortOrder,
        tags: Set<String>,
        ownerFilter: String?,
        fridge: List<IngredientEntity>
    ): List<RecipeWithIngredients> {
        var filtered = if (query.isBlank()) {
            recipes
        } else {
            recipes.filter { it.recipe.name.contains(query, ignoreCase = true) }
        }

        if (tags.isNotEmpty()) {
            filtered = filtered.filter { tags.contains(it.recipe.tag) }
        }

        if (ownerFilter != null) {
            filtered = if (ownerFilter == "WŁASNE") {
                filtered.filter { it.recipe.isUserCreated }
            } else {
                filtered.filter { !it.recipe.isUserCreated && it.recipe.ownerName == ownerFilter }
            }
        }

        return when (sort) {
            RecipeSortOrder.NAME_ASC -> filtered.sortedBy { it.recipe.name }
            RecipeSortOrder.NAME_DESC -> filtered.sortedByDescending { it.recipe.name }
            RecipeSortOrder.DATE_ASC -> filtered.sortedBy { it.recipe.addedAt }
            RecipeSortOrder.DATE_DESC -> filtered.sortedByDescending { it.recipe.addedAt }
            RecipeSortOrder.AVAILABILITY_ASC -> filtered.sortedBy { calculateAvailability(it, fridge) }
            RecipeSortOrder.AVAILABILITY_DESC -> filtered.sortedByDescending { calculateAvailability(it, fridge) }
        }
    }

    private fun calculateAvailability(rwI: RecipeWithIngredients, fridge: List<IngredientEntity>): Int {
        if (rwI.ingredients.isEmpty()) return 100
        var totalPercentage = 0.0

        for (req in rwI.ingredients) {
            val normalizedReqName = req.name.normalize()
            
            // Find all matching items in fridge by name
            val matches = fridge.filter { it.name.normalize() == normalizedReqName }
            
            if (matches.isNotEmpty()) {
                val totalAmountInFridge = matches.sumOf { 
                    convertAmountToBase(it.amount, it.unit)
                }
                val requiredAmountBase = convertAmountToBase(req.amount, req.unit)
                
                if (requiredAmountBase > 0) {
                    val ratio = totalAmountInFridge / requiredAmountBase
                    totalPercentage += ratio.coerceAtMost(1.0)
                } else {
                    totalPercentage += 1.0
                }
            }
        }
        return (totalPercentage * 100 / rwI.ingredients.size).toInt()
    }

    private fun convertAmountToBase(amount: Double, unit: String): Double {
        return when {
            unit.endsWith("kg") -> amount * 1000.0
            unit.endsWith("dag") -> amount * 10.0
            unit.endsWith("g") && !unit.endsWith("dag") && !unit.endsWith("kg") -> amount
            unit.endsWith("ml") -> amount / 1000.0
            unit.endsWith("l") && !unit.endsWith("ml") -> amount
            else -> amount // For "szt." or unknown units
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
 * Factory for creating [RecipeViewModel] with required dependencies.
 */
class RecipeViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val backupManager: com.example.zlotywidelec.data.io.DataBackupManager,
    private val syncManager: com.example.zlotywidelec.data.sync.DriveSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(ingredientDao, recipeDao, backupManager, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
