package com.example.progfront.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.progfront.data.Result
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.repository.ScheduleRepository

class DashboardViewModel : ViewModel() {

    private val scheduleRepository = ScheduleRepository()

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    suspend fun getSchedulesForDay(date: String): Result<List<ScheduleResponse>> {
        return scheduleRepository.getSchedulesForDay(date)
    }

    suspend fun deleteSchedule(id: Int): Result<Unit> {
        return scheduleRepository.deleteSchedule(id)
    }
}