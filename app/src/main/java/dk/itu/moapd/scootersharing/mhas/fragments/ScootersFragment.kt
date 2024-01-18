package dk.itu.moapd.scootersharing.mhas.fragments

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dk.itu.moapd.scootersharing.mhas.R
import dk.itu.moapd.scootersharing.mhas.broadcasts.GeofenceBroadcastReceiver
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentScootersBinding
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.launch

class ScootersFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap : GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private val scooterViewModel: ScooterViewModel by activityViewModels()
    private var _binding: FragmentScootersBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentScootersBinding.inflate(inflater,container,false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.floatingBackButton.setOnClickListener {
            findNavController().navigate(ScootersFragmentDirections.showMainFragment())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        if (checkPermission()) {
            return
        } else {
            mMap = googleMap

            viewLifecycleOwner.lifecycleScope.launch {

                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    scooterViewModel.scooters.collect { scooters ->
                        scooters.forEach { scooter ->
                            val latitude = scooter.latitude
                            val longitude = scooter.longitude
                            val scooterLocation = LatLng(latitude, longitude)

                            mMap.addMarker(
                                MarkerOptions()
                                    .position(scooterLocation)
                                    .title(scooter.name))
                            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            mMap.isMyLocationEnabled = true
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scooterLocation,12f))


                        }
                    }
                }
            }

            val geofence = Geofence.Builder()
                .setRequestId("myGeofenceId")
                .setCircularRegion(55.729510, 12.342960, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()


            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.e(TAG,"Geofence(s) added")
                }
                addOnFailureListener {
                    Log.e(TAG,"Failed to add geofence(s) ${it.printStackTrace()}")
                }
            }

            drawGeoFenceCircle(mMap)
        }
    }

    private fun drawGeoFenceCircle(mMap : GoogleMap) {
        val circleOptions = CircleOptions()
            .center(LatLng(55.729510, 12.342960))
            .radius(100.0)
            .strokeColor(Color.RED)
            .strokeWidth(4.0f)
            .fillColor(Color.argb(32,255,0,0))

        mMap.addCircle(circleOptions)
    }

    private fun checkPermission() : Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.e(TAG,"Geofence(s) removed successfully")
            }
            addOnFailureListener {
                Log.e(TAG,"Geofence(s) couldn't be removed ${it.printStackTrace()}")
            }
        }
    }
}