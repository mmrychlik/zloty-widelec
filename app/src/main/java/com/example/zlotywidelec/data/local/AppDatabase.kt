package com.example.zlotywidelec.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        IngredientEntity::class,
        ProductSuggestionEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zloty_widelec_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }

            suspend fun populateDatabase(db: AppDatabase) {
                val dao = db.ingredientDao()
                
                // Initial Suggestions
                val initialSuggestions = listOf(
                    ProductSuggestionEntity("Mleko", "litr", "nabiał"),
                    ProductSuggestionEntity("Chleb", "bochenek", "pieczywo"),
                    ProductSuggestionEntity("Jajka", "szt.", "nabiał"),
                    ProductSuggestionEntity("Boczek", "g", "mięso"),
                    ProductSuggestionEntity("Mąka", "kg", "zbożowe"),
                    ProductSuggestionEntity("Cukier", "kg", "przyprawy"),
                    ProductSuggestionEntity("Masło", "szt.", "nabiał"),
                    ProductSuggestionEntity("Woda", "l", ""),
                    ProductSuggestionEntity("Sól", "g", "przyprawy"),
                    ProductSuggestionEntity("Pieprz", "g", "przyprawy")
                )
                initialSuggestions.forEach { dao.insertProductSuggestion(it) }
            }
        }
    }
}
