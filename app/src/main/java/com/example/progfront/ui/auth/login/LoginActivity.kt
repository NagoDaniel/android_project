package com.example.progfront.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.R
import com.example.progfront.data.Result
import com.example.progfront.databinding.ActivityLoginBinding
import com.example.progfront.ui.auth.register.RegisterActivity
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager

class LoginActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupObservers()

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    Log.d("LoginActivity", "Login success: ${result.data}")
                    tokenManager.saveTokens(result.data.tokens.accessToken, result.data.tokens.refreshToken)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    Log.e("LoginActivity", "Login failed: ${result.message}")
                    Toast.makeText(this, "Login failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email & password required", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("LoginActivity", "Attempting login for: $email")
        viewModel.login(email, password)
    }

    private fun showLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
        // You can add a ProgressBar to your layout and show/hide it here
    }
}
