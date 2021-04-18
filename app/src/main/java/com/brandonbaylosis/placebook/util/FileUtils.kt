package com.brandonbaylosis.placebook.util

import android.content.Context
import java.io.File

// Utility method that deletes a single file in the app’s main files directory.
// Deletes image associated with a deleted bookmark
object FileUtils {
    fun deleteFile(context: Context, filename: String) {
        val dir = context.filesDir
        val file = File(dir, filename)
        file.delete()
    }
}