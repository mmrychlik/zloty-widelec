package com.example.zlotywidelec.ui.viewmodel

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

enum class RecipeSortOrder {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, AVAILABILITY_DESC
}

class RecipeViewModel(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(RecipeSortOrder.NAME_ASC)
    val sortOrder: StateFlow<RecipeSortOrder> = _sortOrder

    private val _filterTags = MutableStateFlow<Set<String>>(emptySet())
    val filterTags: StateFlow<Set<String>> = _filterTags

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

    val filteredUserRecipes = combine(
        recipeDao.getUserRecipes(),
        _searchQuery,
        _sortOrder,
        _filterTags,
        fridgeItems
    ) { recipes, query, sort, tags, fridge ->
        filterAndSortRecipes(recipes, query, sort, tags, fridge)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredChefRecipes = combine(
        recipeDao.getChefRecipes(),
        _searchQuery,
        _sortOrder,
        _filterTags,
        fridgeItems
    ) { recipes, query, sort, tags, fridge ->
        filterAndSortRecipes(recipes, query, sort, tags, fridge)
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
            "AVAILABILITY" -> RecipeSortOrder.AVAILABILITY_DESC
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

    fun addRecipe(name: String, instructions: String, imageUrl: String, tag: String, ingredients: List<Triple<String, Double, String>>) {
        viewModelScope.launch {
            val recipe = RecipeEntity(
                name = name,
                instructions = instructions,
                imageUrl = imageUrl,
                tag = tag,
                isUserCreated = true
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
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            recipeDao.deleteRecipe(recipe)
        }
    }

    fun updateRecipe(recipe: RecipeEntity, ingredients: List<Triple<String, Double, String>>) {
        viewModelScope.launch {
            val ingredientEntities = ingredients.map { (iName, amount, iUnit) ->
                RecipeIngredientEntity(
                    recipeId = recipe.id,
                    name = iName,
                    amount = amount,
                    unit = iUnit
                )
            }
            recipeDao.updateRecipeWithIngredients(recipe, ingredientEntities)
        }
    }

    private fun filterAndSortRecipes(
        recipes: List<RecipeWithIngredients>,
        query: String,
        sort: RecipeSortOrder,
        tags: Set<String>,
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

        return when (sort) {
            RecipeSortOrder.NAME_ASC -> filtered.sortedBy { it.recipe.name }
            RecipeSortOrder.NAME_DESC -> filtered.sortedByDescending { it.recipe.name }
            RecipeSortOrder.DATE_ASC -> filtered.sortedBy { it.recipe.addedAt }
            RecipeSortOrder.DATE_DESC -> filtered.sortedByDescending { it.recipe.addedAt }
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

class RecipeViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(ingredientDao, recipeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
