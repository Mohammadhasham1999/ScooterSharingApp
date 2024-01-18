package dk.itu.moapd.scootersharing.mhas.broadcasts

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.widget.EdgeEffectCompat.getDistance
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class GeofenceBroadcastReceiver : BroadcastReceiver()  {

    private fun BroadcastReceiver.goAsync(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch(context) {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) = goAsync {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError()!!) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return@goAsync
        }
        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            for(geofence in triggeringGeofences!!) {
                if(geofence.requestId == "myGeofenceId") {
                    val intent = Intent("dk.itu.moapd.scootersharing.mhas.ACTION_GEOFENCE_UPDATE").apply {
                        putExtra("inGeoFence",true)
                    }
                    val localBroadcastManager = LocalBroadcastManager.getInstance(context)
                    localBroadcastManager.sendBroadcast(intent)
                }

            }

        } else if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            for(geofence in triggeringGeofences!!) {
                if(geofence.requestId == "myGeofenceId") {
                    val intent = Intent("dk.itu.moapd.scootersharing.mhas.ACTION_GEOFENCE_UPDATE").apply {
                        putExtra("inGeoFence",false)
                    }
                    val localBroadcastManager = LocalBroadcastManager.getInstance(context)
                    localBroadcastManager.sendBroadcast(intent)
                }
            }
        }
    }
}