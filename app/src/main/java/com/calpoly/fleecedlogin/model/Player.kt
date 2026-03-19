package com.calpoly.fleecedlogin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Player (
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val position: String = "",
    val team: Team,
    val imageUrl: String? = null,
)

@Serializable
data class DbPlayer(
    val id: String = "",
    val name: String = "",
    val position: String = "",
    val team: String = "",
    @SerialName("is_active")
    val isActive: Boolean = true
) {
    fun toPlayer(): Player {
        val parts = name.split(" ", limit = 2)
        return Player(
            id = id,
            firstName = parts.getOrElse(0) { "" },
            lastName = parts.getOrElse(1) { "" },
            position = position,
            team = Team(
                teamName = team,
                teamColor = NflTeamColors.primary(team)
            ),
            imageUrl = "file:///android_asset/jerseys/$id.png"
        )
    }
}
