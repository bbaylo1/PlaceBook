package com.brandonbaylosis.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

// 1 Declared as an object so it behaves like a singleton
object ImageUtils {
    // 2 Takes in a context, bitmap, and string object filename
    // then saves the Bitmap to permanent storage
    fun saveBitmapToFile(context: Context, bitmap: Bitmap,
                         filename: String) {
        // 3 Holds image data
        val stream = ByteArrayOutputStream()
        // 4 Write image bitmap to stream object using the lossless PNG format
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        // 5 converts stream to an array of bytes
        val bytes = stream.toByteArray()
        // 6 Called to write bytes to a file
        ImageUtils.saveBytesToFile(context, bytes, filename)
    }
    // 7 Takes in a Context, ByteArray, and String object filename and saves bytes to a file
    private fun saveBytesToFile(context: Context, bytes:
    ByteArray, filename: String) {
        val outputStream: FileOutputStream
        // 8 Next calls could cause exceptions, so they're wrapped in a try/catch to prevent
        // a crash
        try {
            // 9 openFileOutput is used to open a FileOutputStream using the given filename.
            // Context.MODE_PRIVATE flag causes the file to be written in the private area
            // where only the app can access it.
            outputStream = context.openFileOutput(filename,
                    Context.MODE_PRIVATE)
            // 10 Writes bytes to outputStream and closes the stream
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    // Passed a context and a filename and returns a Bitmap image by loading
    // the image from the specified filename.
    fun loadBitmapFromFile(context: Context, filename: String):
            Bitmap? {
        // File object used to combine files directory for the given context with
        // the filename. Then a filePath's constructed from the absolute path of the File
        val filePath = File(context.filesDir, filename).absolutePath
        // Loads the image from the file, and the image is returned to the caller
        return BitmapFactory.decodeFile(filePath)
    }
}