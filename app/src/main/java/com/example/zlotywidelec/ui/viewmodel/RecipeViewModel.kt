package com.example.zlotywidelec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.dao.ShoppingDao
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeDao: RecipeDao,
    private val shoppingDao: ShoppingDao
) : ViewModel() {

    val allRecipes: StateFlow<List<RecipeEntity>> = recipeDao.getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteRecipes: StateFlow<List<RecipeEntity>> = recipeDao.getFavoriteRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecipe(title: String, description: String, ingredients: String, instructions: String, imageUrl: String? = null) {
        viewModelScope.launch {
            recipeDao.insertRecipe(
                RecipeEntity(
                    title = title,
                    description = description,
                    ingredients = ingredients,
                    instructions = instructions,
                    imageUrl = imageUrl
                )
            )
        }
    }

    fun addItemsToShoppingList(items: List<Pair<String, String>>) {
        viewModelScope.launch {
            items.forEach { (name, quantity) ->
                shoppingDao.insertItem(ShoppingItemEntity(name = name, quantity = quantity))
            }
        }
    }

    // Mock data for "Chef's Recipes" until an API is integrated
    val chefsRecipes: List<RecipeEntity> = listOf(
        RecipeEntity(
            title = "Burger z szarpanką",
            description = "Pyszny burger z wolno pieczoną wieprzowiną.",
            ingredients = "Bułka do burgera:1 szt,Karkówka wieprzowa:150 g,Pomidor:1 szt,Ogórek:1 szt,Sałata lodowa:1 szt",
            instructions = "Przygotuj szarpankę według innego przepisu. Trzeba pokroić warzywa. Złóż burgera.",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80"
        ),
        RecipeEntity(
            title = "Tost z jajkiem i boczkiem",
            description = "Klasyczne śniadanie na ciepło.",
            ingredients = "Chleb tostowy:2 plastry,Jajko:1 szt,Boczek:2 plastry,Ser żółty:1 plaster",
            instructions = "Podsmaż boczek. Na tej samej patelni usmaż jajko. Złóż tosta z serem i zapiecz.",
            imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=300&q=80"
        )
    )
}

class RecipeViewModelFactory(
    private val recipeDao: RecipeDao,
    private val shoppingDao: ShoppingDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(recipeDao, shoppingDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
