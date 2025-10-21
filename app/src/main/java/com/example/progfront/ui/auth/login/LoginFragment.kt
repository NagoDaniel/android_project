package com.example.progfront.ui.auth.login

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
import com.example.progfront.databinding.FragmentLoginBinding
import com.example.progfront.utils.TokenManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        setupObservers()

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Log.d("LoginFragment", "Login success: ${result.data}")
                    tokenManager.saveTokens(result.data.tokens.accessToken, result.data.tokens.refreshToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is Result.Error -> {
                    showLoading(false)
                    Log.e("LoginFragment", "Login failed: ${result.message}")
                    Toast.makeText(requireContext(), "Login failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Email & password required", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("LoginFragment", "Attempting login for: $email")
        viewModel.login(email, password)
    }

    private fun showLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

