package com.brandonbaylosis.placebook.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.brandonbaylosis.placebook.R
import com.brandonbaylosis.placebook.adapter.BookmarkInfoWindowAdapter
import com.brandonbaylosis.placebook.adapter.BookmarkListAdapter
import com.brandonbaylosis.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()


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
        setupToolbar()
        setupPlacesClient()
        setupNavigationDrawer()

    }

    // Manipulates the map once available.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        createBookmarkObserver()
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
        fab.setOnClickListener {
            searchAtCurrentLocation()
        }

        map.setOnMapLongClickListener { latLng ->
            newBookmark(latLng)
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
        // Defines a key for storing the bookmark ID in the intent extras
        const val EXTRA_BOOKMARK_ID = "com.brandonbaylosis.placebook.EXTRABOOKMARK_ID"
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
        private const val AUTOCOMPLETE_REQUEST_CODE = 2

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
        showProgress() // Displays progress bar when tapped
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
                Place.Field.LAT_LNG,
                Place.Field.TYPES)
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
                        hideProgress()
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
                    hideProgress()
                }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?)
    {
        hideProgress()
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
        when (marker.tag) {
            is MapsActivity.PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                            placeInfo.image)
                    }
                }
                marker.remove();
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = (marker.tag as
                        MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    // Adds a single blue marker to the map based on BookmarkMarkerView
    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkView): Marker? {
        val marker = map.addMarker(MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let {
                    BitmapDescriptorFactory.fromResource(it)
                })
                .alpha(0.8f))
        marker.tag = bookmark
        // Adds new entry to markers when a new marker is added to the map
        bookmark.id?.let { markers.put(it, marker) }
        return marker
    }

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkView>) {
        // For loop that walks through a list of BookmarkMarkerView objects
        // and calls addPlaceMarker(bookmark) for each one
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    private fun createBookmarkObserver() {
        // Retrieves a LiveData object
        mapsViewModel.getBookmarkViews()?.observe(
            // Calls observe method to follow the lifecycle of the current activity
            // as well as to be notified when the underlying data changes on the LiveData object
            this, Observer<List<MapsViewModel.BookmarkView>> {
                // 2 Clear existing markers on map after the updated data is retrieved
                map.clear()
            // Clears markers when bookmark data changes. Markers are populated again as all of the
            // bookmarks are added to the map
            markers.clear()
            // 3 Call displayAllBookmarks() passing in the list of updated BookmarkMarkerView
                // objects
                it?.let {
                    displayAllBookmarks(it)
                    // sets the new list of BookmarkView items on the recycler view adapter
                    // whenever the bookmark data changes
                    bookmarkListAdapter.setBookmarkData(it)
                }
            })
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        // Adds bookmarkId as extra parameter on the Intent
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    // Activates support for the support toolbar in the maps Activity
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        // Takes drawerLayout and toolbar and fully manages the display
        // and functionality of the toggle icon
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer)
        // Called to ensure toggle icon is displayed initially
        toggle.syncState()
    }

    // Sets up adapter for bookmark recycler view
    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        // Gets the RecyclerView from the Layout, sets a default
        // LinearLayoutManager for the RecyclerView, then creates a
        // new BookmarkListAdapter and assigns it to the RecyclerView
        bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
    }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        // 1 Navigation drawer closed before zooming the bookmark
        drawerLayout.closeDrawer(drawerView)
        // 2 markers HashMap used to look up the Marker
        val marker = markers[bookmark.id]
        // 3 If marker found, Info window shown
        marker?.showInfoWindow()
        // 4 Location object created from bookmark
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        // Called to zoom the mapp to the bookmark
        updateMapToLocation(location)
    }

    private fun searchAtCurrentLocation() {
        // 1 Define fields
        val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.PHONE_NUMBER,
                Place.Field.PHOTO_METADATAS,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.TYPES)
        // 2 Computes bounds of currently visible region of the map
        val bounds =
                RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)
        try {
            // 3 Autocomplete provides IntentBuilder method to build up the Intent
            // Passes AutocompleteActivityMode.OVERLAY to indicate that the
            // search widget can overlay current activity. Other option
                // is .FULLSCREEN which causes search interface to replace entire screen
            val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, placeFields)
                    // Pass map bounds to setBoundBias
                    .setLocationBias(bounds)
                    .build(this)
            // 4 Start Activity, passing request code of AUTOCOMPLETE_REQUEST_CODE
            // Results are identified by this search code
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesRepairableException) {
            //TODO: Handle exception
        } catch (e: GooglePlayServicesNotAvailableException) {
            // //TODO: Handle exception
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 1 Check requestCode to make sure it matches AUTOCOMPLETE_REQUEST_CODE passed
        // into startActivityForResult
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE ->
                // 2 If resultCode finds a place, and data is not null, continue
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 3 getPlaceFromIntent takes data and returns populated Place object
                    val place = Autocomplete.getPlaceFromIntent(data)
                    // 4
                    val location = Location("")
                    // Converts place latLng to a location
                    location.latitude = place.latLng?.latitude ?: 0.0
                    location.longitude = place.latLng?.longitude ?: 0.0
                    // Passes to existing updateMapToLocation method, causing
                    // the map to zoom to the place
                    updateMapToLocation(location)
                    // Displays progress bar after searching for a place but before
                    // place photo is loaded
                    showProgress()

                    // 5 Instead of processing the data in several steps, it can start at the
                    // displayPoiGetPhotoMetaDataStep() and pass it the found place. This
                    // loads the place photo and displays the place Info window.
                    displayPoiGetPhotoStep(place)
                }
        }
    }

    // Creates a new bookmark from a location, then starts the bookmark details Activity
    // to allow editing of new bookmark
    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    // Sets a flag on the main window to prevent user touches
    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // Clears the flag set
    private fun enableUserInteraction() {
        window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    //Makes progress bar visible and disables user interaction
    private fun showProgress() {
        progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }

    // Hides progress bar and enables user interaction
    private fun hideProgress() {
        progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }

    // Class that holds a Place and a Bitmap
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)

}