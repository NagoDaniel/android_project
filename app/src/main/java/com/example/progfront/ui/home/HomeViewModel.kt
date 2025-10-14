package com.example.progfront.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository()

    private val _schedules = MutableLiveData<Result<List<ScheduleResponse>>>()
    val schedules: LiveData<Result<List<ScheduleResponse>>> = _schedules

    private val _statusUpdateResult = MutableLiveData<Result<ScheduleResponse>>()
    val statusUpdateResult: LiveData<Result<ScheduleResponse>> = _statusUpdateResult

    fun loadSchedulesForDay(date: String) {
        viewModelScope.launch {
            _schedules.value = Result.Loading
            val result = scheduleRepository.getSchedulesForDay(date)
            _schedules.value = result
        }
    }

    fun toggleScheduleStatus(scheduleId: Int, newStatus: String) {
        viewModelScope.launch {
            val result = scheduleRepository.updateScheduleStatus(scheduleId, newStatus)
            _statusUpdateResult.value = result
        }
    }
}

