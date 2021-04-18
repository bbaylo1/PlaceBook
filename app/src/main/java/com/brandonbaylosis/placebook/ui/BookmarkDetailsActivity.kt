package com.brandonbaylosis.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.util.ImageUtils
import com.brandonbaylosis.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*
import java.io.File


class BookmarkDetailsActivity : AppCompatActivity(),
        PhotoOptionDialogFragment.PhotoOptionDialogListener {
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    private var photoFile: File? = null


    override fun onCreate(savedInstanceState:
                          android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_details)
        setupToolbar()
        // Processes Intent data passed in from maps Activity
        getIntentData()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    // Populates all of the UI fields using the current bookmarkView
    // if it's not null
    private fun populateFields() {
        bookmarkDetailsView?.let { bookmarkView ->
            editTextName.setText(bookmarkView.name)
            editTextPhone.setText(bookmarkView.phone)
            editTextNotes.setText(bookmarkView.notes)
            editTextAddress.setText(bookmarkView.address)
        }
    }

    // Loads image from bookmarkView and then uses it to set the imageViewPlace
    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                imageViewPlace.setImageBitmap(placeImage)

            }
        }
        // Sets click listener on imageViewPlace and calls replaceImage()
        // when image is tapped
        imageViewPlace.setOnClickListener {
            replaceImage()
        }
    }

    private fun getIntentData() {
        // 1 Pull bookmarkId from Intent data
        val bookmarkId = intent.getLongExtra(
            MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)
        // 2 Retrieve BookmarkDetailsView from BookmarkDetailsViewModel
        // and then observes it for changes
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
            this,
            Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {
                // 3 When BookmarkDetailsView is loaded or changed, bookmarkDetailsView
                // property is assigned to it, and populate bookmark fields from the data
                // Calls the previously defined functions to populate the fields
                it?.let {
                    bookmarkDetailsView = it
                    // Populate fields from bookmark
                    populateFields()
                    populateImageView()
                }
            })
    }

    // Overrides onCreateOptions menu and provide items for the toolbar
    // by loading in menu_bookmark_details menu
    override fun onCreateOptionsMenu(menu: android.view.Menu):
            Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    // Takes current changes from text fields and updates the bookmark
    private fun saveChanges() {
        val name = editTextName.text.toString()
        // Doesn't doo anything if editTextName is blank
        if (name.isEmpty()) {
            return
        }
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = editTextName.text.toString()
            bookmarkView.notes = editTextNotes.text.toString()
            bookmarkView.address = editTextAddress.text.toString()
            bookmarkView.phone = editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish()
    }

    // Method called when user selects a Toolbar checkmarked item
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Check item.itemId to see if it matches action_save
        when (item.itemId) {
            R.id.action_save -> {
                // Calls saveChanges if it does match
                saveChanges()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCaptureClick() {
        // 1 Clears previously assigned photoFile
        photoFile = null
        try {
            // 2 call createUniqueImageFile() to create a uniquely named
                // image File and assign it to photoFile
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            // 3 If an exception is thrown, the method returns without doing anything.
            return
        }
        // 4 use the ?.let to make sure photoFile is not null before continuing with
        // the rest of the method.
        photoFile?.let { photoFile ->
            // 5 FileProvider.getUriForFile() is called to get a Uri for the temporary
            // photo file
            val photoUri = FileProvider.getUriForFile(this,
                    "com.brandonbaylosis.placebook.fileprovider",
                    photoFile)
            // 6 New Intent created via ACTION_IMAGE_CAPTURE action to display
            // campera viewfinder and allow the user to snap a new photo
            val captureIntent =
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // 7 The photoUri is added as an extra on Intent,
            // so Intent knows where to save the full-size image captured by the user.

            captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                    photoUri)
            // 8 Temporary write permissions on the photoUri are given to the Intent.
            val intentActivities = packageManager.queryIntentActivities(
                    captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
            intentActivities.map { it.activityInfo.packageName }
                    .forEach { grantUriPermission(it, photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            // 9 Invokes Intent, passing in request code REQUEST_CAPTURE_IMAGE
            startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
        }
    }

    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    // When user taps bookmark image, call replaceImage.
    // It then attempts to create PhotoOptionDialogFragment
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        // If newFragment is not null, it is displayed
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    // Assigns image to imageViewPlace and saves it to bookmark image file
    // using bookmarkDetailsView.setImage()
    private fun updateImage(image: Bitmap) {
        val bookmarkView = bookmarkDetailsView ?: return
        imageViewPlace.setImageBitmap(image)
        bookmarkView.setImage(this, image)
    }

    // Uses new decodeFIleSize method to load downsampled image and return int
    private fun getImageWithPath(filePath: String): Bitmap? {
        return ImageUtils.decodeFileToSize(filePath,
                resources.getDimensionPixelSize(
                        R.dimen.default_image_width),
                resources.getDimensionPixelSize(
                        R.dimen.default_image_height))
    }

    // Called by android when an Activity returns a result such as
    // the Camera capture activity
    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 1 resultCode checked to make sure the user didn’t cancel the photo capture.
        if (resultCode == android.app.Activity.RESULT_OK) {
            // 2 requestCode is checked to see which call is returning a result.
            when (requestCode) {
                // 3 If requestCode matches REQUEST_CAPTURE_IMAGE, processing continues.
                REQUEST_CAPTURE_IMAGE -> {
                    // 4 return early from the method if there is no photoFile defined.
                    val photoFile = photoFile ?: return
                    // 5 e permissions set before are now revoked since they’re no longer needed
                    val uri = FileProvider.getUriForFile(this,
                            "com.brandonbaylosis.placebook.fileprovider",
                            photoFile)
                    revokeUriPermission(uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    // 6 getImageWithPath() is called to get the image from the new photo
                    // path, and updateImage() is called to update the bookmark image
                    val image = getImageWithPath(photoFile.absolutePath)
                    image?.let { updateImage(it) }
                }
                // If Activity result is from selecting a gallery image, and the data
                // returned is valid, then getImageWithAuthority() is called to
                // load the selected image.
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null)
                {
                    val imageUri = data.data as Uri
                    val image = getImageWithAuthority(imageUri)
                    // updateImage() is called to update the bookmark image.
                    image?.let { updateImage(it) }
                }
            }
        }
    }

    // Uses decodeUriStreamToSize method to load downsampled image and return it
    private fun getImageWithAuthority(uri: Uri): Bitmap? {
        return ImageUtils.decodeUriStreamToSize(uri,
                resources.getDimensionPixelSize(
                        R.dimen.default_image_width),
                resources.getDimensionPixelSize(
                        R.dimen.default_image_height),
                this)
    }

    // Defines request code to use when processing the camera capture Intent
    companion object {
        private const val REQUEST_CAPTURE_IMAGE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
    }
}