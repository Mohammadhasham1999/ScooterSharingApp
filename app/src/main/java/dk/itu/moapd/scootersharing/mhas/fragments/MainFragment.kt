package dk.itu.moapd.scootersharing.mhas.fragments
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.scootersharing.mhas.R
import dk.itu.moapd.scootersharing.mhas.adapters.RidesArrayAdapter
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentMainBinding
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import dk.itu.moapd.scootersharing.mhas.viewmodels.ScooterViewModel
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class MainFragment : Fragment() {

    /**
     * Defining the binding for the UI components in fragment_main
     */


    private val scooterViewModel: ScooterViewModel by activityViewModels()
    private lateinit var adapter : RidesArrayAdapter
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentMainBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

    }

    /**
     * Creates the view which is bound to
     * @return the root element of the binding which is the FragmentContainerView
     */


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentMainBinding.inflate(inflater,container,false)

        return binding.root

    }


    /**
     * Defines the actions once the view is created
     * Provides two navigations: one to the start ride fragment and one to the update ride fragment
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listRidesView.layoutManager = LinearLayoutManager(context)

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                scooterViewModel.scooters.collect { scooters ->

                    adapter = RidesArrayAdapter(requireContext(),scooters)

                    binding.listRidesView.adapter = adapter

                    showScooterDetailsOnClick(adapter,scooters)

                }
            }
        }

        //Creates menuHost for menu item(s)
        createMenuHost()

        binding.startRideButton.setOnClickListener {
            requestUserPermissions()
            findNavController().navigate(MainFragmentDirections.showStartRideFragment())

        }

        binding.listScootersButton.setOnClickListener {
            requestUserPermissions()
            findNavController().navigate(MainFragmentDirections.showScooters())
        }

        binding.floatingAddButton.setOnClickListener {
            findNavController().navigate(MainFragmentDirections.showStartRideFragment())

        }

    }

    private fun createMenuHost() {
        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if(menu.hasVisibleItems()) return

                menuInflater.inflate(R.menu.sign_out,menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sign_out_button -> {
                        auth.signOut()
                        findNavController().navigate(MainFragmentDirections.showLoginFragmentFromMainFragment())
                        true
                    } else -> false               }
            }

            override fun onMenuClosed(menu: Menu) {
                super.onMenuClosed(menu)
                menu.clear()
            }
        })
    }

    private fun requestUserPermissions() {
        val permissions : ArrayList<String> = ArrayList()
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionsToRequest = permissionsToRequest(permissions)

        if (permissionsToRequest.size > 0)
            ActivityCompat.requestPermissions(requireActivity(),permissionsToRequest.toTypedArray(),1011)
    }

    private fun permissionsToRequest(permissions : ArrayList<String>) : ArrayList<String> {
        val result : ArrayList<String> = ArrayList()

        for (permission in permissions)
            if (checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED)
                result.add(permission)

        return result
    }

    private fun showScooterDetailsOnClick(adapter: RidesArrayAdapter, scooters: List<Scooter>){
        adapter.setOnItemClickListener(object : RidesArrayAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val scooter = scooters[position]
                /* AlertDialog.Builder(requireContext())
                    .setMessage("Are you sure you want to delete ${scooter.name}?")
                    .setPositiveButton("Yes", DialogInterface.OnClickListener {
                            _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            scooterViewModel.deleteScooter(scooter)
                        }
                    })
                    .setNegativeButton("No",DialogInterface.OnClickListener {
                            dialog, _ -> dialog.cancel()
                    })
                    .show() */

                if(scooter.userId == auth.currentUser?.uid || !scooter.isStarted) {
                    scooterViewModel.selectScooter(scooter)
                    findNavController().navigate(MainFragmentDirections.showScooterDetailsFragmentFromMainFragment())
                } else {
                    Toast.makeText(requireContext(),"${scooter.name} is currently in ride. Please try again later",Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Destroys the created view and makes the binding nullable
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}


