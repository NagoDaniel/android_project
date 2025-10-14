package com.example.progfront.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.ProfileResponse
import com.example.progfront.data.model.UpdateProfileRequest
import com.example.progfront.data.repository.HabitRepository
import com.example.progfront.data.repository.ProfileRepository
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ProfileViewModel : ViewModel() {
    private val profileRepository = ProfileRepository()
    private val habitRepository = HabitRepository()
    private val scheduleRepository = ScheduleRepository()

    private val _profile = MutableLiveData<Result<ProfileResponse>>()
    val profile: LiveData<Result<ProfileResponse>> = _profile

    private val _habits = MutableLiveData<Result<List<HabitResponse>>>()
    val habits: LiveData<Result<List<HabitResponse>>> = _habits

    private val _allSchedules = MutableLiveData<Result<List<com.example.progfront.data.model.ScheduleResponse>>>()
    val allSchedules: LiveData<Result<List<com.example.progfront.data.model.ScheduleResponse>>> = _allSchedules

    private val _updateResult = MutableLiveData<Result<ProfileResponse>>()
    val updateResult: LiveData<Result<ProfileResponse>> = _updateResult

    fun loadProfile() {
        viewModelScope.launch {
            _profile.value = Result.Loading
            val result = profileRepository.getMyProfile()
            _profile.value = result
        }
    }

    fun loadHabitsByUser(userId: Int) {
        viewModelScope.launch {
            _habits.value = Result.Loading
            val result = habitRepository.getHabitsByUser(userId)
            _habits.value = result
        }
    }

    fun loadAllSchedules() {
        viewModelScope.launch {
            val result = scheduleRepository.getAllSchedules()
            _allSchedules.value = result
        }
    }

    fun updateProfile(updateData: UpdateProfileRequest) {
        viewModelScope.launch {
            _updateResult.value = Result.Loading
            val result = profileRepository.updateMyProfile(updateData)
            _updateResult.value = result
        }
    }

    fun uploadProfileImage(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            _updateResult.value = Result.Loading
            val result = profileRepository.uploadProfileImage(imagePart)
            _updateResult.value = result
        }
    }
}

