package com.calpoly.fleecedlogin.model

data class PollData(
    val id: String = "",
    val postId: String = "",
    val options: List<PollOption> = emptyList(),
    val totalVotes: Int = 0,
    val userVote: String? = null,
    val correctOptionId: String? = null,
    val status: String = "active",
    val resolvedAt: Long? = null,
    val player1Points: Double? = null,
    val player2Points: Double? = null
)

data class PollOption(
    val id: String = "",
    val pollId: String = "",
    val players: List<Player> = emptyList(),
    val side: TradeSide? = null,
    val voteCount: Int = 0,
    val votePercentage: Float = 0f
)

data class Vote(
    val id: String = "",
    val userId: String = "",
    val pollId: String = "",
    val optionId: String = "",
    val createdAt: Long = 0L
)

enum class TradeSide {
    GIVE,
    RECEIVE
}
