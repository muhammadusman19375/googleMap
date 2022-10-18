package com.example.googlemap

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.common.api.GoogleApiActivity
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var mMapView: MapView? = null
    lateinit var mGoogleMap: GoogleMap
    private val DEFAULT_ZOOM_VALUE = 15.0F
    private var PERMISSION_REQUEST_CODE = 100
    private var PLAY_SERVICES_ERROR_CODE = 100
    private var GPS_REQUEST_CODE = 200
    private var ISLAMABAD_LAT: Double = 33.69326885464607
    private var ISLAMABAD_LNG: Double = 73.06896835270616
    private var TAG = "TAG"
    private var mLocationPermissionGranted: Boolean? = null
    private var editText: EditText? = null
    private var search: ImageButton? = null
    private lateinit var mLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingInflatedId", "VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        initGoogleMap()
        search?.setOnClickListener {
            geoLocate()
        }

        editText = findViewById(R.id.editText)
        search = findViewById(R.id.search)

        mLocationClient = FusedLocationProviderClient(this)

    }


    private fun initGoogleMap() {
        if(isServicesOk()){
            if(isGPSEnabled()){
                if(checkLocationPermission()){

                    Toast.makeText(this@MainActivity,"Ready to Map",Toast.LENGTH_SHORT).show()

                    var supportMapFragment: SupportMapFragment = supportFragmentManager
                        .findFragmentById(R.id.map_fragment_container) as SupportMapFragment
                    supportMapFragment.getMapAsync(this)
                }
                else{
                    checkPermissions()
                }
            }
        }
    }
    private fun isGPSEnabled(): Boolean {

        var locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        var providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if(providerEnabled){
            return true
        }
        else{
            var alertDialog: AlertDialog? = AlertDialog.Builder(this)
                .setTitle("GPS Permissions")
                .setMessage("GPS is required for this app to work. please enable GPS.")
                .setPositiveButton("Yes",DialogInterface.OnClickListener { dialog, which ->
                    var intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent,GPS_REQUEST_CODE)
                })
                .setNegativeButton("No",DialogInterface.OnClickListener { dialog, which ->

                })
                .setCancelable(false)
                .show()
        }

        return false
    }
    private fun isServicesOk(): Boolean {

        var googleApi: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        var result: Int = googleApi.isGooglePlayServicesAvailable(this@MainActivity)

        if(result == ConnectionResult.SUCCESS){
            return true
        }
        else if (googleApi.isUserResolvableError(result)){
            var dialog: Dialog? = googleApi.getErrorDialog(this,
                result,PLAY_SERVICES_ERROR_CODE,
                DialogInterface.OnCancelListener {
                    Toast.makeText(this@MainActivity,"Dialog is cancelled by User",Toast.LENGTH_SHORT).show()
                })
            dialog!!.show()
        }
        else{
            Toast.makeText(this@MainActivity,"Play services are required by this application",Toast.LENGTH_SHORT).show()
        }

        return false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: Map is showing on the screen")
        mGoogleMap = googleMap

//        if(checkLocationPermission()){
//            mGoogleMap.isMyLocationEnabled = true
//        }
        mGoogleMap.uiSettings.isZoomControlsEnabled = true
        mGoogleMap.uiSettings.isCompassEnabled = true

    }

    private fun geoLocate() {
        var locationName: String = editText!!.text.toString()

        var geoCoder: Geocoder = Geocoder(this, Locale.getDefault())
        try {
            var addressList: List<Address> = geoCoder.getFromLocationName(locationName,1)

            if(addressList.size > 0){
                var address: Address = addressList.get(0)
                gotoLocation(address.latitude,address.longitude)
                Log.d(TAG, "geoLocate: country "+address.locality)
            }

            for(address in addressList){
                Log.d(TAG, "geoLocate: "+address)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun gotoLocation(lat: Double,lng: Double) {
        var latlng: LatLng = LatLng(lat,lng)
        var cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng,DEFAULT_ZOOM_VALUE)
        showMarker(latlng)

        mGoogleMap.moveCamera(cameraUpdate)
        mGoogleMap.uiSettings.isScrollGesturesEnabled

    }
    private fun showMarker(latlng: LatLng) {
        var markerOptions: MarkerOptions = MarkerOptions()
            .position(latlng)
            .title("Marker in title")
        mGoogleMap.addMarker(markerOptions)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        var id = item.itemId
        when(id){
            R.id.maptype_none -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_NONE
            R.id.maptype_normal -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.maptype_satellite -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.maptype_terrain -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            R.id.maptype_hybrid -> mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.current_location -> getCurrentLocation()
        }

        return super.onOptionsItemSelected(item)
    }
    private fun getCurrentLocation() {
        if(checkLocationPermission()){
            mLocationClient.lastLocation.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    var location: Location = task.getResult()
                    gotoLocation(location.latitude,location.longitude)
                }
                else{
                    Log.d(TAG, "getCurrentLocation: Error: "+task.exception!!.message)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkPermissions() {
        if(mLocationPermissionGranted == true){
            Toast.makeText(this@MainActivity,"Ready to Map",Toast.LENGTH_SHORT).show()
        }
        else{
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),PERMISSION_REQUEST_CODE)
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)  {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionGranted = true
            Toast.makeText(this@MainActivity,"permission granted",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this@MainActivity,"permission not granted",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        var providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if(requestCode == GPS_REQUEST_CODE) {
            if (providerEnabled) {
                Toast.makeText(this@MainActivity, "GPS is enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "GPS not enabled. Unable to show user location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun floatingActionButton() {
        findViewById<FloatingActionButton>(R.id.fab_click).setOnClickListener(View.OnClickListener {
            if(mGoogleMap != null){
                var bottomBoundary: Double = ISLAMABAD_LAT - 0.1
                var leftBoundary: Double = ISLAMABAD_LNG - 0.1
                var topBoundary: Double = ISLAMABAD_LAT + 0.1
                var rightBoundary: Double = ISLAMABAD_LNG + 0.1

                var ISLAMABAD_BOUNDS = LatLngBounds(
                    LatLng(bottomBoundary,leftBoundary),
                    LatLng(topBoundary,rightBoundary)
                )
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(ISLAMABAD_BOUNDS,1))
                showMarker(ISLAMABAD_BOUNDS.center)
            }
        })
    }

}