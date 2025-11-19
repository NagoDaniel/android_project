package com.example.progfront.data.repository

import com.example.progfront.data.Result
import com.example.progfront.data.model.CustomScheduleRequest
import com.example.progfront.data.model.ProgressCreateRequest
import com.example.progfront.data.model.RecurringScheduleRequest
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.model.WeekdayScheduleRequest
import com.example.progfront.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRepository {

    suspend fun getAllSchedules(): Result<List<ScheduleResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getAllSchedules()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch schedules: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun getSchedulesForDay(date: String): Result<List<ScheduleResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getSchedulesForDay(date)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch schedules for day: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun getScheduleById(id: Int): Result<ScheduleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getScheduleById(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun createCustomSchedule(scheduleData: CustomScheduleRequest): Result<ScheduleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createCustomSchedule(scheduleData)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to create schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun createRecurringSchedule(scheduleData: RecurringScheduleRequest): Result<List<ScheduleResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createRecurringSchedule(scheduleData)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to create recurring schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun createWeekdaySchedule(scheduleData: WeekdayScheduleRequest): Result<List<ScheduleResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createWeekdaySchedule(scheduleData)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to create weekday schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun updateScheduleStatus(id: Int, status: String): Result<ScheduleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateScheduleStatus(id, mapOf("status" to status))
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to update status: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun updateSchedule(id: Int, body: Map<String, Any?>): Result<ScheduleResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateSchedule(id, body)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to update schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun deleteSchedule(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.deleteSchedule(id)
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    Result.Error("Failed to delete schedule: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun createProgress(request: ProgressCreateRequest): Result<com.example.progfront.data.model.ProgressResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createProgress(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to create progress: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    fun filterSchedulesByDate(list: List<ScheduleResponse>, targetDay: String): List<ScheduleResponse> {
        if (list.isEmpty()) return list
        return list.filter { schedule ->
            val dateCandidates = listOfNotNull(schedule.start_time, schedule.date)
            dateCandidates.any { candidate ->
                val dayPart = candidate.take(10)
                dayPart == targetDay
            }
        }
    }
}
