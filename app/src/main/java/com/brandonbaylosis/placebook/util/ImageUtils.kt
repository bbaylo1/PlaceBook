package com.brandonbaylosis.placebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

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

    // Returns an empty FIle in app's private pictures folder using a unique filename
    // Flagged with @Throws to account for File.createTempFile() possibly throwing an
    // IOException
    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp =
                SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        // Creates filename by using current timestamp with "PlaceBook_" prepended
        val filename = "PlaceBook_" + timeStamp + "_"
        val filesDir = context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", filesDir)
    }

    // Calculates optimum inSampleSize that can be used to resize an image to a
    // specified width and height
    private fun calculateInSampleSize(
            width: Int, height: Int,
            reqWidth: Int, reqHeight: Int): Int {
        // This method starts with an inSampleSize of 1 (no downsampling), and it increases
        // the inSampleSize by a power of two until it reaches a value that will
        // cause the image to be downsampled to no larger than the requested
        // image width and height.
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun decodeFileToSize(filePath: String,
                         width: Int, height: Int): Bitmap {
        // 1 Size of image is loadid using BitmapFactory.decodeFile(). The
        // inJustDecodeBounds setting tells BitmapFactory to not load the
        // actual image, just its size.
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        // 2 calculateInSampleSize() is called with the image width and height and the
        // requested width and height. Options is updated with the resulting inSampleSize.
        options.inSampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight, width, height)
        // 3 Set to false to load the full image
        options.inJustDecodeBounds = false
        // 4 Loads downsampled image from the file returns it
        return BitmapFactory.decodeFile(filePath, options)
    }

    fun decodeUriStreamToSize(uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options: BitmapFactory.Options
            // 1 Opens inputStream for the Uri
            inputStream = context.contentResolver.openInputStream(uri)
            // 2 if not null, continue processing
            if (inputStream != null) {
                // 3 Determines image size
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)
                // 4 Closes and opens input stream again, then checks for null
                inputStream.close()
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // 5 image loaded from stream using the downsampling options and
                        // returned to caller
                    options.inSampleSize = calculateInSampleSize(
                            options.outWidth, options.outHeight,
                            width, height)
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeStream(
                            inputStream, null, options)
                    inputStream.close()
                    return bitmap
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            // 6 inputStream must be closed once opened, even if exception is thrown
            inputStream?.close()
        }
    }
}