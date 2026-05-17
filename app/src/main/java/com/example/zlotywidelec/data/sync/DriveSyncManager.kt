package com.example.zlotywidelec.data.sync

import android.util.Log
import com.example.zlotywidelec.data.local.dao.RecipeWithIngredients
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@Serializable
data class SyncData(
    val recipes: List<RecipeWithIngredients> = emptyList(),
    val shoppingList: List<IngredientEntity> = emptyList(),
    val fridgeList: List<IngredientEntity> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

class DriveSyncManager(private val googleDriveService: GoogleDriveService) {

    private val json = Json { ignoreUnknownKeys = true }
    private val FOLDER_NAME = "ZlotyWidelec_Sync"
    
    enum class Category(val fileName: String) {
        RECIPES("recipes.json"),
        SHOPPING("shopping.json"),
        FRIDGE("fridge.json")
    }

    suspend fun getOrCreateSubFolder(parentFolderId: String, folderName: String): String? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        try {
            val query = "name = '$folderName' and '$parentFolderId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result = driveService.files().list().setQ(query).execute()
            val folder = result.files.firstOrNull()

            if (folder != null) return@withContext folder.id

            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentFolderId)
            }
            val newFolder = driveService.files().create(folderMetadata).setFields("id").execute()
            newFolder.id
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error getting/creating subfolder $folderName", e)
            null
        }
    }

    suspend fun uploadImage(fileName: String, content: ByteArray): String? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        val rootId = getOrCreateRootFolder() ?: return@withContext null
        val imagesFolderId = getOrCreateSubFolder(rootId, "images") ?: return@withContext null

        try {
            val contentStream = ByteArrayContent("image/jpeg", content)
            val query = "name = '$fileName' and '$imagesFolderId' in parents and trashed = false"
            val existingFiles = driveService.files().list().setQ(query).execute().files

            if (existingFiles.isNotEmpty()) {
                driveService.files().update(existingFiles[0].id, null, contentStream).execute()
                existingFiles[0].id
            } else {
                val fileMetadata = File().apply {
                    name = fileName
                    parents = listOf(imagesFolderId)
                }
                val newFile = driveService.files().create(fileMetadata, contentStream).setFields("id").execute()
                newFile.id
            }
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error uploading image $fileName", e)
            null
        }
    }

    suspend fun downloadImage(fileName: String, ownerEmail: String? = null): ByteArray? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        try {
            val rootId = if (ownerEmail == null) {
                getOrCreateRootFolder()
            } else {
                getFriendRootFolder(ownerEmail)
            } ?: return@withContext null
            
            val imagesFolderId = getOrCreateSubFolder(rootId, "images") ?: return@withContext null
            val query = "name = '$fileName' and '$imagesFolderId' in parents and trashed = false"

            val files = driveService.files().list().setQ(query).execute().files
            if (files.isEmpty()) return@withContext null

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(files[0].id).executeMediaAndDownloadTo(outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error downloading image $fileName", e)
            null
        }
    }

    suspend fun getOrCreateRootFolder(): String? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        
        try {
            // Dodajemy 'me' in owners, aby upewnić się, że pobieramy własny folder, 
            // a nie folder udostępniony przez znajomego o tej samej nazwie.
            val query = "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false and 'me' in owners"
            val result = driveService.files().list().setQ(query).execute()
            val folder = result.files.firstOrNull()

            if (folder != null) {
                return@withContext folder.id
            }

            val folderMetadata = File().apply {
                name = FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }
            val newFolder = driveService.files().create(folderMetadata).setFields("id").execute()
            newFolder.id
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error getting/creating folder", e)
            null
        }
    }

    suspend fun getFriendRootFolder(friendEmail: String): String? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        try {
            val query = "name = '$FOLDER_NAME' and '$friendEmail' in owners and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result = driveService.files().list().setQ(query).execute()
            val folder = result.files.firstOrNull()
            Log.d("DriveSyncManager", "Znaleziono folder znajomego $friendEmail: ${folder?.id}")
            folder?.id
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Błąd podczas szukania folderu znajomego $friendEmail", e)
            null
        }
    }

    suspend fun <T> uploadCategoryData(category: Category, data: T, serializer: kotlinx.serialization.KSerializer<T>): Boolean = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext false
        val folderId = getOrCreateRootFolder() ?: return@withContext false

        try {
            val content = json.encodeToString(serializer, data)
            val contentStream = ByteArrayContent.fromString("application/json", content)

            val query = "name = '${category.fileName}' and '$folderId' in parents and trashed = false"
            val existingFiles = driveService.files().list().setQ(query).execute().files

            if (existingFiles.isNotEmpty()) {
                driveService.files().update(existingFiles[0].id, null, contentStream).execute()
            } else {
                val fileMetadata = File().apply {
                    name = category.fileName
                    parents = listOf(folderId)
                }
                driveService.files().create(fileMetadata, contentStream).execute()
            }
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error uploading ${category.fileName}", e)
            false
        }
    }

    suspend fun <T> downloadCategoryData(category: Category, serializer: kotlinx.serialization.KSerializer<T>, friendEmail: String? = null): T? = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext null
        
        try {
            val folderId = if (friendEmail == null) {
                getOrCreateRootFolder()
            } else {
                getFriendRootFolder(friendEmail)
            } ?: return@withContext null

            val query = "name = '${category.fileName}' and '$folderId' in parents and trashed = false"
            
            val existingFiles = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, owners)")
                .execute().files

            if (existingFiles.isEmpty()) return@withContext null

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(existingFiles[0].id).executeMediaAndDownloadTo(outputStream)
            
            json.decodeFromString(serializer, outputStream.toString())
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error downloading ${category.fileName} for $friendEmail", e)
            null
        }
    }

    suspend fun shareWithFriend(email: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = googleDriveService.getDriveService() ?: return@withContext false
        val folderId = getOrCreateRootFolder() ?: return@withContext false

        try {
            val permission = Permission().apply {
                type = "user"
                role = "writer"
                emailAddress = email
            }
            driveService.permissions().create(folderId, permission).execute()
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Error sharing folder", e)
            false
        }
    }
}
