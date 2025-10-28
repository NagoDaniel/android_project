package com.example.progfront.ui.auth.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.data.Result
import com.example.progfront.databinding.ActivityRegisterBinding
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var tokenManager: TokenManager
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupObservers()

        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextPasswordConfirm.text.toString()

            if (password == confirmPassword) {
                viewModel.register(username, email, password)
            } else {
                binding.editTextPassword.error = "Passwords do not match"
                binding.editTextPasswordConfirm.error = "Passwords do not match"
            }
        }
    }

    private fun setupObservers() {
        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Log.d("RegisterActivity", "Registration success: ${result.data}")
                    tokenManager.saveTokens(
                        result.data.tokens.accessToken,
                        result.data.tokens.refreshToken
                    )
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    Log.e("RegisterActivity", "Registration failed: ${result.message}")
                    Toast.makeText(this, "Registration failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.buttonRegister.isEnabled = !loading
    }
}
