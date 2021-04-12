package com.brandonbaylosis.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.brandonbaylosis.placebook.model.Bookmark

// 1 Identifies Database class to Room
@Database(entities = arrayOf(Bookmark::class), version = 1)
abstract class PlaceBookDatabase : RoomDatabase() {
    // 2 Returns a DAO interface
    abstract fun bookmarkDao(): BookmarkDao
    // 3 Define a companion object on PlaceBookDatabase
    companion object {
        // 4 Define the one and only instance variable on the companion object
        private var instance: PlaceBookDatabase? = null
        // 5 Define getInstance() to take in a Context and return the single PlaceBookDatabase instance
        fun getInstance(context: Context): PlaceBookDatabase {
            if (instance == null) {
                // 6 If it's the first time getInstance is called,
                    // , a single PlaceBookDatabase instance is made
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceBookDatabase::class.java, // Creates a room database based on the abstract class
                    "PlaceBook").build()
            }
            // 7 returns the instance
            return instance as PlaceBookDatabase
        }
    }
}