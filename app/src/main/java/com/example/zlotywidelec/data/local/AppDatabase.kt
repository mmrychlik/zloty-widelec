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
    version = 13,
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

                // Initial Shopping Items
                dao.insertIngredient(IngredientEntity(name = "Mleko", amount = 1.0, unit = "litr", tag = "nabiał"))
                dao.insertIngredient(IngredientEntity(name = "Chleb", amount = 1.0, unit = "bochenek", tag = "pieczywo"))

                // Initial Fridge Items
                dao.insertIngredient(IngredientEntity(name = "Jajka", amount = 10.0, unit = "szt.", tag = "nabiał", isInFridge = true))
                dao.insertIngredient(IngredientEntity(name = "Boczek", amount = 200.0, unit = "g", tag = "mięso", isInFridge = true))

                // Initial Recipes
                val recipeDao = db.recipeDao()
                
                val recipes = listOf(
                    Pair(
                        RecipeEntity(
                            name = "Spaghetti Carbonara",
                            imageUrl = "https://www.giallozafferano.com/images/228-22875/spaghetti-carbonara_1200x800.jpg",
                            tag = "obiad",
                            instructions = "1. Ugotuj makaron al dente w osolonej wodzie.\n2. Na patelni podsmaż pokrojony w kostkę boczek.\n3. W miseczce roztrzep jajka i wymieszaj ze startym serem.\n4. Odcedź makaron, zachowując trochę wody.\n5. Wymieszaj makaron z boczkiem, zdejmij z ognia i dodaj masę jajeczną, energicznie mieszając."
                        ),
                        listOf(
                            RecipeIngredientEntity(recipeId = 0, name = "Makaron spaghetti", amount = 500.0, unit = "g"),
                            RecipeIngredientEntity(recipeId = 0, name = "Boczek", amount = 150.0, unit = "g"),
                            RecipeIngredientEntity(recipeId = 0, name = "Jajka", amount = 4.0, unit = "szt."),
                            RecipeIngredientEntity(recipeId = 0, name = "Ser parmezan", amount = 50.0, unit = "g")
                        )
                    ),
                    Pair(
                        RecipeEntity(
                            name = "Sałatka Grecka",
                            imageUrl = "https://cdn.discordapp.com/attachments/1494610723798782022/1494611247172292608/IMG_20260114_164653887.jpg?ex=6a0826b1&is=6a06d531&hm=c6cc975874e6d09cf12da1086fa9ea11e85165b0cf5755278b5e984cd8220622&",
                            tag = "sałatka",
                            instructions = "1. Pomidory i ogórka pokrój w dużą kostkę.\n2. Cebulę pokrój w piórka.\n3. Warzywa przełóż do miski, dodaj oliwki.\n4. Na wierzchu ułóż plastry sera feta.\n5. Całość polej oliwą z oliwek i posyp suszonym oregano."
                        ),
                        listOf(
                            RecipeIngredientEntity(recipeId = 0, name = "Pomidory", amount = 3.0, unit = "szt."),
                            RecipeIngredientEntity(recipeId = 0, name = "Ogórek", amount = 1.0, unit = "szt."),
                            RecipeIngredientEntity(recipeId = 0, name = "Ser feta", amount = 200.0, unit = "g"),
                            RecipeIngredientEntity(recipeId = 0, name = "Oliwki", amount = 50.0, unit = "g"),
                            RecipeIngredientEntity(recipeId = 0, name = "Cebula czerwona", amount = 0.5, unit = "szt.")
                        )
                    ),
                    Pair(
                        RecipeEntity(
                            name = "Naleśniki",
                            imageUrl = "https://static.fajnegotowanie.pl/media/uploads/potrawy/84/nalesniki-z-dzemem.jpg",
                            tag = "obiad",
                            instructions = "1. Wszystkie składniki umieść w misce.\n2. Zmiksuj na gładką masę bez grudek.\n3. Smaż cienkie naleśniki na dobrze rozgrzanej patelni z obu stron na złoty kolor."
                        ),
                        listOf(
                            RecipeIngredientEntity(recipeId = 0, name = "Mleko", amount = 500.0, unit = "ml"),
                            RecipeIngredientEntity(recipeId = 0, name = "Mąka", amount = 250.0, unit = "g"),
                            RecipeIngredientEntity(recipeId = 0, name = "Jajka", amount = 2.0, unit = "szt."),
                            RecipeIngredientEntity(recipeId = 0, name = "Olej", amount = 2.0, unit = "łyżki")
                        )
                    )
                )

                recipes.forEach { (recipe, ingredients) ->
                    recipeDao.insertRecipeWithIngredients(recipe, ingredients)
                }
            }
        }
    }
}
