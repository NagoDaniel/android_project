package com.example.progfront.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.CustomScheduleRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.RecurringScheduleRequest
import com.example.progfront.data.model.WeekdayScheduleRequest
import com.example.progfront.data.repository.HabitRepository
import com.example.progfront.data.repository.ScheduleRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
class CreateScheduleViewModel : ViewModel() {
    private val scheduleRepository = ScheduleRepository()
    private val habitRepository = HabitRepository()

    data class CreateScheduleUiState(
        val habits: List<HabitResponse> = emptyList(),
        val selectedHabitPosition: Int = 0,
        val selectedDate: Calendar = Calendar.getInstance(),
        val formattedDate: String = "",
        val startHour: String = "09",
        val startMinute: String = "00",
        val endHour: String = "10",
        val endMinute: String = "00",
        val notes: String = "",
        val duration: String = "",
        val repeatPattern: RepeatPattern = RepeatPattern.NONE,
        val selectedDays: List<Int> = emptyList(),
        val numberOfWeeks: Int = 4,
        val showCustomDaysLayout: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val createSuccess: Boolean = false
    )

    enum class RepeatPattern {
        NONE, DAILY, WEEKDAYS, WEEKENDS, CUSTOM
    }

    private val _uiState = MutableLiveData(CreateScheduleUiState())
    val uiState: LiveData<CreateScheduleUiState> = _uiState

    init {
        formatAndUpdateDate(_uiState.value?.selectedDate ?: Calendar.getInstance())
    }

    private fun formatAndUpdateDate(calendar: Calendar) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _uiState.value = _uiState.value?.copy(
            selectedDate = calendar,
            formattedDate = sdf.format(calendar.time)
        )
    }

    fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, dayOfMonth)
        }
        formatAndUpdateDate(calendar)
    }

    fun loadHabits() {
        viewModelScope.launch {

            _uiState.value = _uiState.value?.copy(isLoading = true)
            when (val result = habitRepository.getHabits()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value?.copy(
                        habits = result.data,
                        isLoading = false
                    )
                    Log.d("CreateScheduleViewModel", "Loaded habits: ${result.data}")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value?.copy(
                        error = result.message,
                        isLoading = false
                    )
                    Log.d("CreateScheduleViewModel", "Error loading habits: ${result.message}")
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
                }
            }
        }
    }

    fun updateSelectedHabitPosition(position: Int) {
        _uiState.value = _uiState.value?.copy(selectedHabitPosition = position)
    }

    fun updateRepeatPattern(pattern: RepeatPattern) {
        _uiState.value = _uiState.value?.copy(
            repeatPattern = pattern,
            showCustomDaysLayout = pattern == RepeatPattern.CUSTOM
        )
    }

    fun updateSelectedDays(days: List<Int>) {
        _uiState.value = _uiState.value?.copy(selectedDays = days)
    }

    fun updateNumberOfWeeks(weeks: Int) {
        _uiState.value = _uiState.value?.copy(numberOfWeeks = weeks)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value?.copy(notes = notes)
    }

    fun updateDuration(duration: String) {
        _uiState.value = _uiState.value?.copy(duration = duration)
    }

    fun updateTimes(startHour: String, startMinute: String, endHour: String, endMinute: String) {
        _uiState.value = _uiState.value?.copy(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )
    }

    fun createSchedule() {
        val state = _uiState.value ?: return
        if (state.habits.isEmpty()) {
            _uiState.value = state.copy(error = "Please create a habit first")
            return
        }

        val selectedHabit = state.habits[state.selectedHabitPosition]
        val durationInt = state.duration.toIntOrNull()

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)

            val dateStr = state.formattedDate
            val startTime = "${dateStr}T${state.startHour}:${state.startMinute}:00"
            val endTime = "${dateStr}T${state.endHour}:${state.endMinute}:00"

            val result = when (state.repeatPattern) {
                RepeatPattern.NONE -> createCustomSchedule(
                    selectedHabit.id, dateStr, startTime, endTime, durationInt, state.notes
                )
                RepeatPattern.DAILY -> createRecurringSchedule(
                    selectedHabit.id, startTime, "daily", endTime, durationInt, state.notes
                )
                RepeatPattern.WEEKDAYS -> createRecurringSchedule(
                    selectedHabit.id, startTime, "weekdays", endTime, durationInt, state.notes
                )
                RepeatPattern.WEEKENDS -> createRecurringSchedule(
                    selectedHabit.id, startTime, "weekends", endTime, durationInt, state.notes
                )
                RepeatPattern.CUSTOM -> {
                    if (state.selectedDays.isEmpty()) {
                        _uiState.value = state.copy(
                            isLoading = false,
                            error = "Please select at least one day for custom schedule"
                        )
                        return@launch
                    }
                    createWeekdaySchedule(
                        selectedHabit.id,
                        startTime,
                        state.selectedDays,
                        state.numberOfWeeks,
                        endTime,
                        durationInt,
                        state.notes
                    )
                }
            }

            handleCreateResult(result)
        }
    }

    private fun handleCreateResult(result: Result<*>) {
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    error = null,
                    createSuccess = true
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    error = result.message,
                    createSuccess = false
                )
            }
            is Result.Loading -> {
                _uiState.value = _uiState.value?.copy(isLoading = true)
            }
        }
    }

    private suspend fun createCustomSchedule(
        habitId: Int,
        date: String,
        startTime: String,
        endTime: String,
        duration: Int?,
        notes: String
    ): Result<*> {
        return scheduleRepository.createCustomSchedule(
            CustomScheduleRequest(
                habitId = habitId,
                date = date,
                start_time = startTime,
                is_custom = true,
                end_time = if (endTime < startTime) null else endTime,
                duration_minutes = duration,
                participantIds = null,
                notes = notes.ifEmpty { null }
            )
        )
    }

    private suspend fun createRecurringSchedule(
        habitId: Int,
        startTime: String,
        pattern: String,
        endTime: String,
        duration: Int?,
        notes: String
    ): Result<*> {
        return scheduleRepository.createRecurringSchedule(
            RecurringScheduleRequest(
                habitId = habitId,
                start_time = startTime,
                repeatPattern = pattern,
                is_custom = true,
                end_time = endTime,
                duration_minutes = duration,
                repeatDays = 30,
                participantIds = null,
                notes = notes.ifEmpty { null }
            )
        )
    }

    private suspend fun createWeekdaySchedule(
        habitId: Int,
        startTime: String,
        selectedDays: List<Int>,
        numberOfWeeks: Int,
        endTime: String,
        duration: Int?,
        notes: String
    ): Result<*> {
        return scheduleRepository.createWeekdaySchedule(
            WeekdayScheduleRequest(
                habitId = habitId,
                start_time = startTime,
                daysOfWeek = selectedDays,
                numberOfWeeks = numberOfWeeks,
                duration_minutes = duration,
                end_time = endTime,
                participantIds = null,
                notes = notes.ifEmpty { null }
            )
        )
    }
}
