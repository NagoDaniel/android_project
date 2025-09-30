package com.example.progfront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<EditText>(R.id.editTextUsername)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val passwordConfirmEditText = findViewById<EditText>(R.id.editTextPasswordConfirm)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            val confirmPassword = passwordConfirmEditText.text.toString()

            if (password == confirmPassword) {
                // Passwords match, proceed to home screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Passwords do not match, show error
                passwordEditText.error = "Passwords do not match"
                passwordConfirmEditText.error = "Passwords do not match"
            }
        }
    }
}

