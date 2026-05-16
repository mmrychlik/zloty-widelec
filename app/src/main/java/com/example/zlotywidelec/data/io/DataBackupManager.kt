package com.example.zlotywidelec.data.io

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class AppDataBackup(
    val shoppingList: List<IngredientEntity>? = null,
    val fridgeItems: List<IngredientEntity>? = null,
    val userRecipes: List<RecipeWithIngredientsBackup>? = null
)

@Serializable
data class RecipeWithIngredientsBackup(
    val recipe: RecipeEntity,
    val ingredients: List<RecipeIngredientEntity>,
    val localImageFileName: String? = null
)

class DataBackupManager(
    private val context: Context,
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao
) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun getPhotoStorageUri(): String? {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("photo_storage_uri", null)
    }

    fun setPhotoStorageUri(uri: String) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString("photo_storage_uri", uri).apply()
    }

    fun takePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun copyImageToInternalStorage(sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val storageUriStr = getPhotoStorageUri() ?: return@withContext null
        if (sourceUri.toString().startsWith(storageUriStr)) return@withContext sourceUri

        val storageUri = Uri.parse(storageUriStr)
        try {
            val treeId = DocumentsContract.getTreeDocumentId(storageUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(storageUri, treeId)
            
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val fileUri = DocumentsContract.createDocument(context.contentResolver, parentUri, "image/jpeg", fileName) ?: return@withContext null
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(fileUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportData(
        uri: Uri,
        exportShopping: Boolean,
        exportFridge: Boolean,
        exportRecipes: Boolean
    ) = withContext(Dispatchers.IO) {
        val userRecipes = if (exportRecipes) {
            recipeDao.getAllUserRecipesSync().map { rwI ->
                val fileName = if (rwI.recipe.imageUrl.startsWith("content://")) {
                    Uri.parse(rwI.recipe.imageUrl).lastPathSegment?.let { 
                        if (it.contains(":")) it.substringAfterLast(":") else it
                    } ?: "image_${rwI.recipe.id}.jpg"
                } else null
                RecipeWithIngredientsBackup(rwI.recipe, rwI.ingredients, fileName)
            }
        } else null

        val backup = AppDataBackup(
            shoppingList = if (exportShopping) ingredientDao.getAllShoppingItemsSync() else null,
            fridgeItems = if (exportFridge) ingredientDao.getAllFridgeItemsSync() else null,
            userRecipes = userRecipes
        )

        val jsonString = json.encodeToString(backup)
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zos ->
                zos.putNextEntry(ZipEntry("backup.json"))
                zos.write(jsonString.toByteArray())
                zos.closeEntry()

                if (exportRecipes && userRecipes != null) {
                    userRecipes.forEach { backupRecipe ->
                        if (backupRecipe.localImageFileName != null && backupRecipe.recipe.imageUrl.isNotEmpty()) {
                            try {
                                context.contentResolver.openInputStream(Uri.parse(backupRecipe.recipe.imageUrl))?.use { input ->
                                    zos.putNextEntry(ZipEntry("images/${backupRecipe.localImageFileName}"))
                                    input.copyTo(zos)
                                    zos.closeEntry()
                                }
                            } catch (e: Exception) {
                                // Skip if image cannot be read
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        val storageUriStr = getPhotoStorageUri()
        val storageUri = storageUriStr?.let { Uri.parse(it) }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var backup: AppDataBackup? = null
                val imageMap = mutableMapOf<String, ByteArray>()
                
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "backup.json" -> {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            backup = json.decodeFromString<AppDataBackup>(baos.toString("UTF-8"))
                        }
                        entry.name.startsWith("images/") -> {
                            val fileName = entry.name.removePrefix("images/")
                            if (fileName.isNotEmpty()) {
                                val baos = ByteArrayOutputStream()
                                zis.copyTo(baos)
                                imageMap[fileName] = baos.toByteArray()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }

                if (backup == null) return@withContext

                backup.shoppingList?.let {
                    ingredientDao.insertIngredients(it.map { item -> item.copy(id = 0) })
                }

                backup.fridgeItems?.let {
                    ingredientDao.insertIngredients(it.map { item -> item.copy(id = 0) })
                }

                backup.userRecipes?.let { recipes ->
                    recipes.forEach { backupRecipe ->
                        var finalImageUrl = backupRecipe.recipe.imageUrl
                        if (backupRecipe.localImageFileName != null && storageUri != null) {
                            val imageData = imageMap[backupRecipe.localImageFileName]
                            if (imageData != null) {
                                try {
                                    val treeId = DocumentsContract.getTreeDocumentId(storageUri)
                                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(storageUri, treeId)
                                    val newFileUri = DocumentsContract.createDocument(context.contentResolver, parentUri, "image/jpeg", backupRecipe.localImageFileName)
                                    if (newFileUri != null) {
                                        context.contentResolver.openOutputStream(newFileUri)?.use { it.write(imageData) }
                                        finalImageUrl = newFileUri.toString()
                                    }
                                } catch (e: Exception) {
                                    // Fallback to original URL if copying fails
                                }
                            }
                        }

                        recipeDao.insertRecipeWithIngredients(
                            backupRecipe.recipe.copy(id = 0, imageUrl = finalImageUrl),
                            backupRecipe.ingredients.map { it.copy(id = 0, recipeId = 0) }
                        )
                    }
                }
            }
        }
    }
}
