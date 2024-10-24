package com.pm.binomtech_test

import android.Manifest
import android.content.pm.*
import android.location.*
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import org.osmdroid.config.Configuration
import org.osmdroid.events.*
import org.osmdroid.tileprovider.tilesource.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.*
import org.osmdroid.views.overlay.mylocation.*

class MainActivity : ComponentActivity(), IMyLocationProvider {
    private var mapView: MapView? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var locationRunning = false
    private var locConsumer: IMyLocationConsumer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationRunning = false
        setupMap()
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
        mapView = findViewById(R.id.mapview)
        mapView!!.isClickable = true
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
        val oMapLocationOverlay = MyLocationNewOverlay(this, mapView)
        oMapLocationOverlay.enableFollowLocation()
        oMapLocationOverlay.enableMyLocation()
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