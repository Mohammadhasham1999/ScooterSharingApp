package dk.itu.moapd.scootersharing.mhas.fragments
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentUpdateRideBinding
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import dk.itu.moapd.scootersharing.mhas.services.LocationService
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val CAMERA_PERMISSION_CODE = 1

class UpdateRideFragment : Fragment() {

    private val scooterViewModel: ScooterViewModel by activityViewModels()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationService: LocationService? = LocationService()
    private lateinit var locationServiceIntent : Intent
    private var id : UUID? = null
    private var uri : String? = null
    private var photoUri : Uri? = null
    private var photoFile : File? = null
    private var photoName : String? = null
    private var lastPhoto : String? = null
    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    private val permissions : Array<String> = arrayOf(Manifest.permission.CAMERA)
    private val storage = Firebase.storage
    private var _binding: FragmentUpdateRideBinding? = null
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

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            latitude = intent.getDoubleExtra("latitude", 0.0)
            longitude = intent.getDoubleExtra("longitude", 0.0)
            setAddress(latitude,longitude)
        }
    }



    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto : Boolean ->
        if(didTakePhoto) {
            lastPhoto = photoUri.toString()
            val bitMap = BitmapFactory.decodeFile(photoFile?.path)
            binding.scooterUpdateImageView.setImageBitmap(bitMap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        var filter = IntentFilter("dk.itu.moapd.scootersharing.mhas.LOCATION_UPDATE_ACTION")
        requireContext().registerReceiver(locationReceiver,filter)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val intent = Intent(requireContext(), LocationService::class.java)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        _binding = FragmentUpdateRideBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scooterUpdateCamera.setOnClickListener {
            photoName = "IMG_${Date()}.JPG"
            photoFile = File(requireContext().applicationContext.filesDir,photoName!!)

            photoUri = FileProvider.getUriForFile(
                requireContext().applicationContext,
                "dk.itu.moapd.scootersharing.mhas.fileprovider",
                photoFile!!
            )

            checkCameraPermissionsAndOpenCamera()

        }

        scooterViewModel.scooter.observe(viewLifecycleOwner) { scooter ->
            binding.updateTextLocation.text = scooter.location
            id = scooter.id
            uri = scooter.url
        }

        binding.updateRideButton.setOnClickListener {
            if (binding.updateTextName.text.isNotEmpty() &&
                binding.updateTextLocation.text.isNotEmpty()) {
                val name = binding.updateTextName.text.toString().trim()
                val location = binding.updateTextLocation.text.toString()
                val imageReference = storage.reference.child("images/${photoUri?.lastPathSegment}")
                uri = imageReference.path

                viewLifecycleOwner.lifecycleScope.launch {
                    val foundScooter = scooterViewModel.getScooter(id!!)
                    if(foundScooter != null) {
                        val scooter = Scooter(id!!,name,location,System.currentTimeMillis(),uri!!,latitude,longitude,lastPhoto,isStarted = false, isInGeoFence = false)
                        scooterViewModel.updateScooter(scooter)
                        showMessage(scooter,view)
                    }
                }

                clearFields()
                uploadImage(imageReference)
                findNavController().navigate(UpdateRideFragmentDirections.showMainFragmentFromUpdateRideFragment())
            }
        }

        binding.cancelUpdateButton.setOnClickListener {
            findNavController().navigate(UpdateRideFragmentDirections.showMainFragmentFromUpdateRideFragment())
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

    private fun uploadImage(imagesRef : StorageReference) {

        val uploadImage = imagesRef.putFile(photoUri!!)

        uploadImage.addOnFailureListener {
            Log.e(ContentValues.TAG,"image upload failed ${it.printStackTrace()}")
        }.addOnSuccessListener {
            Log.e(ContentValues.TAG,"image uploaded successfully")
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

    private fun Address.toAddressString() : String {
        val address = this
        val stringBuilder = StringBuilder()
        stringBuilder.apply {
            append(address.getAddressLine(0))
        }

        return stringBuilder.toString()
    }

    private fun setAddress(latitude : Double, longitude : Double) {
        val geocoder = Geocoder(requireContext(),Locale.getDefault())
        if (Build.VERSION.SDK_INT >= 33) {
            val geocodeListener = Geocoder.GeocodeListener { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    binding.updateTextLocation.text = address
                }
            }
            geocoder.getFromLocation(latitude,longitude,1,geocodeListener)

        } else
            geocoder.getFromLocation(latitude,longitude,1)?.let { addresses ->
                addresses.firstOrNull()?.toAddressString()?.let { address ->
                    binding.updateTextLocation.text = address
                }
            }
    }

    private fun showMessage (scooter : Scooter, view: View) {
        val message = "Updated ${scooter.name} at ${scooter.location} on timestamp=${convertLongToTime(scooter.timestamp)}."
        Snackbar.make(view,message,Snackbar.LENGTH_SHORT).show()
    }

    private fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        return format.format(date)
    }

    private fun clearFields () {
        binding.updateTextName.text.clear()
    }

}


