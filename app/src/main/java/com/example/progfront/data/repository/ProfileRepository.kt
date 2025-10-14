package com.example.progfront.data.repository

import com.example.progfront.data.Result
import com.example.progfront.data.model.ProfileResponse
import com.example.progfront.data.model.UpdateProfileRequest
import com.example.progfront.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

class ProfileRepository {

    suspend fun getMyProfile(): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getMyProfile()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to fetch profile: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun updateMyProfile(updateData: UpdateProfileRequest): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateMyProfile(updateData)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to update profile: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }

    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.uploadProfileImage(imagePart)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error("Failed to upload image: ${response.message()}")
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}", e)
            }
        }
    }
}

