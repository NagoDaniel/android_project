package com.example.progfront.data.repository

import com.example.progfront.data.Result
import com.example.progfront.data.model.HabitCategoryResponse
import com.example.progfront.data.model.HabitRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HabitRepository {

    suspend fun getHabits(): Result<List<HabitResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getHabits()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch habits: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun getHabitsByUser(userId: Int): Result<List<HabitResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getHabitsByUser(userId)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch user habits: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun createHabit(habitData: HabitRequest): Result<HabitResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createHabit(habitData)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to create habit: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun getHabitCategories(): Result<List<HabitCategoryResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getHabitCategories()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch categories: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}

