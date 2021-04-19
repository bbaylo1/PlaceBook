package com.brandonbaylosis.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.brandonbaylosis.placebook.model.Bookmark
import com.brandonbaylosis.placebook.repository.BookmarkRepo
import com.brandonbaylosis.placebook.util.ImageUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

// 1 Inherits from AndroidViewModel, allows to include application context needed
// when creating BookmarkRepo
class MapsViewModel(application: Application) : AndroidViewModel(application) {
    // Defines an object that wraps a list of BookmarkMarkerView objects
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    private val TAG = "MapsViewModel"
    // 2 Create BookmarkRepo object and pass it in the application context
    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    // 3 Declare that this method takes in a Google Place and a BitmapImage
    // Called by MapsActivity when it wants to make a Bookmark for a Google Place identified
    // by a user
    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        // 4 Creates empty Bookmark object and fills it using the Place data
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        // If latLng is null, set to 0.0
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        // Assigns category to new bookmark
        bookmark.category = getPlaceCategory(place)

        // 5 Save the Bookmark to repository and print verification message
        val newId = bookmarkRepo.addBookmark(bookmark)
        // Updates addBookmarkFromPlace() to call the new setImage() method if image is not null
        image?.let { bookmark.setImage(it, getApplication()) }
        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    // Converts Bookmark object from repo into a BookmarkView object
    private fun bookmarkToBookmarkView(bookmark: Bookmark):
            MapsViewModel.BookmarkView {
        return MapsViewModel.BookmarkView(
                bookmark.id,
                LatLng(bookmark.latitude, bookmark.longitude),
                bookmark.name,
                bookmark.phone,
                bookmarkRepo.getCategoryResourceId(bookmark.category))
    }

    private fun mapBookmarksToBookmarkView() {
        // 1 Uses Transformations class to dynamically map Bookmark objects into
        // BookmarkMarkerView objects as they get updated
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            // 2 Provides a list of Bookmarks returned from the bookmark repo
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
                // Stored in repoBookmarks variable
            }
        }
    }

    // Returns the LiveData object that'll be observed by MapsActivity
    fun getBookmarkViews() :
            LiveData<List<BookmarkView>>? {
        // Bookmarks are null first call, if null, it calls
        // this function to set up the initial mapping
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    data class BookmarkView(val id: Long? = null,
                            val location: LatLng = LatLng(0.0, 0.0),
                            val name: String = "",
                            val phone: String = "",
                            val categoryResourceId: Int? = null)
    {
        // Check to make sure BookmarkMarkerView has a valid ID
        fun getImage(context: Context): Bitmap? {
            id?.let{
                // Then, calls generateImageFilename() and pass in the
                // bookmark ID represented as id
                // loadBitmapFromFile() is then called with the current context and Bookmark
                // image filename, and it returns the resulting Bitmap to the caller.
                return ImageUtils.loadBitmapFromFile(context,
                        Bookmark.generateImageFilename(it))
            }
            return null
        }
    }

    // Converts place type to bookmark category
    private fun getPlaceCategory(place: Place): String {
        // 1 Defaults to "Other" in case no type assigned to place
        var category = "Other"
        val placeTypes = place.types
        placeTypes?.let { placeTypes ->
            // 2 Checks placeTypes list to see if it's populated
            if (placeTypes.size > 0) {
                // 3 If so, extract first type from the List and call
                    // placeTypeToCategory() to make the conversion
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        // 4 returns category
        return category
    }

    // Takes in a LatLng location and creates a new untitled bookmark
    // at the given location
    fun addBookmark(latLng: LatLng) : Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        // Returns bookmark ID to caller
        return bookmarkRepo.addBookmark(bookmark)
    }
}