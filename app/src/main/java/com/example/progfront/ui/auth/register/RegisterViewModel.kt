package com.example.progfront.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _registerResult = MutableLiveData<Result<RegisterResponse>>()
    val registerResult: LiveData<Result<RegisterResponse>> = _registerResult

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerResult.value = Result.Loading
            val result = authRepository.register(username, email, password)
            _registerResult.value = result
        }
    }
}

