package com.example.progfront.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<RegisterResponse>>()
    val loginResult: LiveData<Result<RegisterResponse>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            val result = authRepository.login(email, password)
            _loginResult.value = result
        }
    }
}

