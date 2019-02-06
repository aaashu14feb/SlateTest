package com.slatetest

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.slatetest.Constants.BROADCAST_ACTION
import com.slatetest.Constants.BROADCAST_TASK_ENTER
import com.slatetest.Constants.KEY_GEOFENCE_LAT
import com.slatetest.Constants.KEY_GEOFENCE_LON
import com.slatetest.Constants.KEY_GEOFENCE_NAME
import com.slatetest.Constants.KEY_GEOFENCE_RADIUS
import com.slatetest.model.Geofence


class HomeActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, ResultCallback<Status>, GeoInputDialog.AddGeofenceListener {

    private var map: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private var lastLocation: Location? = null

    private var tvLat: TextView? = null
    private var tvLon: TextView? = null
    private var tvStatus: TextView? = null
    private var mapFragment: MapFragment? = null

    private var geofence: Geofence? = null

    private var brGeofence: BroadcastReceiver? = null
    private var brNetwork: BroadcastReceiver? = null
    private var isInsideArea = false
    private var isConnectWiFi = false

    // Draw Geofence circle on GoogleMap
    private var geoFenceLimits: Circle? = null

    private var locationRequest: LocationRequest? = null

    private var locationMarker: Marker? = null

    private var geoFenceMarker: Marker? = null

    private var geoFencePendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvLat = findViewById(R.id.tvLat)
        tvLon = findViewById(R.id.tvLon)
        tvStatus = findViewById(R.id.tvStatus)

        // initialize GoogleMaps
        initGMaps()

        // create GoogleApiClient
        createGoogleApi()

        brGeofence = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isInsideArea = intent.getBooleanExtra(BROADCAST_TASK_ENTER, false)
                Log.d(TAG, "onReceive: isInsideArea = $isInsideArea")
                checkStatus()
            }
        }

        brNetwork = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                checkNetwork()
            }
        }
    }

    private fun checkNetwork() {
        val info = getNetworkInfo(this)
        Log.e(TAG, "getNetworkInfo " + info!!.toString())
        if (info != null && info.isConnected) {
            isConnectWiFi = false
            val wifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid.replace("\"", "")
            if (geofence != null)
                if (ssid == geofence!!.name) {
                    isConnectWiFi = true
                }
        } else {
            isConnectWiFi = false
        }
        checkStatus()
    }

    // Create GoogleApiClient instance
    private fun createGoogleApi() {
        Log.d(TAG, "createGoogleApi()")
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }
    }


    private fun checkStatus() {
        if (isConnectWiFi || isInsideArea) {
            setEnterStatus()
        } else if (isConnectWiFi) {
            setEnterStatus()
        } else {
            setExistStatus()
        }
    }

    private fun setEnterStatus() {
        tvStatus?.text = (getString(R.string.inside_in_area));
        tvStatus?.setBackgroundColor(resources.getColor(R.color.status_enter));
    }

    private fun setExistStatus() {
        tvStatus?.text = (getString(R.string.not_inside_in_area));
        tvStatus?.setBackgroundColor(resources.getColor(R.color.status_exit));
    }


    override fun onStart() {
        super.onStart()

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient!!.connect()

        val intFilt = IntentFilter(BROADCAST_ACTION)
        registerReceiver(brGeofence, intFilt)

        registerReceiver(brNetwork, IntentFilter("android.net.wifi.STATE_CHANGE")) // IntentFilter to wifi state change is "android.net.wifi.STATE_CHANGE"
        checkNetwork()
    }

    override fun onStop() {
        super.onStop()

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient!!.disconnect()

        //unregister entering receiver
        unregisterReceiver(brGeofence)
        unregisterReceiver(brNetwork)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.creategeofence -> {
                getGeofenceCoordinates()
                return true
            }
            R.id.clear -> {
                clearGeofence()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    // Check for permission to access Location
    private fun checkPermission(): Boolean {
        Log.d(TAG, "checkPermission()")
        // Ask for permission if it wasn't granted yet
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    // Asks for permission
    private fun askPermission() {
        Log.d(TAG, "askPermission()")
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_ACCESS_FINE_LOCATION
        )
    }

    // Verify user's response of the permission requested
    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_ACCESS_FINE_LOCATION -> {
                when {
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> getLastKnownLocation()
                    else -> permissionsDenied()
                }
            }
        }
    }

    // App cannot work without the permissions
    private fun permissionsDenied() {
        Log.w(TAG, "permissionsDenied()")
    }

    // Initialize GoogleMaps
    private fun initGMaps() {
        mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment?
        mapFragment!!.getMapAsync(this)
    }

    // Callback called when Map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady()")
        map = googleMap
        map!!.setOnMapClickListener(this)
        map!!.setOnMarkerClickListener(this)
    }

    override fun onMapClick(latLng: LatLng) {
        Log.d(TAG, "onMapClick($latLng)")
        //        markerForGeofence(latLng);
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.d(TAG, "onMarkerClickListener: " + marker.position)
        return false
    }


    // Start location Updates
    private fun startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()")
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Companion.UPDATE_INTERVAL.toLong())
                .setFastestInterval(Companion.FASTEST_INTERVAL.toLong())

        if (checkPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged [$location]")
        lastLocation = location
        writeActualLocation(location)
    }

    // GoogleApiClient.ConnectionCallbacks connected
    override fun onConnected(@Nullable bundle: Bundle?) {
        Log.i(TAG, "onConnected()")
        getLastKnownLocation()
        recoverGeofenceMarker()
        checkNetwork()
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    override fun onConnectionSuspended(i: Int) {
        Log.w(TAG, "onConnectionSuspended()")
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        Log.w(TAG, "onConnectionFailed()")
    }

    // Get last known location
    private fun getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()")
        if (checkPermission()) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if (lastLocation != null) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation!!.longitude +
                        " | Lat: " + lastLocation!!.latitude)
                writeLastLocation()
                startLocationUpdates()
            } else {
                Log.w(TAG, "No location retrieved yet")
                startLocationUpdates()
            }
        } else
            askPermission()
    }

    private fun writeActualLocation(location: Location) {
        tvLat!!.text = "Lat: " + location.latitude
        tvLon!!.text = "Long: " + location.longitude

        markerLocation(LatLng(location.latitude, location.longitude))
    }

    private fun writeLastLocation() {
        writeActualLocation(lastLocation!!)
    }

    private fun markerLocation(latLng: LatLng) {
        Log.i(TAG, "markerLocation($latLng)")
        val title = latLng.latitude.toString() + ", " + latLng.longitude
        val markerOptions = MarkerOptions()
                .position(latLng)
                .title(title)
        if (map != null) {
            if (locationMarker != null)
                locationMarker!!.remove()
            locationMarker = map!!.addMarker(markerOptions)
        }
    }

    private fun markerForGeofence(latLng: LatLng) {
        Log.i(TAG, "markerForGeofence($latLng)")
        val title = geofence!!.name//latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title)
        if (map != null) {
            // Remove last geoFenceMarker
            if (geoFenceMarker != null)
                geoFenceMarker!!.remove()

            geoFenceMarker = map!!.addMarker(markerOptions)

        }
    }

    private fun getGeofenceCoordinates() {
        GeoInputDialog().apply {
            this.setListener(this@HomeActivity)
            this.geofence = geofence
        }.show(supportFragmentManager, "GeoInputDialog")
    }


    // Start Geofence creation process
    private fun startGeofence() {
        Log.i(TAG, "startGeofence()")
        if (geoFenceMarker != null) {
            isConnectWiFi = false
            isInsideArea = false
            checkStatus()

            val geofence = createGeofence(geoFenceMarker!!.position, this.geofence!!.radius * 1000.0f)
            val geofenceRequest = createGeofenceRequest(geofence)
            addGeofence(geofenceRequest)
        } else {
            Log.e(TAG, "Geofence marker is null")
        }
    }

    // Create a Geofence
    private fun createGeofence(latLng: LatLng, radius: Float): com.google.android.gms.location.Geofence {
        Log.d(TAG, "createGeofence")
        return com.google.android.gms.location.Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER or com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
    }

    // Create a Geofence Request
    private fun createGeofenceRequest(geofence: com.google.android.gms.location.Geofence): GeofencingRequest {
        Log.d(TAG, "createGeofenceRequest")
        return GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build()
    }

    private fun createGeofencePendingIntent(): PendingIntent? {
        Log.d(TAG, "createGeofencePendingIntent")
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent

        val intent = Intent(this, GeofenceTransitionService::class.java)
        geoFencePendingIntent = PendingIntent.getService(
                this, Companion.GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return geoFencePendingIntent
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private fun addGeofence(request: GeofencingRequest) {
        Log.d(TAG, "addGeofence")
        if (checkPermission()) {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this)
        }
    }

    override fun onResult(@NonNull status: Status) {
        Log.i(TAG, "onResult: $status")
        if (status.isSuccess) {
            saveGeofence()
            drawGeofence()
        } else {
            showSnackbar("Error")
        }
    }

    private fun showSnackbar(text: String) {
        findViewById<View>(android.R.id.content)?.let { view ->
            Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun drawGeofence() {
        Log.d(TAG, "drawGeofence()")
        if (geoFenceLimits != null)
            geoFenceLimits!!.remove()

        val circleOptions = CircleOptions()
                .center(geoFenceMarker!!.position)
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius((geofence!!.radius * 1000.0f).toDouble())
        geoFenceLimits = map!!.addCircle(circleOptions)
    }

    // Saving GeoFence marker with prefs mng
    private fun saveGeofence() {
        Log.d(TAG, "saveGeofence()")
        getPreferences(Context.MODE_PRIVATE).edit().apply{
            putString(KEY_GEOFENCE_NAME, geofence!!.name)
            putFloat(KEY_GEOFENCE_RADIUS, geofence!!.radius)
            putLong(KEY_GEOFENCE_LAT, java.lang.Double.doubleToRawLongBits(geoFenceMarker!!.position.latitude))
            putLong(KEY_GEOFENCE_LON, java.lang.Double.doubleToRawLongBits(geoFenceMarker!!.position.longitude))
        }.apply()
    }

    // Removing GeoFence marker with prefs mng
    private fun removeGeofence() {
        Log.d(TAG, "removeGeofence()")
        getPreferences(Context.MODE_PRIVATE).edit().clear().apply()
    }


    // Recovering last Geofence marker
    private fun recoverGeofenceMarker() {
        Log.d(TAG, "recoverGeofenceMarker")
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        if (sharedPref.contains(KEY_GEOFENCE_LAT) && sharedPref.contains(KEY_GEOFENCE_LON)) {
            val lat = java.lang.Double.longBitsToDouble(sharedPref.getLong(KEY_GEOFENCE_LAT, -1))
            val lon = java.lang.Double.longBitsToDouble(sharedPref.getLong(KEY_GEOFENCE_LON, -1))
            val latLng = LatLng(lat, lon)
            geofence = Geofence().apply {
                longitude = (lon)
                latitude = (lat)
                name = (sharedPref.getString(KEY_GEOFENCE_NAME, ""))
                radius = (sharedPref.getFloat(KEY_GEOFENCE_RADIUS, 0.01f))
            }
            markerForGeofence(latLng)
            drawGeofence()
            startGeofence()
        }
    }

    // Clear Geofence
    private fun clearGeofence() {
        Log.d(TAG, "clearGeofence()")
        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                createGeofencePendingIntent()
        ).setResultCallback { status ->
            if (status.isSuccess) {
                removeGeofenceDraw()
            }
        }
    }

    private fun removeGeofenceDraw() {
        Log.d(TAG, "removeGeofenceDraw()")
        geoFenceMarker?.remove()
        geoFenceLimits?.remove()
        if (geofence != null)
            geofence = null
        removeGeofence()
        isConnectWiFi = false
        isInsideArea = false
        checkStatus()
    }

    private fun getNetworkInfo(context: Context): NetworkInfo? {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
    }

    override fun onDialogPositiveClick(dialog: androidx.fragment.app.DialogFragment, geofence: Geofence) {
        this.geofence = geofence
        markerForGeofence(LatLng(geofence.latitude, geofence.longitude))
        startGeofence()
        checkNetwork()
    }

    override fun onDialogNegativeClick(dialog: androidx.fragment.app.DialogFragment) {
    }

    companion object {
        private val TAG = HomeActivity::class.java.simpleName
        private const val GEO_DURATION = (60 * 60 * 1000).toLong()
        private const val GEOFENCE_REQ_ID = "My Geofence"

        private const val REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
        // Defined in mili seconds.
        // This number in extremely low, and should be used only for debug
        private val UPDATE_INTERVAL = 1000
        private val FASTEST_INTERVAL = 900
        private val GEOFENCE_REQ_CODE = 0
    }
}