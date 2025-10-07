package com.example.progfront.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.R
import com.example.progfront.data.model.LoginRequest
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.databinding.ActivityLoginBinding
import com.example.progfront.ui.auth.register.RegisterActivity
import com.example.progfront.ui.main.MainActivity
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout via View Binding and set as content view
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        // Set listeners using binding references
        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email & password required", Toast.LENGTH_SHORT).show()
            return
        }

        val loginRequest = LoginRequest(email, password)
        Log.d("LoginActivity", "Sending login request: $loginRequest")

        RetrofitClient.instance.loginUser(loginRequest)
            .enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    Log.d("LoginActivity", "Login response code=${response.code()}")
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("LoginActivity", "Login success body=$body")
                        if (body != null) {
                            tokenManager.saveTokens(body.tokens.accessToken, body.tokens.refreshToken)
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Empty response", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val err = response.errorBody()?.string()
                        Log.e("LoginActivity", "Login failed: $err")
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Log.e("LoginActivity", "Network error: ${t.message}", t)
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
