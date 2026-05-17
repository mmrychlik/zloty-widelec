package com.example.zlotywidelec.data.local.dao

import androidx.room.*
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<RecipeIngredientEntity>
)

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isUserCreated = 1 ORDER BY addedAt DESC")
    fun getUserRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isUserCreated = 0 ORDER BY name ASC")
    fun getFriendsRecipes(): Flow<List<RecipeWithIngredients>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Transaction
    suspend fun insertRecipeWithIngredients(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>) {
        val recipeId = insertRecipe(recipe)
        val ingredientsWithId = ingredients.map { it.copy(recipeId = recipeId) }
        insertIngredients(ingredientsWithId)
    }

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE isUserCreated = 1")
    suspend fun deleteAllUserRecipes()

    @Query("SELECT * FROM recipes WHERE uuid = :uuid LIMIT 1")
    suspend fun getRecipeByUuid(uuid: String): RecipeEntity?

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: Long)

    @Transaction
    @Query("SELECT * FROM recipes WHERE isUserCreated = 1")
    suspend fun getAllUserRecipesSync(): List<RecipeWithIngredients>

    @Transaction
    suspend fun updateRecipeWithIngredients(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>) {
        insertRecipe(recipe) // Replaces if same ID due to REPLACE strategy
        deleteIngredientsByRecipeId(recipe.id)
        val ingredientsWithId = ingredients.map { it.copy(recipeId = recipe.id) }
        insertIngredients(ingredientsWithId)
    }
}
