package com.example.progfront.data.model

import com.google.gson.annotations.SerializedName

// Extended with alternate fields and an explicit base64 field so we can reliably pick whichever backend provides.
data class ProfileResponse(
    val id: Int,
    val email: String,
    val username: String?,
    val description: String?,
    @SerializedName(value = "profileImageUrl", alternate = ["profile_image_url", "profileImageURL", "imageUrl", "avatarUrl", "avatar"])
    val profileImageUrl: String?,
    @SerializedName(value = "profileImageBase64", alternate = ["profileImage", "profile_image", "profileImageData", "profile_image_base64", "imageBase64", "image_base64"])
    val profileImageBase64: String? = null,
    val createdAt: String?,
    val updatedAt: String?
)
