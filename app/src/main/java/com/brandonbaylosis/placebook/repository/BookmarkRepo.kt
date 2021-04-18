package com.brandonbaylosis.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.db.BookmarkDao
import com.brandonbaylosis.placebook.db.PlaceBookDatabase
import com.brandonbaylosis.placebook.model.Bookmark
import com.google.android.libraries.places.api.model.Place

// 1 Defines the class with a construct that passes in an object called context
class BookmarkRepo(private val context: Context) {
    // 2 Properties defined that will be used for BookmarkRepo's data source
    private var db = PlaceBookDatabase.getInstance(context)
    private var bookmarkDao: BookmarkDao = db.bookmarkDao()
    // Initialize categoryMap to hold mapping of place types to category names
    private var categoryMap: HashMap<Place.Type, String> = buildCategoryMap()
    // Initialized to hold mapping of category names to resource IDs
    private var allCategories: HashMap<String, Int> = buildCategories()
    // Defines a get accessor on categories that takes all of the HashMap keys
    // then returns them as an ArrayList of strings
    val categories: List<String>
        get() = ArrayList(allCategories.keys)

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

    // Builds a HashMap that relates Place types to category names
    private fun buildCategoryMap() : HashMap<Place.Type, String> {
        return hashMapOf(
                Place.Type.BAKERY to "Restaurant",
                Place.Type.BAR to "Restaurant",
                Place.Type.CAFE to "Restaurant",
                Place.Type.FOOD to "Restaurant",
                Place.Type.RESTAURANT to "Restaurant",
                Place.Type.MEAL_DELIVERY to "Restaurant",
                Place.Type.MEAL_TAKEAWAY to "Restaurant",
                Place.Type.GAS_STATION to "Gas",
                Place.Type.CLOTHING_STORE to "Shopping",
                Place.Type.DEPARTMENT_STORE to "Shopping",
                Place.Type.FURNITURE_STORE to "Shopping",
                Place.Type.GROCERY_OR_SUPERMARKET to "Shopping",
                Place.Type.HARDWARE_STORE to "Shopping",
                Place.Type.HOME_GOODS_STORE to "Shopping",
                Place.Type.JEWELRY_STORE to "Shopping",
                Place.Type.SHOE_STORE to "Shopping",
                Place.Type.SHOPPING_MALL to "Shopping",
                Place.Type.STORE to "Shopping",
                Place.Type.LODGING to "Lodging",
                Place.Type.ROOM to "Lodging"
        )
    }

    // Takes in a PLace type and converts it to a valid category
    fun placeTypeToCategory(placeType: Place.Type): String {
        // Category initialized to Other by default
        var category = "Other"
        // If categoryMap contains a key matching placeType it's assigned to category
        if (categoryMap.containsKey(placeType)) {
            category = categoryMap[placeType].toString()
        }
        return category
    }

    // Builds HashMap that relates category names to category icon resource IDs
    private fun buildCategories() : HashMap<String, Int> {
        return hashMapOf(
                "Gas" to R.drawable.ic_gas,
                "Lodging" to R.drawable.ic_lodging,
                "Other" to R.drawable.ic_other,
                "Restaurant" to R.drawable.ic_restaurant,
                "Shopping" to R.drawable.ic_shopping
        )
    }

    // Public method to convert a category name to resource ID
    fun getCategoryResourceId(placeCategory: String): Int? {
        return allCategories[placeCategory]
    }

    // Deletes bookmark image and bookmark from database
    fun deleteBookmark(bookmark: Bookmark) {
        bookmark.deleteImage(context)
        bookmarkDao.deleteBookmark(bookmark)
    }

}