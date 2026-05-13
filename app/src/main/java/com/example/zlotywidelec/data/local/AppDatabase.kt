package com.example.zlotywidelec.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        IngredientEntity::class,
        com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao

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
            private val context: Context
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
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Mleko", "litr"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Chleb", "bochenek"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Jajka", "szt."),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Boczek", "g"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Mąka", "kg"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Cukier", "kg"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Masło", "szt."),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Woda", "l"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Sól", "g"),
                    com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity("Pieprz", "g")
                )
                initialSuggestions.forEach { dao.insertProductSuggestion(it) }

                // Initial Shopping Items
                dao.insertIngredient(IngredientEntity(name = "Mleko", amount = 1.0, unit = "litr"))
                dao.insertIngredient(IngredientEntity(name = "Chleb", amount = 1.0, unit = "bochenek"))

                // Initial Fridge Items
                dao.insertIngredient(IngredientEntity(name = "Jajka", amount = 10.0, unit = "szt", isInFridge = true))
                dao.insertIngredient(IngredientEntity(name = "Boczek", amount = 200.0, unit = "g", isInFridge = true))
            }
        }
    }
}
