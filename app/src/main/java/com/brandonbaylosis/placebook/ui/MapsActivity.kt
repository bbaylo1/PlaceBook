package com.brandonbaylosis.placebook.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.adapter.BookmarkInfoWindowAdapter
import com.brandonbaylosis.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Holds MapsViewModel, and is initialized when the map is ready
    private val mapsViewModel by viewModels<MapsViewModel>()

    // Loads activity_maps.xml Layout, then finds the map Fragment from the Layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        // Uses the found map Fragment to initialize the map
        mapFragment.getMapAsync(this)
        setupLocationClient()
        setupPlacesClient()
    }

    // Manipulates the map once available.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        createBookmarkMarkerObserver()
        getCurrentLocation()
    }

    // Called by onMapReady
    private fun setupMapListeners() {
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        // When called it calls displayPoi when a place on the map is tapped
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        // Call handleInfoWindowClick when user taps an info window
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
    }

    // Creates PlacesClient
    private fun setupPlacesClient() {
        Places.initialize(getApplicationContext(),
                getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    // Uses fused location API
    private fun setupLocationClient() {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION)
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }

    private fun getCurrentLocation() {
        // 1 Checks permissions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 2 Calls if no permissions granted
            requestLocationPermissions()
        } else {
            // Displays the blue dot that designates the user's location
            map.isMyLocationEnabled = true

            // 3 lastLocation runs in the background to fetch location
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                // Creates LatLng object from location if not null
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    // CameraUpdate object that specifies how map camera's updated
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    // Calls moveCamera to update with CameraUpdate object
                    map.moveCamera(update)
                } else {
                    // 8
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        // Checks if results match
        if (requestCode == REQUEST_LOCATION) {
            // Checks if first item in the array contains PERMISSION_GRANTED
                // and gets current location if correct
            if (grantResults.size == 1 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    // References refactored method to get details of points of interest
    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        // 1 Retrieves placeId
        val placeId = pointOfInterest.placeId
        // 2 Creates a field mask containing the attributes of the place that is to be retrieved
        val placeFields = listOf(Place.Field.ID,
                Place.Field.NAME,
                Place.Field.PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG)
        // 3 Creates a fetch request via familiar builder pattern
        val request = FetchPlaceRequest
                .builder(placeId, placeFields)
                .build()
        // 4 Handles request to fetch the place
        placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    // 5 Add success listener if response is received
                    // It then retrieves place object and then display name and phone number of the place
                    val place = response.place
                    displayPoiGetPhotoStep(place)
                }.addOnFailureListener { exception ->
                    // 6 Catches exceptions, mostly API errors
                    if (exception is ApiException) {
                        val statusCode = exception.statusCode
                        // Log status code and message
                        Log.e(TAG,
                                "Place not found: " +
                                        exception.message + ", " +
                                        "statusCode: " + statusCode)
                    }
                }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        // 1 Get first PhotoMetaData from retrieved photo metadata array
        val photoMetadata = place
                .getPhotoMetadatas()?.get(0)
        // 2 If no photo, skip
        if (photoMetadata == null) {
            // Pass along place object and null bitmap
            displayPoiDisplayStep(place, null)
            return
        }
        // 3 Passes the builder the photoMetaData, its max width and height for the retreived image
        val photoRequest = FetchPhotoRequest
                .builder(photoMetadata)
                .setMaxWidth(resources.getDimensionPixelSize(
                        R.dimen.default_image_width))
                .setMaxHeight(resources.getDimensionPixelSize(
                        R.dimen.default_image_height))
                .build()
        // 4 Calls fetchPhoto passing in photoRequest, letting callbacks handle the response
        placesClient.fetchPhoto(photoRequest)
                // If successful, assign photo to bitmap
                .addOnSuccessListener { fetchPhotoResponse ->
                    val bitmap = fetchPhotoResponse.bitmap
                    // Pass along the place object and the bitmap image
                    displayPoiDisplayStep(place, bitmap)
                }.addOnFailureListener { exception ->
                // Logs error if unsuccessful
                    if (exception is ApiException) {
                        val statusCode = exception.statusCode
                        Log.e(TAG,
                                "Place not found: " +
                                        exception.message + ", " +
                                        "statusCode: " + statusCode)
                    }
                }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?)
    {
        // Adds red marker
        val marker = map.addMarker(MarkerOptions()
                .position(place.latLng as LatLng)
                .title(place.name)
                .snippet(place.phoneNumber)
        )
        // Holds the full place object and associated bitmap photo
        marker?.tag = PlaceInfo(place, photo)
        // Instructs the map to display Info window for the marker
        marker?.showInfoWindow()

    }

    // Handles an action when the user taps a place Info window
    // Saves bookmark if it hasn't been saved before,
    // or starts bookmark details Activity if it has already been saved
    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {
            // Add the tapped place to repository
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
            }
        }
        // Removes marker from map
        marker.remove()
    }

    // Adds a single blue marker to the map based on BookmarkMarkerView
    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = map.addMarker(MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.8f))
        marker.tag = bookmark
        return marker
    }

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        // For loop that walks through a list of BookmarkMarkerView objects
        // and calls addPlaceMarker(bookmark) for each one
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    private fun createBookmarkMarkerObserver() {
        // Retrieves a LiveData object
        mapsViewModel.getBookmarkMarkerViews()?.observe(
            // Calls observe method to follow the lifecycle of the current activity
            // as well as to be notified when the underlying data changes on the LiveData object
            this, Observer<List<MapsViewModel.BookmarkMarkerView>> {
                // 2 Clear existing markers on map after the updated data is retrieved
                map.clear()
                // 3 Call displayAllBookmarks() passing in the list of updated BookmarkMarkerView
                // objects
                it?.let {
                    displayAllBookmarks(it)
                }
            })
    }

    // Class that holds a Place and a Bitmap
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)

}