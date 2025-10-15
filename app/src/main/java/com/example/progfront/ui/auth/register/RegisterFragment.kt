package com.example.progfront.ui.auth.register

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.progfront.R
import com.example.progfront.data.Result
import com.example.progfront.databinding.FragmentRegisterBinding
import com.example.progfront.utils.TokenManager

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        setupObservers()

        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextPasswordConfirm.text.toString()

            if (password == confirmPassword) {
                viewModel.register(username, email, password)
            } else {
                // Passwords do not match, show error
                binding.editTextPassword.error = "Passwords do not match"
                binding.editTextPasswordConfirm.error = "Passwords do not match"
            }
        }

        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun setupObservers() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Log.d("RegisterFragment", "Registration success: ${result.data}")
                    tokenManager.saveTokens(
                        result.data.tokens.accessToken,
                        result.data.tokens.refreshToken
                    )
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
                is Result.Error -> {
                    showLoading(false)
                    Log.e("RegisterFragment", "Registration failed: ${result.message}")
                    Toast.makeText(requireContext(), "Registration failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.buttonRegister.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

