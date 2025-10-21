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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // Expose per-habit percent completions for the UI to observe
    private val _habitPercents = MutableLiveData<Map<Int, Int>>()
    val habitPercents: LiveData<Map<Int, Int>> = _habitPercents

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

    // Compute per-habit percent completed based on schedules during the past 15 days (including today)

    fun computeProgress(habitsList: List<HabitResponse>, schedules: List<com.example.progfront.data.model.ScheduleResponse>) {
        viewModelScope.launch {
            val habitIds = habitsList.map { it.id }.toSet()
            val totals = mutableMapOf<Int, Int>()
            val completed = mutableMapOf<Int, Int>()

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val today = todayCal.time
            val cutoffCal = Calendar.getInstance().apply {
                time = today
                add(Calendar.DAY_OF_MONTH, -14)
            }
            val cutoff = cutoffCal.time

            for (sch in schedules) {
                val hid = sch?.habit?.id ?: sch.habitId ?: continue
                if (!habitIds.contains(hid)) continue
                val dateStr = sch.date
                val dateOk = try {
                    val d = sdf.parse(dateStr)
                    d != null && !d.after(today) && !d.before(cutoff)
                } catch (e: Exception) {
                    false
                }
                if (!dateOk) continue
                totals[hid] = (totals[hid] ?: 0) + 1
                if (sch.status.equals("Completed", ignoreCase = true)) {
                    completed[hid] = (completed[hid] ?: 0) + 1
                }
            }

            val percents = habitIds.associateWith { hid ->
                val t = totals[hid] ?: 0
                val c = completed[hid] ?: 0
                if (t > 0) (c * 100 / t) else 0
            }

            _habitPercents.postValue(percents)
        }
    }
}
