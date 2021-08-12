package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.GeocodingApiRequest
import com.google.maps.PlacesApi
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnPoiClickListener {

    private lateinit var map : GoogleMap

    private val REQUEST_LOCATION_PERMISSION = 1
    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    lateinit var locationRequest: LocationRequest
    private var currentMarker: Marker? = null
    private var currentPoi: PointOfInterest? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                Log.i(TAG, "Location: " + location.latitude + " " + location.longitude)
                currentMarker?.remove()

                // 구글 37.42396658875102, -122.08284734633067
                //Place current location marker
                val latLng = LatLng(location.latitude, location.longitude)
                val markerOptions = MarkerOptions()
                markerOptions.position(latLng)
                markerOptions.title("Current Position")
                currentMarker = map.addMarker(markerOptions)
                currentMarker?.showInfoWindow()

                //move map camera
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0F))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation
        val mMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mMapFragment?.getMapAsync(this)
//        TODO: zoom to the user location after taking his permission
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())


//        TODO: call this function after the user confirms on the selected location
        binding.saveBtn.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        currentPoi?.let {
            _viewModel.latitude.value = it.latLng.latitude
            _viewModel.longitude.value = it.latLng.longitude
            _viewModel.selectedPOI.value = it
            _viewModel.reminderSelectedLocationStr.value = it.name
            _viewModel.navigationCommand.value = NavigationCommand.Back
        } ?: Toast.makeText(requireContext(), getString(R.string.select_poi), Toast.LENGTH_SHORT).show()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

//        TODO: add style to the map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
        }
        map.setOnPoiClickListener(this)
        map.setOnMapLongClickListener(this)
        setMapStyle(map)
        locationRequest = LocationRequest()
        locationRequest.interval = 120000 // two minute interval
        locationRequest.fastestInterval = 120000
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //Location Permission already granted
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
                map.isMyLocationEnabled = true
            } else {
                //Request Location Permission
                checkLocationPermission()
            }
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            map.isMyLocationEnabled = true
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style)
            )
            if (!success) {
                Log.i(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.i(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION
                        )
                    }
                    .create()
                    .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.myLooper()
                        )
                        map.isMyLocationEnabled = true
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(requireContext(), "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }


    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val TAG = "SelectLocationFragment"
    }

    override fun onPoiClick(poi: PointOfInterest?) {
        poi?.let {
            currentMarker?.remove()
            MarkerOptions().apply {
                position(poi.latLng)
                title(poi.name)
            }.also {
                currentMarker = map.addMarker(it)
                currentMarker?.showInfoWindow()
                currentPoi = poi
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            fusedLocationClient.requestLocationUpdates(
                LocationRequest(), locationCallback, Looper.myLooper()
            )
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    override fun onMyLocationButtonClick(): Boolean {
        enableMyLocation()
        return true
    }

    override fun onMapLongClick(latLng: LatLng?) {
        latLng?.let {
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            binding.saveBtn.setOnClickListener {
                _viewModel.latitude.value = latLng.latitude
                _viewModel.longitude.value = latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
                _viewModel.navigationCommand.value = NavigationCommand.Back
            }

            currentMarker?.remove()
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            currentMarker?.showInfoWindow()
        }
    }
}
