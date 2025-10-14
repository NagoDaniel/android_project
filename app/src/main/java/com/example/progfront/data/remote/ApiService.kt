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
import com.example.progfront.data.model.ProgressResponse
import com.example.progfront.data.model.ProgressCreateRequest
import com.example.progfront.data.model.ProfileResponse
import com.example.progfront.data.model.UpdateProfileRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Auth
    @POST("/auth/local/signup")
    suspend fun registerUser(@Body userData: RegisterRequest): Response<RegisterResponse>

    @POST("/auth/local/signin")
    suspend fun loginUser(@Body userData: LoginRequest): Response<RegisterResponse>

    // Token refresh returns plain Tokens object per spec
    @POST("/auth/local/refresh")
    suspend fun refreshTokens(@Header("refreshtoken") refreshToken: String): Response<Tokens>

    // Habits
    @GET("/habit")
    suspend fun getHabits(): Response<List<HabitResponse>>

    @POST("/habit")
    suspend fun createHabit(@Body habitData: HabitRequest): Response<HabitResponse>

    @GET("/habit/categories")
    suspend fun getHabitCategories(): Response<List<HabitCategoryResponse>>

    // Schedules (creation)
    @POST("/schedule/custom")
    suspend fun createCustomSchedule(@Body scheduleData: CustomScheduleRequest): Response<ScheduleResponse>

    @POST("/schedule/recurring")
    suspend fun createRecurringSchedule(@Body scheduleData: RecurringScheduleRequest): Response<List<ScheduleResponse>>

    @POST("/schedule/recurring/weekdays")
    suspend fun createWeekdaySchedule(@Body scheduleData: WeekdayScheduleRequest): Response<List<ScheduleResponse>>

    // Schedules (query)
    @GET("/schedule")
    suspend fun getAllSchedules(): Response<List<ScheduleResponse>>

    // Schedules (query) – confirmed endpoint form /schedule?day=YYYY-MM-DD
    @GET("/schedule")
    suspend fun getSchedulesForDay(@Query("date") day: String): Response<List<ScheduleResponse>>

    // Schedules (update)
    @PATCH("/schedule/{id}")
    suspend fun updateScheduleStatus(
        @Path("id") id: Int,
        @Body statusBody: Map<String, String>
    ): Response<ScheduleResponse>

    // Schedules (delete)
    @DELETE("/schedule/{id}")
    suspend fun deleteSchedule(@Path("id") id: Int): Response<Void>

    // Schedule detail
    @GET("/schedule/{id}")
    suspend fun getScheduleById(@Path("id") id: Int): Response<ScheduleResponse>

    // Generic schedule update (notes, times, etc.)
    @PATCH("/schedule/{id}")
    suspend fun updateSchedule(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ScheduleResponse>

    // Progress
    @POST("/progress")
    suspend fun createProgress(@Body body: ProgressCreateRequest): Response<ProgressResponse>

    // Profile
    @GET("/profile")
    suspend fun getMyProfile(): Response<ProfileResponse>

    @GET("/habit/user/{userId}")
    suspend fun getHabitsByUser(@Path("userId") userId: Int): Response<List<HabitResponse>>

    @PATCH("/profile")
    suspend fun updateMyProfile(@Body body: UpdateProfileRequest): Response<ProfileResponse>

    @POST("/auth/local/logout")
    suspend fun logout(): Response<Void>

    @Multipart
    @POST("/profile/upload-profile-image")
    suspend fun uploadProfileImage(@Part profileImage: MultipartBody.Part): Response<ProfileResponse>
}
