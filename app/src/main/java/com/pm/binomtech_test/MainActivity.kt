package com.pm.binomtech_test

import android.Manifest
import android.content.pm.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.*
import android.location.*
import android.os.*
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.pm.binomtech_test.Utils.toPx
import org.osmdroid.config.Configuration
import org.osmdroid.events.*
import org.osmdroid.tileprovider.tilesource.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.*
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.*

class MainActivity : ComponentActivity(), IMyLocationProvider {
    private var mapView: MapView? = null

    // My Location
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var locationRunning = false
    private var locConsumer: IMyLocationConsumer? = null

    // Marker
    private var selectedMarkerIdx = -1
    // TODO: dummy data, should be loaded from server and declared in model according to MVVM
    private var allMarkers: List<BinomMarker> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // TODO: dummy data
        allMarkers = listOf(
            BinomMarker(
                loc = Utils.locationByLatLng(54.7497331,55.9993335),
                dispNetworkType = "GPS",
                dispDate = "01.01.24",
                dispTime = "12:15",
                iconAsset = "person.png"
            ),
            BinomMarker(
                loc = Utils.locationByLatLng(54.7393646,55.9569262),
                dispNetworkType = "GPS",
                dispDate = "02.07.17",
                dispTime = "14:00",
                iconAsset = "person.png"
            ),
            BinomMarker(
                loc = Utils.locationByLatLng(54.7739054,56.0611441),
                dispNetworkType = "Cellular",
                dispDate = "05.07.25",
                dispTime = "12:00",
                iconAsset = "person.png"
            ),
        )
        locationRunning = false
        setupMap()
        setupMarkers()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupCurrentLocation()
        } else {
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults: Map<String, Boolean> ->
                if (grantResults.all { it.value }) {
                    setupCurrentLocation()
                } else {
                    Toast.makeText(this, R.string.loc_permission_required, Toast.LENGTH_LONG).show()
                }
            }
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    private fun setupMap() {
        // Map
        mapView = findViewById(R.id.mapview)
        mapView!!.isClickable = true
        mapView!!.setMultiTouchControls(true)
        mapView!!.setBuiltInZoomControls(false)
        mapView!!.controller.setZoom(15.0)
        mapView!!.controller.setCenter(GeoPoint(54.733334, 56.0))
        mapView!!.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        Configuration.getInstance().userAgentValue = "Chrome"
        mapView!!.setMapListener(DelayedMapListener(object : MapListener {
            override fun onZoom(e: ZoomEvent): Boolean {
                return true
            }

            override fun onScroll(e: ScrollEvent): Boolean {
                return true
            }
        }, 100))
        mapView!!.setOnClickListener {
            selectedMarkerIdx = -1
            updateLowerUI()
        }

        // Buttons
        findViewById<ImageView>(R.id.btnZoomIn).setOnClickListener {
            mapView!!.controller.zoomIn()
        }
        findViewById<ImageView>(R.id.btnZoomOut).setOnClickListener {
            mapView!!.controller.zoomOut()
        }
        findViewById<ImageView>(R.id.btnMoveToMe).setOnClickListener {
            if (locationRunning) {
                if (lastLocation == null) {
                    Toast.makeText(this, R.string.wait_location, Toast.LENGTH_LONG).show()
                } else {
                    mapView!!.controller.setCenter(GeoPoint(lastLocation!!.latitude, lastLocation!!.longitude))
                }
            } else {
                checkPermissionsState()
            }
        }
        findViewById<ImageView>(R.id.btnMoveToMarker).setOnClickListener {
            if (selectedMarkerIdx < 0 || selectedMarkerIdx > (allMarkers.size - 1)) {
                Toast.makeText(this, R.string.select_marker, Toast.LENGTH_LONG).show()
            } else {
                val loc = allMarkers[selectedMarkerIdx].loc
                mapView!!.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
            }
        }
    }

    private fun setupMarkers() {
        selectedMarkerIdx = -1
        // TODO: load data from server and show progress bar?

        // Remove Old
        for (i in 0 until mapView!!.overlays.size) {
            if (mapView!!.overlays[i] is Marker) {
                mapView!!.overlays.removeAt(i)
            }
        }

        // Add New
        val markerIcon: Drawable = ContextCompat.getDrawable(this, R.drawable.marker)!!
        var idx = 0
        for (bm: BinomMarker in allMarkers) {
            val bIcon: Drawable = BitmapDrawable(Utils.getCroppedBitmap(BitmapFactory.decodeStream(assets.open(bm.iconAsset)))) // TODO: handle load error
            val finalDrawable = LayerDrawable(arrayOf(markerIcon, bIcon))
            finalDrawable.setLayerGravity(1, Gravity.CENTER_HORIZONTAL)
            finalDrawable.setLayerWidth(1, toPx(54).toInt())
            finalDrawable.setLayerHeight(1, toPx(54).toInt())
            finalDrawable.setLayerInsetTop(1, toPx(3).toInt())
            val mk = Marker(mapView)
            mk.position = GeoPoint(bm.loc.latitude, bm.loc.longitude)
            mk.icon = finalDrawable
            mk.image = ColorDrawable(Color.TRANSPARENT)
            val idxSnapshot = idx + 0
            mk.setOnMarkerClickListener { marker, _ ->
                marker.infoWindow.close()
                selectedMarkerIdx = idxSnapshot
                updateLowerUI()
                return@setOnMarkerClickListener true
            }
            mapView!!.overlays.add(mk)
            idx += 1
        }
    }

    private fun setupCurrentLocation() {
        // Launch Process
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    lastLocation = locationResult.lastLocation
                    lastLocation?.removeBearing()
                    locConsumer?.onLocationChanged(lastLocation, this@MainActivity)
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            null
        )
        locationRunning = true

        // Marker
        val myLocBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.my_location), 100, 100, false)
        val oMapLocationOverlay = MyLocationNewOverlay(this, mapView)
        oMapLocationOverlay.isDrawAccuracyEnabled = false
        oMapLocationOverlay.setPersonIcon(myLocBitmap)
        oMapLocationOverlay.setDirectionIcon(myLocBitmap)
        oMapLocationOverlay.enableFollowLocation()
        oMapLocationOverlay.enableMyLocation()
        oMapLocationOverlay.setPersonAnchor(0.5f, 0.5f)
        oMapLocationOverlay.setDirectionAnchor(0.5f, 0.5f)
        mapView!!.overlays.add(oMapLocationOverlay)
    }

    override fun onPause() {
        super.onPause()
        if (locationRunning) {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            locationRunning = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!locationRunning && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                null
            )
            locationRunning = true
        }
    }

    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        locConsumer = myLocationConsumer
        return true
    }

    override fun stopLocationProvider() {
        locConsumer = null
    }

    override fun getLastKnownLocation(): Location {
        return lastLocation ?: Location("")
    }

    override fun destroy() {}
}