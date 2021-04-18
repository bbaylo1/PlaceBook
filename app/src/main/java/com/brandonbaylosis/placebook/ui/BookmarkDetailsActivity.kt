package com.brandonbaylosis.placebook.ui

import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*


class BookmarkDetailsActivity : AppCompatActivity() {
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

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
}