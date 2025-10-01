package com.example.progfront.ui.auth.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.data.model.RegisterRequest
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.databinding.ActivityRegisterBinding
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextPasswordConfirm.text.toString()

            if (password == confirmPassword) {
                val registerRequest = RegisterRequest(username, email, password)
                RetrofitClient.instance.registerUser(registerRequest)
                    .enqueue(object : Callback<RegisterResponse> {
                        override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                            if (response.isSuccessful) {
                                response.body()?.let { authResponse ->
                                    // Save tokens
                                    tokenManager.saveTokens(
                                        authResponse.tokens.accessToken,
                                        authResponse.tokens.refreshToken
                                    )
                                    // Registration successful
                                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                // Registration failed
                                Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                            // Network error
                            Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                // Passwords do not match, show error
                binding.editTextPassword.error = "Passwords do not match"
                binding.editTextPasswordConfirm.error = "Passwords do not match"
            }
        }
    }
}

