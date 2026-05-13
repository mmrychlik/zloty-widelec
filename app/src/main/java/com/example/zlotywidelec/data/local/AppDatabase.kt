package com.example.zlotywidelec.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zlotywidelec.data.local.dao.IngredientDao
import com.example.zlotywidelec.data.local.entity.IngredientEntity
import com.example.zlotywidelec.data.local.entity.ProductSuggestionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        IngredientEntity::class,
        ProductSuggestionEntity::class
    ],
    version = 12,
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

                // Initial Shopping Items
                dao.insertIngredient(IngredientEntity(name = "Mleko", amount = 1.0, unit = "litr", tag = "nabiał"))
                dao.insertIngredient(IngredientEntity(name = "Chleb", amount = 1.0, unit = "bochenek", tag = "pieczywo"))

                // Initial Fridge Items
                dao.insertIngredient(IngredientEntity(name = "Jajka", amount = 10.0, unit = "szt", tag = "nabiał", isInFridge = true))
                dao.insertIngredient(IngredientEntity(name = "Boczek", amount = 200.0, unit = "g", tag = "mięso", isInFridge = true))
            }
        }
    }
}
