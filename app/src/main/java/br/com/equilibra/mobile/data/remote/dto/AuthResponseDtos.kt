package br.com.equilibra.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("tokenType") val tokenType: String = "Bearer",
    @SerialName("expiresIn") val expiresIn: Long
)

@Serializable
data class RegisteredUserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("createdAt") val createdAt: String
)

@Serializable
data class CurrentUserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("createdAt") val createdAt: String
)
