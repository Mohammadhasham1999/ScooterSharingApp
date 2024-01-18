package dk.itu.moapd.scootersharing.mhas.fragments

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.zxing.integration.android.IntentIntegrator
import dk.itu.moapd.scootersharing.mhas.R
import dk.itu.moapd.scootersharing.mhas.broadcasts.GeofenceBroadcastReceiver
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentScooterDetailsBinding
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.launch
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import java.util.*

class ScooterDetailsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap : GoogleMap
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var auth: FirebaseAuth
    private lateinit var geofenceReceiver: BroadcastReceiver
    private var isInGeofence : Boolean = false
    private val scooterViewModel: ScooterViewModel by activityViewModels()
    private var _binding: FragmentScooterDetailsBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
    }


    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data != null) {
            val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
            if (intentResult?.contents != null) {
                scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (intentResult.contents == scooter.name) {
                            if(!scooter.isStarted) {
                                scooter.isStarted = true
                                scooter.userId = auth.currentUser?.uid
                            }
                            updateScooter(scooter)
                        } else {
                            Toast.makeText(requireContext(),"Cannot start ride",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "No data", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        geofencingClient = LocationServices.getGeofencingClient(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentScooterDetailsBinding.inflate(inflater,container,false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var filter = IntentFilter("dk.itu.moapd.scootersharing.mhas.ACTION_GEOFENCE_UPDATE")

        geofenceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isInGeofence = intent.getBooleanExtra("inGeoFence", false)
                scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
                    scooter.isInGeoFence = isInGeofence
                    if (scooter.isInGeoFence) {
                        Toast.makeText(requireContext(),"${scooter.name} in geofence zone",Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(),"${scooter.name} left the geofence zone",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(geofenceReceiver,filter)

        scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
            Log.d("test","${scooter.isStarted}")
            binding.editScooterName.setText(scooter.name)
            binding.editScooterName.isFocusable = false
            binding.editScooterName.setBackgroundResource(android.R.color.transparent)
            binding.scooterLocationTextView.text = scooter.location

            if(scooter.isStarted) {
                binding.scooterStartButton.text = getString(R.string.end_ride_button_text)
                binding.scooterStartButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
            }

        }

        binding.scooterStartButton.setOnClickListener {
            scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
                if(!scooter.isInGeoFence) {
                    if (!scooter.isStarted) {
                        launchScanner()
                    } else {
                        binding.scooterStartButton.text = getString(R.string.start_ride_button_text)
                        binding.scooterStartButton.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
                        scooter.isStarted = false
                        scooter.userId = null
                        updateScooter(scooter)
                    }
                } else {
                    Toast.makeText(requireContext(),"Cannot start/end ride because ${scooter.name} is in the geofence",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.floatingBackButton.setOnClickListener {
            findNavController().navigate(ScooterDetailsFragmentDirections.showMainFragmentFromScooterDetailsFragment())
        }

        binding.editScooterButton.setOnClickListener {
            findNavController().navigate(ScooterDetailsFragmentDirections.showUpdateScooterFragmentFromScooterDetailsFragment())
        }

    }

    private fun launchScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setPrompt("Scan a QR code")
        integrator.setOrientationLocked(false)
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        launcher.launch(integrator.createScanIntent())
    }

    private fun updateScooter(scooter : Scooter) {
        viewLifecycleOwner.lifecycleScope.launch {
            scooterViewModel.updateScooter(scooter)
        }
        findNavController().navigate(ScooterDetailsFragmentDirections.showMainFragmentFromScooterDetailsFragment())
    }

    override fun onMapReady(googleMap: GoogleMap) {

        if (checkPermission()) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                1)
        } else {
            mMap = googleMap

            // Add a marker for the user location and the scooter location and move the camera

            scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
                val latitude = scooter.latitude
                val longitude = scooter.longitude
                val scooterLocation = LatLng(latitude, longitude)

                mMap.addMarker(
                    MarkerOptions()
                    .position(scooterLocation)
                    .title(scooter.location))
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                mMap.isMyLocationEnabled = true
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scooterLocation,12f))

            }

            val geofence = Geofence.Builder()
                .setRequestId("myGeofenceId")
                .setCircularRegion(55.729510, 12.342960, 30f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(1000)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()


            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG,"Geofence(s) added")
                }
                addOnFailureListener {
                    Log.e(TAG,"Failed to add geofence(s) ${it.printStackTrace()}")
                }
                addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Geofence(s) added successfully.")
                    } else {
                        Log.e(TAG, "Failed to add geofence(s): ${task.exception?.message}")
                    }
                }
            }

            drawGeoFenceCircle(mMap)
        }
    }

    private fun drawGeoFenceCircle(mMap : GoogleMap) {
        val circleOptions = CircleOptions()
            .center(LatLng(55.729510, 12.342960))
            .radius(30.0)
            .strokeColor(Color.RED)
            .strokeWidth(4.0f)
            .fillColor(Color.argb(32,255,0,0))

        mMap.addCircle(circleOptions)
    }

    private fun checkPermission(): Boolean {
        val foregroundPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val backgroundPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        return foregroundPermission && backgroundPermission
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG,"Geofence(s) removed successfully")
            }
            addOnFailureListener {
                Log.e(TAG,"Geofence(s) couldn't be removed ${it.printStackTrace()}")
            }
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(geofenceReceiver)

    }
}