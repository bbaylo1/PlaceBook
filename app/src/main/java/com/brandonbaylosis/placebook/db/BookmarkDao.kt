package com.brandonbaylosis.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.brandonbaylosis.placebook.model.Bookmark

// 1 Designates this to Room as Data Access Object
@Dao
interface BookmarkDao {
    // 2 Define an sql statement to read all of the bookmarks from the database and
    // return them as List of Bookmarks
    @Query("SELECT * FROM Bookmark")
    fun loadAll(): LiveData<List<Bookmark>>
    // 3 Returns a bookmark object
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>
    // 4 Use Insert annotation to define insertBookmark
    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long
    // 5  Updates a single bookmark in the database via the passed in bookmark argument
    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)
    // 6 Deletes existing bookmark based on the passed in bookmaark
    @Delete
    fun deleteBookmark(bookmark: Bookmark)
}