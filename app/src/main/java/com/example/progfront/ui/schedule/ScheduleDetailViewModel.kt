package com.example.progfront.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.ProgressCreateRequest
import com.example.progfront.data.model.ProgressResponse
import com.example.progfront.data.model.ScheduleResponse
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

    // Keep existing API that accepts a ready-made ProgressCreateRequest
    fun createProgress(request: ProgressCreateRequest) {
        viewModelScope.launch {
            _progressResult.value = Result.Loading
            val result = scheduleRepository.createProgress(request)
            _progressResult.value = result
        }
    }

    // New helper: build the request from a ScheduleResponse and submit. Moves small business logic to ViewModel.
    fun addProgressForSchedule(schedule: ScheduleResponse, loggedTime: Int?, notes: String?, isCompleted: Boolean) {
        viewModelScope.launch {
            _progressResult.value = Result.Loading
            // brief delay to avoid rapid requests throttling the backend
            kotlinx.coroutines.delay(50)
            val dateStr = schedule.date.ifBlank { schedule.start_time.take(10) }
            val request = ProgressCreateRequest(
                scheduleId = schedule.id,
                date = dateStr,
                logged_time = loggedTime,
                notes = notes,
                is_completed = isCompleted
            )
            val result = scheduleRepository.createProgress(request)
            _progressResult.value = result
        }
    }
}
