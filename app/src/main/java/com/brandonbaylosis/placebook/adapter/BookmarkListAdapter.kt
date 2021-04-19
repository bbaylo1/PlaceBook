package com.brandonbaylosis.placebook.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.ui.MapsActivity
import com.brandonbaylosis.placebook.viewmodel.MapsViewModel
import kotlinx.android.synthetic.main.bookmark_item.view.*

// 1 Adapter takes two arguments defined as class properties
class BookmarkListAdapter(
        // List of BookmarkView items and a reference to MapsActivity
        private var bookmarkData: List<MapsViewModel.BookmarkView>?,
        private val mapsActivity: MapsActivity) :
        RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {
    // 2 Holds view widgets
    class ViewHolder(v: View,
                     private val mapsActivity: MapsActivity) :
            RecyclerView.ViewHolder(v) {
        val nameTextView: TextView = v.bookmarkNameTextView
        val categoryImageView: ImageView = v.bookmarkIcon
        // Called when ViewHolder is initialized
        init {
            // Sets an onClickListener on the ViewHolder that when fired
            // it gets the bookmarkView associated with the ViewHolder and call
            // moveToBookmark() to zoom the map to the bookmark.
            v.setOnClickListener {
                val bookmarkView = itemView.tag as MapsViewModel.BookmarkView
                mapsActivity.moveToBookmark(bookmarkView)
            }
        }

    }
    // 3 To be called when bookmark data changes
    fun setBookmarkData(bookmarks: List<MapsViewModel.BookmarkView>) {
        // Assigns bookmarks to new BookmarkViewList
        this.bookmarkData = bookmarks
        // Refreshes RecyclerView when called
        notifyDataSetChanged()
    }
    // 4 Creates ViewHolder by inflating bookmark_item layout and passing in
    // mapsActivity property
    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int): BookmarkListAdapter.ViewHolder {
        val vh = ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.bookmark_item, parent, false), mapsActivity)
        return vh
    }
    override fun onBindViewHolder(holder: ViewHolder,
                                  position: Int) {
        // 5 Assigns bookmarkData to bookmarkData if not null,
        // but returns early if null
        val bookmarkData = bookmarkData ?: return
        // 6 Assigns bookmarkViewData to bookmark data for current item position
        val bookmarkViewData = bookmarkData[position]
        // 7 Reference to bookmarkViewData is assigned to holder's itemView.tag
        // and ViewHolder items are populated from bookmarkViewData
        holder.itemView.tag = bookmarkViewData
        holder.nameTextView.text = bookmarkViewData.name
        // Checks to see if categoryResourcesId is set, if so it sets image
        // resource to categoryResourceID
        bookmarkViewData.categoryResourceId?.let {
            holder.categoryImageView.setImageResource(it)
        }
    }
    // 8 getItemCount's overridden to return number of items in bookmarkData list
    override fun getItemCount(): Int {
        return bookmarkData?.size ?: 0
    }
}