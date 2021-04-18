package com.brandonbaylosis.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.brandonbaylosis.placebook.model.Bookmark
import com.brandonbaylosis.placebook.repository.BookmarkRepo
import com.brandonbaylosis.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {
    private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    data class BookmarkDetailsView(
            var id: Long? = null,
            var name: String = "",
            var phone: String = "",
            var address: String = "",
            var notes: String = "",
            var category: String = "",
            var longitude: Double = 0.0,
            var latitude: Double = 0.0,
            var placeId: String? = null

    ) {
        fun getImage(context: Context): Bitmap? {
            id?.let {
                return ImageUtils.loadBitmapFromFile(context,
                    Bookmark.generateImageFilename(it))
            }
            return null
        }
        // Takes in a Bitmap image and saves it to associated image file
        // for current BookmarkView
        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(context, image,
                        Bookmark.generateImageFilename(it))
            }
        }
    }

    // Converts a Bookmark model to a BookmarkDetailsView model
    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
                bookmark.id,
                bookmark.name,
                bookmark.phone,
                bookmark.address,
                bookmark.notes,
                bookmark.category,
                bookmark.longitude,
                bookmark.latitude,
                bookmark.placeId
        )
    }

    // Get live Bookmark from BookmarkRepo and then transform it to
    // the live BookmarkDetailsView
    fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark)
        { repoBookmark ->
            repoBookmark?.let { repoBookmark ->
                bookmarkToBookmarkView(repoBookmark)
            }
        }
    }
    fun getBookmark(bookmarkId: Long):
            LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }
        return bookmarkDetailsView
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView):
            Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // 1 Couroutine used to run method in background so calls
        // can be made by the bookmark repo that access the database
        GlobalScope.launch {
            // 2  Converts BookmarkDetailsView to a Bookmark
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            // 3 If bookmark's not null it's updated in bookmark repo
            // This updates the bookmark in the database
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    // Pass-through to a similar method in bookmark repo to return
    // category resource ID from a category name
    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }

    // Another pass-through method, returns categories list from bookmark repo
    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }

    // Takes in  BookmarkDetailsView and loads the bookmark from the
    // repo. If bookmark is found, it calls deleteBookmark() on the repo.
    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let {
                bookmarkRepo.getBookmark(it)
            }
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }

}