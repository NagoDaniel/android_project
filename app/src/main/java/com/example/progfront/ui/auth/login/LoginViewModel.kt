package com.example.progfront.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.repository.AuthRepository
import com.example.progfront.utils.TokenManager
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class LoginViewModel(application: Application) : AndroidViewModel( application) {
    private val authRepository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<RegisterResponse>>()
    val loginResult: LiveData<Result<RegisterResponse>> = _loginResult
    private val tokenManager = TokenManager(application.applicationContext)

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            val result = authRepository.login(email, password)
            if (result is Result.Success) {
                try {
                    tokenManager.saveTokens(
                        result.data.tokens.accessToken,
                        result.data.tokens.refreshToken
                    )
                } catch (e: Exception) {
                    // Log or handle if token saving fails, but still propagate the result
                }
            }
            _loginResult.value = result
        }
    }
}

