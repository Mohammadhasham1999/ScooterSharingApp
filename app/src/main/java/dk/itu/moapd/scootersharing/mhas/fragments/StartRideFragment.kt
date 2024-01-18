package dk.itu.moapd.scootersharing.mhas.fragments
import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentStartRideBinding
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import dk.itu.moapd.scootersharing.mhas.services.LocationService
import dk.itu.moapd.scootersharing.mhas.utils.LocationHelper
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

private const val CAMERA_PERMISSION_CODE = 1

class StartRideFragment : Fragment() {

    private val scooterViewModel: ScooterViewModel by activityViewModels()
    private val storage = Firebase.storage
    private lateinit var auth: FirebaseAuth
    private var locationService: LocationService? = LocationService()
    private lateinit var locationServiceIntent : Intent
    private val permissions : Array<String> = arrayOf(Manifest.permission.CAMERA)
    private var photoUri : Uri? = null
    private var photoFile : File? = null
    private var photoName : String? = null
    private var lastPhoto : String? = null
    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var _binding: FragmentStartRideBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            locationService = (service as LocationService.LocationServiceBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
        }

    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto : Boolean ->
        if(didTakePhoto) {
            lastPhoto = photoUri.toString()
            val bitMap = BitmapFactory.decodeFile(photoFile?.path)
            binding.scooterImageView.setImageBitmap(bitMap)
        }
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            latitude = intent.getDoubleExtra("latitude", 0.0)
            longitude = intent.getDoubleExtra("longitude", 0.0)
            setAddress(latitude,longitude)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        var filter = IntentFilter("dk.itu.moapd.scootersharing.mhas.LOCATION_UPDATE_ACTION")
        requireContext().registerReceiver(locationReceiver,filter)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val intent = Intent(requireContext(), LocationService::class.java)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        _binding = FragmentStartRideBinding.inflate(inflater,container,false)

        return binding.root
    }

    /**
     * Responsible for binding the values to the variables name and location
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scooterCamera.setOnClickListener {
            photoName = "IMG_${Date()}.JPG"
            photoFile = File(requireContext().applicationContext.filesDir,photoName!!)

            photoUri = FileProvider.getUriForFile(
                requireContext().applicationContext,
                "dk.itu.moapd.scootersharing.mhas.fileprovider",
                photoFile!!
            )

            checkCameraPermissionsAndOpenCamera()

        }

        binding.doneButton.setOnClickListener {
            if (binding.editTextName.text.isNotEmpty() &&
                binding.editTextLocation.text.isNotEmpty()) {
                val name = binding.editTextName.text.toString().trim()
                val location = binding.editTextLocation.text.toString()  //locationService?.getAddressAsString()!!
                val imageRef = storage.reference.child("images/${photoUri?.lastPathSegment}")
                val uri = imageRef.path
                val scooter = Scooter(UUID.randomUUID(),name,location,System.currentTimeMillis(),uri,latitude,longitude,lastPhoto,isStarted = false, isInGeoFence = false)

                viewLifecycleOwner.lifecycleScope.launch {
                    scooterViewModel.addScooter(scooter)
                    showMessage(scooter,view)
                    findNavController().navigate(StartRideFragmentDirections.showMainFragmentFromStartRideFragment())

                    clearFields()
                }

                uploadImage()
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigate(StartRideFragmentDirections.showMainFragmentFromStartRideFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        locationServiceIntent = Intent(requireContext(),LocationService::class.java)
        requireContext().startService(locationServiceIntent)
    }

    override fun onPause() {
        super.onPause()
        requireContext().stopService(locationServiceIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().unbindService(serviceConnection)

        _binding = null

        requireContext().unregisterReceiver(locationReceiver)
    }

    private fun Address.toAddressString() : String {
        val address = this
        val stringBuilder = StringBuilder()
        stringBuilder.apply {
            append(address.getAddressLine(0))
        }

        return stringBuilder.toString()
    }

    fun setAddress(latitude : Double, longitude : Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        if (Build.VERSION.SDK_INT >= 33) {
            val geocodeListener = Geocoder.GeocodeListener { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    binding.editTextLocation.text = address
                }
            }
            geocoder.getFromLocation(latitude,longitude,1,geocodeListener)

        } else
            geocoder.getFromLocation(latitude,longitude,1)?.let { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    binding.editTextLocation.text = address
                }
            }
    }

    private fun uploadImage() {
        val imagesRef = storage.reference.child("images/${photoUri?.lastPathSegment}")

        val uploadImage = imagesRef.putFile(photoUri!!)

        uploadImage.addOnFailureListener {
            Log.e(TAG,"image upload failed ${it.printStackTrace()}")
        }.addOnSuccessListener {
            Log.e(TAG,"image uploaded successfully")
        }
    }

    private fun checkCameraPermissionsAndOpenCamera() {
        if(ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                permissions,CAMERA_PERMISSION_CODE)
        } else {
            takePhoto.launch(photoUri)
        }
    }

    /**
     * Prints the content of the toString method in the debugging console
     * @param scooter is the instance that is given to the function with name and location
     */
    private fun showMessage (scooter : Scooter, view: View) {
        val message = "Ride started for ${scooter.name}\n at ${scooter.location}\n on ${convertLongToTime(scooter.timestamp)}."
        Snackbar.make(view,message,Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Converts the timestamp in Long to actual DateTime as formatted on line 82
     */

    @SuppressLint("SimpleDateFormat")
    private fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        return format.format(date)
    }

    /**
     * Clears the text fields of both the name and location of the scooter
     */
    private fun clearFields () {
        binding.editTextName.text.clear()
    }

}


