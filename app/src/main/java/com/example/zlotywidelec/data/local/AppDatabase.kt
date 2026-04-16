package com.example.zlotywidelec.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zlotywidelec.data.local.dao.FridgeDao
import com.example.zlotywidelec.data.local.dao.RecipeDao
import com.example.zlotywidelec.data.local.dao.ShoppingDao
import com.example.zlotywidelec.data.local.entity.FridgeItemEntity
import com.example.zlotywidelec.data.local.entity.RecipeEntity
import com.example.zlotywidelec.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShoppingItemEntity::class,
        RecipeEntity::class,
        FridgeItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
    abstract fun recipeDao(): RecipeDao
    abstract fun fridgeDao(): FridgeDao

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

            suspend fun populateDatabase(db: AppDatabase) {
                // Initial Shopping Items
                db.shoppingDao().insertItem(ShoppingItemEntity(name = "Mleko", quantity = "1 litr"))
                db.shoppingDao().insertItem(ShoppingItemEntity(name = "Chleb", quantity = "1 bochenek"))

                // Initial Fridge Items
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Jajka", quantity = "10 szt"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Boczek", quantity = "200 g"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Ser żółty", quantity = "150 g"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Pomidor", quantity = "2 szt"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Makaron", quantity = "500 g"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Pierś z kurczaka", quantity = "400 g"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Śmietana", quantity = "200 ml"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Cebula", quantity = "3 szt"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Czosnek", quantity = "1 główka"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Ryż", quantity = "1 kg"))
                db.fridgeDao().insertItem(FridgeItemEntity(name = "Curry", quantity = "1 opakowanie"))

                // Initial Recipes
                db.recipeDao().insertRecipe(
                    RecipeEntity(
                        title = "Burger z szarpanką",
                        description = "Pyszny burger z wolno pieczoną wieprzowiną.",
                        ingredients = "Bułka do burgera:1 szt,Karkówka wieprzowa:150 g,Pomidor:1 szt,Ogórek:1 szt,Sałata lodowa:1 szt",
                        instructions = "Przygotuj szarpankę według innego przepisu. Trzeba pokroić warzywa. Złóż burgera.",
                        imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80"
                    )
                )
                db.recipeDao().insertRecipe(
                    RecipeEntity(
                        title = "Tost z jajkami i boczkiem",
                        description = "Klasyczne śniadanie na ciepło.",
                        ingredients = "Chleb tostowy:2 plastry,Jajka:1 szt,Boczek:2 plastry,Ser żółty:1 plaster",
                        instructions = "Podsmaż boczek. Na tej samej patelni usmaż jajko. Złóż tosta z serem i zapiecz.",
                        imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=300&q=80"
                    )
                )
                db.recipeDao().insertRecipe(
                    RecipeEntity(
                        title = "Spaghetti Carbonara",
                        description = "Klasyczne włoskie danie z boczkiem i jajkami.",
                        ingredients = "Makaron:200 g,Boczek:100 g,Jajka:2 szt,Ser żółty:50 g,Czosnek:1 ząbek",
                        instructions = "Ugotuj makaron. Podsmaż boczek z czosnkiem. Wymieszaj jajka z serem. Połącz wszystko z gorącym makaronem poza ogniem.",
                        imageUrl = "https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=300&q=80"
                    )
                )
                db.recipeDao().insertRecipe(
                    RecipeEntity(
                        title = "Kurczak w sosie śmietanowym",
                        description = "Szybki obiad z kurczakiem i śmietaną.",
                        ingredients = "Pierś z kurczaka:200 g,Śmietana:100 ml,Cebula:0.5 szt,Czosnek:1 ząbek,Makaron:150 g",
                        instructions = "Pokrój kurczaka i podsmaż z cebulą i czosnkiem. Dodaj śmietanę i gotuj aż sos zgęstnieje. Podawaj z makaronem.",
                        imageUrl = "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=300&q=80"
                    )
                )
                db.recipeDao().insertRecipe(
                    RecipeEntity(
                        title = "Curry z kurczakiem",
                        description = "Aromatyczne curry z ryżem.",
                        ingredients = "Pierś z kurczaka:200 g,Ryż:100 g,Cebula:0.5 szt,Curry:1 łyżka,Mleko:50 ml",
                        instructions = "Ugotuj ryż. Podsmaż kurczaka z cebulą i przyprawą curry. Dodaj odrobinę mleka (lub mleczka kokosowego) i duś chwilę. Podawaj z ryżem.",
                        imageUrl = "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=300&q=80"
                    )
                )
            }
        }
    }
}
