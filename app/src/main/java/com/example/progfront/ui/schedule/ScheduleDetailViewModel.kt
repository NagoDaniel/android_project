package com.example.progfront.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.ProgressCreateRequest
import com.example.progfront.data.model.ProgressResponse
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class ScheduleDetailViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository()

    private val _scheduleDetail = MutableLiveData<Result<ScheduleResponse>>()
    val scheduleDetail: LiveData<Result<ScheduleResponse>> = _scheduleDetail

    private val _updateResult = MutableLiveData<Result<ScheduleResponse>>()
    val updateResult: LiveData<Result<ScheduleResponse>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private val _progressResult = MutableLiveData<Result<ProgressResponse>>()
    val progressResult: LiveData<Result<ProgressResponse>> = _progressResult

    fun loadScheduleById(id: Int) {
        viewModelScope.launch {
            _scheduleDetail.value = Result.Loading
            val result = scheduleRepository.getScheduleById(id)
            _scheduleDetail.value = result
        }
    }

    fun updateSchedule(id: Int, body: Map<String, Any?>) {
        viewModelScope.launch {
            _updateResult.value = Result.Loading
            val result = scheduleRepository.updateSchedule(id, body)
            _updateResult.value = result
        }
    }

    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            _deleteResult.value = Result.Loading
            val result = scheduleRepository.deleteSchedule(id)
            _deleteResult.value = result
        }
    }

    fun createProgress(request: ProgressCreateRequest) {
        viewModelScope.launch {
            _progressResult.value = Result.Loading
            try {
                val response = RetrofitClient.instance.createProgress(request)
                if (response.isSuccessful && response.body() != null) {
                    _progressResult.value = Result.Success(response.body()!!)
                } else {
                    _progressResult.value = Result.Error("Failed to create progress: ${response.message()}")
                }
            } catch (e: Exception) {
                _progressResult.value = Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}

