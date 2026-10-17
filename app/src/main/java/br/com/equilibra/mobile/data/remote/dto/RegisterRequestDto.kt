package br.com.equilibra.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)
