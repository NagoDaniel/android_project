package com.example.progfront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.network.RegisterRequest
import com.example.progfront.network.RegisterResponse
import com.example.progfront.network.RetrofitClient
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tokenManager = TokenManager(this)

        val usernameEditText = findViewById<EditText>(R.id.editTextUsername)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val passwordConfirmEditText = findViewById<EditText>(R.id.editTextPasswordConfirm)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = passwordConfirmEditText.text.toString()

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
                passwordEditText.error = "Passwords do not match"
                passwordConfirmEditText.error = "Passwords do not match"
            }
        }
    }
}
