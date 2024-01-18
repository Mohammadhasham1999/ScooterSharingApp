package dk.itu.moapd.scootersharing.mhas.services

import android.Manifest
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.location.*
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentStartRideBinding
import dk.itu.moapd.scootersharing.mhas.utils.LocationHelper
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.e(TAG,"Entered onLocationResult")
            super.onLocationResult(locationResult)
            locationResult.lastLocation.let { location ->
                if(location != null) {
                    val intent = Intent("dk.itu.moapd.scootersharing.mhas.LOCATION_UPDATE_ACTION")
                    intent.putExtra("latitude",location.latitude)
                    intent.putExtra("longitude",location.longitude)
                    sendBroadcast(intent)
                }
            }
        }
    }

    inner class LocationServiceBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private val binder = LocationServiceBinder()


    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        subscribeToLocationUpdates()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        unsubscribeToLocationUpdates()
        super.onDestroy()
    }


    private fun subscribeToLocationUpdates() {
        if(checkPermission()) {
            return
        }

        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY,5)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,locationCallback, Looper.getMainLooper())

    }

    private fun unsubscribeToLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun checkPermission() : Boolean {
        return ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }
}
