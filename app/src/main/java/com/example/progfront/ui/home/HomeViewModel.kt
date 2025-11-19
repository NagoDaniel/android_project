package com.example.progfront.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.delay
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
            if (result is Result.Success) {
                val filtered = scheduleRepository.filterSchedulesByDate(result.data, date)
                _schedules.value = Result.Success(filtered)
            } else {
                _schedules.value = result
            }
        }
    }


    fun toggleScheduleStatus(scheduleId: Int, currentStatus: String) {
        viewModelScope.launch {
            _statusUpdateResult.value = Result.Loading
            // small delay to avoid rapid-fire calls that may hit backend throttle limits
            delay(50)
            val sequence = listOf("Planned", "Completed", "Skipped")
            val idx = sequence.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
                .let { if (it == -1) 0 else it }
            val next = sequence[(idx + 1) % sequence.size]
            val result = scheduleRepository.updateScheduleStatus(scheduleId, next)
            _statusUpdateResult.value = result
        }
    }
}
