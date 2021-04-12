package com.brandonbaylosis.placebook.viewmodel

import android.app.Application
import android.util.Log
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.brandonbaylosis.placebook.model.Bookmark
import com.brandonbaylosis.placebook.repository.BookmarkRepo
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

// 1 Inherits from AndroidViewModel, allows to include application context needed
// when creating BookmarkRepo
class MapsViewModel(application: Application) : AndroidViewModel(application) {
    // Defines an object that wraps a list of BookmarkMarkerView objects
    private var bookmarks: LiveData<List<BookmarkMarkerView>>? = null

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
        // 5 Save the Bookmark to repository and print verification message
        val newId = bookmarkRepo.addBookmark(bookmark)
        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    // Converts Bookmark object from repo into a BookmarkMarkerView object
    private fun bookmarkToMarkerView(bookmark: Bookmark):
            MapsViewModel.BookmarkMarkerView {
        return MapsViewModel.BookmarkMarkerView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude))
    }

    private fun mapBookmarksToMarkerView() {
        // 1 Uses Transformations class to dynamically map Bookmark objects into
        // BookmarkMarkerView objects as they get updated
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks)
        { repoBookmarks ->
            // 2 Provides a list of Bookmarks returned from the bookmark repo
            repoBookmarks.map { bookmark ->
                bookmarkToMarkerView(bookmark)
                // Stored in repoBookmarks variable
            }
        }
    }

    // Returns the LiveData object that'll be observed by MapsActivity
    fun getBookmarkMarkerViews() :
            LiveData<List<BookmarkMarkerView>>? {
        // Bookmarks are null first call, if null, it calls
        // this function to set up the initial mapping
        if (bookmarks == null) {
            mapBookmarksToMarkerView()
        }
        return bookmarks
    }

    // Holds information needed by the View to plot a marker for a single bookmark
    data class BookmarkMarkerView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0))
    }