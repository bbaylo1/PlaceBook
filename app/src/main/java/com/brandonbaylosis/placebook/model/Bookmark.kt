package com.brandonbaylosis.placebook.model

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.brandonbaylosis.placebook.util.FileUtils
import com.brandonbaylosis.placebook.util.ImageUtils

// 1 Designates this as a data base entry class to Room
@Entity
// 2 Define primary constructor by using arguments for all properties with the default values defined
data class Bookmark(
// 3 Defines id property, autoGenerate automatically generates incrementing numbers for this field
        @PrimaryKey(autoGenerate = true) var id: Long? = null,
        var placeId: String? = null,
        var name: String = "",
        var address: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var phone: String = "",
        var notes: String = "",
        var category: String = ""

)

{
    // 1 Provides public interface for saving an image for a Bookmark
    fun setImage(image: Bitmap, context: Context) {
        // 2 If bookmark has an id, image is saved to a file
        // The filename incorporates the bookmark ID
        id?.let {
            ImageUtils.saveBitmapToFile(context, image, generateImageFilename(it))
        }
    }
    // 3 generateImageFilename() is placed in a companion object so it's available at
    // the class level, allowing another object to load an image without having to load
    // the bookmark from the database
    companion object {
        fun generateImageFilename(id: Long): String {
            // 4 generateImageFilename returns filename based on a Bookmark ID, using
            // an algorithm that appends the bookmark ID to the word "bookmark".
            return "bookmark$id.png"
        }
    }

    // Uses FileUtils.deleteFile() in the util package to delete image file associated
    // with the current bookmark
    fun deleteImage(context: Context) {
        id?.let {
            FileUtils.deleteFile(context, generateImageFilename(it))
        }
    }
}