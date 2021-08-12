package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private var geofence: Geofence? = null
    private var geofenceSetting: SettingsClient? = null
    private var geofenceClient: GeofencingClient? = null
    private var geofencePendingIntent: PendingIntent? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        inflater.context?.let {
            geofenceSetting = LocationServices.getSettingsClient(it)
            geofenceClient = LocationServices.getGeofencingClient(it)
            val intent = Intent(it, GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT
            // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            geofencePendingIntent = PendingIntent.getBroadcast(it, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            _viewModel.validateAndSaveReminder(
                ReminderDataItem(
                    title = title,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            _viewModel.savedReminder.observe(requireActivity(), Observer { item ->
                geofence = Geofence.Builder()
                    .setRequestId(item.id)
                    .setCircularRegion(
                        item.latitude!!,
                        item.longitude!!,
                        GEOFENCE_RADIUS_IN_METERS
                    )
                    .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
                checkDeviceLocationSettingsAndStartGeofence()
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val locationSettingsResponseTask =
            geofenceSetting?.checkLocationSettings(builder.build())
        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.activityMain,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask?.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        if (geofence == null) return
        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        geofenceClient?.addGeofences(geofenceRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.d(TAG, "Add Geofence : ${geofence!!.requestId}")
            }
            addOnFailureListener {
                if ((it.message != null)) {
                    Log.w(TAG, it.message.toString())
                }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"
    }
}

private const val REQUEST_TURN_DEVICE_LOCATION_ON = 290
private const val TAG = "SaveReminderFragment"
private const val GEOFENCE_RADIUS_IN_METERS = 100f
val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)