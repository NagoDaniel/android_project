package com.example.progfront.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/auth/local/signup")
    fun registerUser(@Body userData: RegisterRequest): Call<RegisterResponse>

    @POST("/auth/local/signin")
    fun loginUser(@Body userData: LoginRequest): Call<RegisterResponse>

    // Habit endpoints
    @GET("/habit")
    fun getHabits(@Header("Authorization") token: String): Call<List<HabitResponse>>

    @POST("/habit")
    fun createHabit(@Header("Authorization") token: String, @Body habitData: HabitRequest): Call<HabitResponse>

    @GET("/habit/categories")
    fun getHabitCategories(@Header("Authorization") token: String): Call<List<HabitCategoryResponse>>

    // Schedule endpoints
    @POST("/schedule/custom")
    fun createCustomSchedule(@Header("Authorization") token: String, @Body scheduleData: CustomScheduleRequest): Call<ScheduleResponse>

    @POST("/schedule/recurring")
    fun createRecurringSchedule(@Header("Authorization") token: String, @Body scheduleData: RecurringScheduleRequest): Call<List<ScheduleResponse>>

    @POST("/schedule/recurring/weekdays")
    fun createWeekdaySchedule(@Header("Authorization") token: String, @Body scheduleData: WeekdayScheduleRequest): Call<List<ScheduleResponse>>
}

