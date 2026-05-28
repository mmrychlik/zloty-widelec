package com.example.zlotywidelec.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zlotywidelec.data.io.DataBackupManager
import com.example.zlotywidelec.data.local.dao.FriendDao
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.entity.FriendEntity
import com.example.zlotywidelec.data.sync.DriveSyncManager
import com.example.zlotywidelec.data.sync.SyncData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing application settings and data synchronization.
 * Handles dark mode, Google Drive synchronization, data backup (export/import), and clearing local data.
 */
class SettingsViewModel(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val friendDao: FriendDao,
    private val backupManager: DataBackupManager,
    val googleDriveService: com.example.zlotywidelec.data.sync.GoogleDriveService,
    private val syncManager: DriveSyncManager
) : ViewModel() {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _photoStorageUri = MutableStateFlow(backupManager.getPhotoStorageUri())
    val photoStorageUri: StateFlow<String?> = _photoStorageUri.asStateFlow()

    val userAccount = googleDriveService.userAccount

    val friends = friendDao.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pendingExportSelection: Triple<Boolean, Boolean, Boolean>? = null

    fun updateGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        googleDriveService.updateAccount(account)
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun signOutGoogle() {
        googleDriveService.signOut {
            _message.value = "Wylogowano z Google"
        }
    }

    private val _syncRecipes = MutableStateFlow(true)
    val syncRecipes: StateFlow<Boolean> = _syncRecipes.asStateFlow()

    private val _syncShopping = MutableStateFlow(true)
    val syncShopping: StateFlow<Boolean> = _syncShopping.asStateFlow()

    private val _syncFridge = MutableStateFlow(true)
    val syncFridge: StateFlow<Boolean> = _syncFridge.asStateFlow()

    fun toggleSyncRecipes(enabled: Boolean) { _syncRecipes.value = enabled }
    fun toggleSyncShopping(enabled: Boolean) { _syncShopping.value = enabled }
    fun toggleSyncFridge(enabled: Boolean) { _syncFridge.value = enabled }

    fun updateFriendSyncSettings(friend: FriendEntity, recipes: Boolean, fridge: Boolean, shopping: Boolean) {
        viewModelScope.launch {
            friendDao.updateFriend(friend.copy(
                syncRecipes = recipes,
                syncFridge = fridge,
                syncShopping = shopping
            ))
        }
    }

    fun deleteFriend(friend: FriendEntity) {
        viewModelScope.launch {
            friendDao.deleteFriend(friend)
        }
    }

    fun syncWithGoogleDrive(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            _message.value = "Synchronizacja..."
            try {
                var recipesSuccess = true
                var shoppingSuccess = true
                var fridgeSuccess = true
                var totalImportedCount = 0

                // 1. Own Data Sync
                // 1.1 Sync Recipes
                if (_syncRecipes.value) {
                    val remoteRecipes = syncManager.downloadCategoryData(
                        DriveSyncManager.Category.RECIPES,
                        kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.dao.RecipeWithIngredients.serializer())
                    ) ?: emptyList()

                    remoteRecipes.forEach { remote ->
                        val existing = recipeDao.getRecipeByUuid(remote.recipe.uuid)
                        if (existing == null) {
                            recipeDao.insertRecipeWithIngredients(
                                remote.recipe.copy(id = 0, isUserCreated = true),
                                remote.ingredients
                            )
                        } else if (remote.recipe.lastUpdated > existing.lastUpdated) {
                            // Update existing recipe if remote is newer
                            recipeDao.updateRecipeWithIngredients(
                                remote.recipe.copy(id = existing.id, isUserCreated = true),
                                remote.ingredients
                            )
                        }
                    }
                    val finalRecipes = recipeDao.getAllUserRecipesSync()
                    
                    // Upload images and videos for user recipes
                    finalRecipes.forEach { rwI ->
                        if (rwI.recipe.imageUrl.startsWith("content://")) {
                            try {
                                val uri = Uri.parse(rwI.recipe.imageUrl)
                                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "img_${rwI.recipe.uuid}.jpg"
                                googleDriveService.context.contentResolver.openInputStream(uri)?.use { input ->
                                    syncManager.uploadImage(fileName, input.readBytes())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        if (rwI.recipe.videoUrl.startsWith("content://")) {
                            try {
                                val uri = Uri.parse(rwI.recipe.videoUrl)
                                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "vid_${rwI.recipe.uuid}.mp4"
                                googleDriveService.context.contentResolver.openInputStream(uri)?.use { input ->
                                    syncManager.uploadFile(fileName, input.readBytes(), "video/mp4")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    recipesSuccess = syncManager.uploadCategoryData(DriveSyncManager.Category.RECIPES, finalRecipes, kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.dao.RecipeWithIngredients.serializer()))
                }
                
                // 1.2 Sync Shopping List
                if (_syncShopping.value) {
                    val remoteShopping = syncManager.downloadCategoryData(
                        DriveSyncManager.Category.SHOPPING,
                        kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer())
                    ) ?: emptyList()

                    remoteShopping.forEach { remote ->
                        val existing = ingredientDao.getIngredientByUuid(remote.uuid)
                        if (existing == null) {
                            ingredientDao.insertIngredient(remote.copy(id = 0))
                        }
                    }
                    val finalShopping = ingredientDao.getAllShoppingItemsSync()
                    shoppingSuccess = syncManager.uploadCategoryData(DriveSyncManager.Category.SHOPPING, finalShopping, kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer()))
                }

                // 1.3 Sync Fridge
                if (_syncFridge.value) {
                    val remoteFridge = syncManager.downloadCategoryData(
                        DriveSyncManager.Category.FRIDGE,
                        kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer())
                    ) ?: emptyList()

                    remoteFridge.forEach { remote ->
                        val existing = ingredientDao.getIngredientByUuid(remote.uuid)
                        if (existing == null) {
                            ingredientDao.insertIngredient(remote.copy(id = 0))
                        }
                    }
                    val finalFridge = ingredientDao.getAllFridgeItemsSync()
                    fridgeSuccess = syncManager.uploadCategoryData(DriveSyncManager.Category.FRIDGE, finalFridge, kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer()))
                }

                // 2. Friends Data Sync
                val friendsList = friendDao.getFriendsList()
                for (friend in friendsList) {
                    // Sync Friend's Recipes
                    if (friend.syncRecipes) {
                        val friendRecipes = syncManager.downloadCategoryData(
                            DriveSyncManager.Category.RECIPES,
                            kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.dao.RecipeWithIngredients.serializer()),
                            friendEmail = friend.email
                        ) ?: emptyList()

                        friendRecipes.forEach { remote ->
                            val existing = recipeDao.getRecipeByUuid(remote.recipe.uuid)
                            
                            if (existing == null || remote.recipe.lastUpdated > existing.lastUpdated) {
                                var finalImageUrl = remote.recipe.imageUrl
                                var finalVideoUrl = remote.recipe.videoUrl

                                // If recipe has image, try to download it from friend's Drive
                                if (finalImageUrl.startsWith("content://")) {
                                    val fileName = Uri.parse(finalImageUrl).lastPathSegment?.substringAfterLast("/") ?: "img_${remote.recipe.uuid}.jpg"
                                    val imageData = syncManager.downloadImage(fileName, friend.email)
                                    if (imageData != null) {
                                        val localUri = backupManager.saveImageFromBytes(imageData, fileName)
                                        if (localUri != null) {
                                            finalImageUrl = localUri.toString()
                                        }
                                    }
                                }

                                // If recipe has video, try to download it from friend's Drive
                                if (finalVideoUrl.startsWith("content://")) {
                                    val fileName = Uri.parse(finalVideoUrl).lastPathSegment?.substringAfterLast("/") ?: "vid_${remote.recipe.uuid}.mp4"
                                    val videoData = syncManager.downloadFile(fileName, "video/mp4", friend.email)
                                    if (videoData != null) {
                                        val localUri = backupManager.saveVideoFromBytes(videoData, fileName)
                                        if (localUri != null) {
                                            finalVideoUrl = localUri.toString()
                                        }
                                    }
                                }

                                if (existing == null) {
                                    recipeDao.insertRecipeWithIngredients(
                                        remote.recipe.copy(
                                            id = 0,
                                            isUserCreated = false,
                                            ownerName = friend.name.ifBlank { friend.email },
                                            imageUrl = finalImageUrl,
                                            videoUrl = finalVideoUrl
                                        ),
                                        remote.ingredients
                                    )
                                    totalImportedCount++
                                } else {
                                    recipeDao.updateRecipeWithIngredients(
                                        remote.recipe.copy(
                                            id = existing.id,
                                            isUserCreated = false,
                                            ownerName = friend.name.ifBlank { friend.email },
                                            imageUrl = finalImageUrl,
                                            videoUrl = finalVideoUrl
                                        ),
                                        remote.ingredients
                                    )
                                }
                            }
                        }
                    }

                    // Sync Friend's Shopping (Merge into user's list? Or just for view? 
                    // Based on requirements, it seems like we might want to see them. 
                    // But for now, let's stick to Recipes as they are integrated into main screen.)
                    // (Fridge and Shopping are more personal, usually friends don't merge those, but settings exist per-friend)
                    // If friend.syncFridge is true, we could download and show, but where? 
                    // The prompt mentions "Friends screen into a dedicated management hub for per-friend synchronization settings"
                    // and "Integrate friends' recipes into the main Recipes screen".
                    
                    if (friend.syncShopping) {
                        val friendShopping = syncManager.downloadCategoryData(
                            DriveSyncManager.Category.SHOPPING,
                            kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer()),
                            friendEmail = friend.email
                        ) ?: emptyList()
                        
                        friendShopping.forEach { remote ->
                            // For shopping/fridge, maybe we don't auto-import them to the main list yet 
                            // unless they are explicitly shared/merged. 
                            // But for now, let's focus on recipes as requested.
                        }
                    }

                    friendDao.updateFriend(friend.copy(lastSync = System.currentTimeMillis()))
                }

                if (recipesSuccess && shoppingSuccess && fridgeSuccess) {
                    _message.value = if (totalImportedCount > 0) {
                        "Synchronizacja zakończona. Pobrano $totalImportedCount nowych przepisów."
                    } else {
                        "Synchronizacja zakończona"
                    }
                    onResult?.invoke(totalImportedCount)
                } else {
                    _message.value = "Błąd synchronizacji"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = "Błąd: ${e.message}"
            }
        }
    }

    fun shareWithFriend(email: String) {
        viewModelScope.launch {
            val success = syncManager.shareWithFriend(email)
            if (success) {
                friendDao.insertFriend(FriendEntity(email = email))
                _message.value = "Udostępniono $email"
            } else {
                _message.value = "Błąd udostępniania"
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun clearShoppingList() {
        viewModelScope.launch {
            ingredientDao.deleteAllShoppingItems()
            // Sync empty list
            try {
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.SHOPPING,
                    emptyList<com.example.zlotywidelec.data.local.entity.IngredientEntity>(),
                    kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearFridge() {
        viewModelScope.launch {
            ingredientDao.deleteAllFridgeItems()
            // Sync empty list
            try {
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.FRIDGE,
                    emptyList<com.example.zlotywidelec.data.local.entity.IngredientEntity>(),
                    kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.entity.IngredientEntity.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearRecipes() {
        viewModelScope.launch {
            recipeDao.deleteAllUserRecipes()
            // Sync empty list
            try {
                syncManager.uploadCategoryData(
                    com.example.zlotywidelec.data.sync.DriveSyncManager.Category.RECIPES,
                    emptyList<com.example.zlotywidelec.data.local.dao.RecipeWithIngredients>(),
                    kotlinx.serialization.builtins.ListSerializer(com.example.zlotywidelec.data.local.dao.RecipeWithIngredients.serializer())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearProductSuggestions() {
        viewModelScope.launch {
            ingredientDao.deleteAllProductSuggestions()
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

/**
 * Factory for creating [SettingsViewModel] with required dependencies.
 */
class SettingsViewModelFactory(
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val friendDao: FriendDao,
    private val backupManager: DataBackupManager,
    private val googleDriveService: com.example.zlotywidelec.data.sync.GoogleDriveService,
    private val syncManager: DriveSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(ingredientDao, recipeDao, friendDao, backupManager, googleDriveService, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
