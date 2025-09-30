package com.example.progfront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.network.LoginRequest
import com.example.progfront.network.RegisterResponse
import com.example.progfront.network.RetrofitClient
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(this)

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val loginRequest = LoginRequest(email, password)

            RetrofitClient.instance.loginUser(loginRequest)
                .enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.let { authResponse ->
                                // Save tokens
                                tokenManager.saveTokens(
                                    authResponse.tokens.accessToken,
                                    authResponse.tokens.refreshToken
                                )
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        val registerButton = findViewById<Button>(R.id.buttonGoToRegister)
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
