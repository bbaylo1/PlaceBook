package com.brandonbaylosis.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.brandonbaylosis.placebook.db.BookmarkDao
import com.brandonbaylosis.placebook.db.PlaceBookDatabase
import com.brandonbaylosis.placebook.model.Bookmark

// 1 Defines the class with a construct that passes in an object called context
class BookmarkRepo(context: Context) {
    // 2 Properties defined that will be used for BookmarkRepo's data source
    private var db = PlaceBookDatabase.getInstance(context)
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()
    // 3 Allows a single Bookmark to be added to the repo
    fun addBookmark(bookmark: Bookmark): Long? {
        // Adds the Bookmark to the database and assigns the newId to the Bookmark
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }
    // 4 Helper method to return a freshly initialized Bookmark object
    fun createBookmark(): Bookmark {
        return Bookmark()
    }
    // 5 Returns a liveData list of all Bookmarks in the repository
    val allBookmarks: LiveData<List<Bookmark>>
        get() {
            // Return the results to the caller
            return bookmarkDao.loadAll()
        }
    // Returns live bookmark from bookmark DAO
    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> {
        val bookmark = bookmarkDao.loadLiveBookmark(bookmarkId)
        return bookmark
    }

    // Takes in a bookmark and saves it using the boomark DAO
    fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }
    // Takes in bookmark ID and uses the bookmark DAO to load
    // the corresponding bookmark
    fun getBookmark(bookmarkId: Long): Bookmark {
        return bookmarkDao.loadBookmark(bookmarkId)
    }
}