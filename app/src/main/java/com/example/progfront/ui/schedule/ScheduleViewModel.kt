package com.example.progfront.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.CustomScheduleRequest
import com.example.progfront.data.model.HabitCategoryResponse
import com.example.progfront.data.model.HabitRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.RecurringScheduleRequest
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.model.WeekdayScheduleRequest
import com.example.progfront.data.repository.HabitRepository
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.launch

class ScheduleViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository()
    private val habitRepository = HabitRepository()

    private val _scheduleDetail = MutableLiveData<Result<ScheduleResponse>>()
    val scheduleDetail: LiveData<Result<ScheduleResponse>> = _scheduleDetail

    private val _createResult = MutableLiveData<Result<*>>()
    val createResult: LiveData<Result<*>> = _createResult

    private val _updateResult = MutableLiveData<Result<ScheduleResponse>>()
    val updateResult: LiveData<Result<ScheduleResponse>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private val _habits = MutableLiveData<Result<List<HabitResponse>>>()
    val habits: LiveData<Result<List<HabitResponse>>> = _habits

    private val _habitCategories = MutableLiveData<Result<List<HabitCategoryResponse>>>()
    val habitCategories: LiveData<Result<List<HabitCategoryResponse>>> = _habitCategories

    private val _createHabitResult = MutableLiveData<Result<HabitResponse>>()
    val createHabitResult: LiveData<Result<HabitResponse>> = _createHabitResult

    fun loadScheduleById(id: Int) {
        viewModelScope.launch {
            _scheduleDetail.value = Result.Loading
            val result = scheduleRepository.getScheduleById(id)
            _scheduleDetail.value = result
        }
    }

    fun createCustomSchedule(scheduleData: CustomScheduleRequest) {
        viewModelScope.launch {
            _createResult.value = Result.Loading
            val result = scheduleRepository.createCustomSchedule(scheduleData)
            _createResult.value = result
        }
    }

    fun createRecurringSchedule(scheduleData: RecurringScheduleRequest) {
        viewModelScope.launch {
            _createResult.value = Result.Loading
            val result = scheduleRepository.createRecurringSchedule(scheduleData)
            _createResult.value = result
        }
    }

    fun createWeekdaySchedule(scheduleData: WeekdayScheduleRequest) {
        viewModelScope.launch {
            _createResult.value = Result.Loading
            val result = scheduleRepository.createWeekdaySchedule(scheduleData)
            _createResult.value = result
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

    fun loadHabits() {
        viewModelScope.launch {
            _habits.value = Result.Loading
            val result = habitRepository.getHabits()
            _habits.value = result
        }
    }

    fun loadHabitCategories() {
        viewModelScope.launch {
            _habitCategories.value = Result.Loading
            val result = habitRepository.getHabitCategories()
            _habitCategories.value = result
        }
    }

    fun createHabit(habitData: HabitRequest) {
        viewModelScope.launch {
            _createHabitResult.value = Result.Loading
            val result = habitRepository.createHabit(habitData)
            _createHabitResult.value = result
        }
    }
}



