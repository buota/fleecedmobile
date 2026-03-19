package com.calpoly.fleecedlogin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val postCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val points: Int = 0
)

@Serializable
data class UserProfile(
    val id: String = "",
    val username: String = "",
    @SerialName("total_points")
    val totalPoints: Int = 0
)