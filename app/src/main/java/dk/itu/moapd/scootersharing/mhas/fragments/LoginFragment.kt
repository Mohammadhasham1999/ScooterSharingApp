package dk.itu.moapd.scootersharing.mhas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.scootersharing.mhas.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentLoginBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {

            val email = binding.emailText.text.toString()

            //Potential security breach --> to be fixed
            val password = binding.passwordText.text.toString()

            if(email.isNotEmpty() || password.isNotEmpty()) {

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            Toast.makeText(requireContext(),"User $user logged in successfully!",Toast.LENGTH_SHORT).show()
                            findNavController().navigate(LoginFragmentDirections.showMainFragment())
                        } else {
                            Toast.makeText(requireContext(),"Couldn't log in!",Toast.LENGTH_SHORT).show()
                            binding.emailText.text.clear()
                            binding.passwordText.text.clear()
                        }
                    }


            } else {
                binding.emailText.error = "invalid email/password!"
                binding.passwordText.error = "invalid email/password!"
            }

        }


        binding.signUpButton.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.showSignUpFragment())
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}