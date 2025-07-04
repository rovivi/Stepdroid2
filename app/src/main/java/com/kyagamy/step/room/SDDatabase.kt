package com.kyagamy.step.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kyagamy.step.room.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Song::class, Category::class, Level::class],
    version = 14    ,
    exportSchema = false
)
 abstract class SDDatabase : RoomDatabase() {


    var nameDatabaseqaaaa="sd_database"
    abstract fun songsDao(): SongDao
    abstract fun categoryDao(): CategoryDao
    abstract fun levelDao(): LevelDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: SDDatabase? = null

        fun getDatabase(context: Context,scope: CoroutineScope): SDDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SDDatabase::class.java,
                    "sd_database"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .addCallback(SDDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun getRedeableDatabase (context: Context):SDDatabase{
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SDDatabase::class.java,
                    "sd_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not part of this codelab.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }


        private class SDDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            /**
             * Override the onOpen method to populate the database.
             * For this sample, we clear the database every time it is created or opened.
             */
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // If you want to keep the data through app restarts,
                // comment out the following line.
                INSTANCE?.let {
                    scope.launch(Dispatchers.IO) {
                      //  populateDatabase(database.wordDao())
                    }
                }
            }
        }

    }
}