package com.brandonbaylosis.placebook.adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.ui.MapsActivity
import com.brandonbaylosis.placebook.viewmodel.MapsViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

//  Take single parameter representing hosting activity
// This class implements the GoogleMap.InfoWindowAdapter
class BookmarkInfoWindowAdapter(val context: Activity) :
        GoogleMap.InfoWindowAdapter {
    //  Hold contents view
    private val contents: View
    // When adapter is instantiated, content_bookmark_info.xml is inflated and saved to contents
    init {
        contents = context.layoutInflater.inflate(
                R.layout.content_bookmark_info, null)
    }
    // Override and return null to indicate the entire info window won't be replaced
    override fun getInfoWindow(marker: Marker): View? {
        // This function is required, but can return null if
        // not replacing the entire info window
        return null
    }
    // Fill in titleView and phoneView widgets on Layout
    // Casts marker.tag to PlaceInfo object and access image property to set it
    // as the imageView bitmap
    override fun getInfoContents(marker: Marker): View? {
        val titleView = contents.findViewById<TextView>(R.id.title)
        titleView.text = marker.title ?: ""
        val phoneView = contents.findViewById<TextView>(R.id.phone)
        phoneView.text = marker.snippet ?: ""

        val imageView = contents.findViewById<ImageView>(R.id.photo)

        when (marker.tag) {
            // 1 If marker.tag's a MapsActivity.PlaceInfo, imageView bitmap is set
                // directly from the PlaceInfo.image object
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap((marker.tag as MapsActivity.PlaceInfo).image)
            }
            // 2 If marker.tag's a MapsViewModel.BookmarkMarkerView, the imageView
            // bitmap is set from the BookmarkMarkerView
            is MapsViewModel.BookmarkView -> {
                var bookMarkview = marker.tag as
                        MapsViewModel.BookmarkView
                // Set imageView bitmap here
                imageView.setImageBitmap(bookMarkview.getImage(context))

            }
        }
        return contents
    }

}