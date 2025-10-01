package com.example.progfront.data.remote

import com.example.progfront.data.model.CustomScheduleRequest
import com.example.progfront.data.model.HabitCategoryResponse
import com.example.progfront.data.model.HabitRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.LoginRequest
import com.example.progfront.data.model.RecurringScheduleRequest
import com.example.progfront.data.model.RegisterRequest
import com.example.progfront.data.model.RegisterResponse
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.model.WeekdayScheduleRequest
import com.example.progfront.data.model.Tokens
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Auth
    @POST("/auth/local/signup")
    fun registerUser(@Body userData: RegisterRequest): Call<RegisterResponse>

    @POST("/auth/local/signin")
    fun loginUser(@Body userData: LoginRequest): Call<RegisterResponse>

    // Token refresh returns plain Tokens object per spec
    @POST("/auth/local/refresh")
    fun refreshTokens(@Header("refreshtoken") refreshToken: String): Call<Tokens>

    // Habits
    @GET("/habit")
    fun getHabits(@Header("Authorization") token: String): Call<List<HabitResponse>>

    @POST("/habit")
    fun createHabit(@Header("Authorization") token: String, @Body habitData: HabitRequest): Call<HabitResponse>

    @GET("/habit/categories")
    fun getHabitCategories(@Header("Authorization") token: String): Call<List<HabitCategoryResponse>>

    // Schedules (creation)
    @POST("/schedule/custom")
    fun createCustomSchedule(@Header("Authorization") token: String, @Body scheduleData: CustomScheduleRequest): Call<ScheduleResponse>

    @POST("/schedule/recurring")
    fun createRecurringSchedule(@Header("Authorization") token: String, @Body scheduleData: RecurringScheduleRequest): Call<List<ScheduleResponse>>

    @POST("/schedule/recurring/weekdays")
    fun createWeekdaySchedule(@Header("Authorization") token: String, @Body scheduleData: WeekdayScheduleRequest): Call<List<ScheduleResponse>>

    // Schedules (query) – confirmed endpoint form /schedule?day=YYYY-MM-DD
    @GET("/schedule")
    fun getSchedulesForDay(@Header("Authorization") token: String, @Query("day") day: String): Call<List<ScheduleResponse>>

    // Schedules (update)
    @PATCH("/schedule/{id}")
    fun updateScheduleStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body statusBody: Map<String, String>
    ): Call<ScheduleResponse>

    // Schedules (delete)
    @DELETE("/schedule/{id}")
    fun deleteSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<Void>
}
