package com.calpoly.fleecedlogin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// === DB row models for Supabase ===

@Serializable
data class DbPoll(
    val id: String = "",
    @SerialName("creator_id")
    val creatorId: String = "",
    @SerialName("poll_type")
    val pollType: String = "",
    val title: String = "",
    val description: String? = null,
    val status: String = "active",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("vote_count")
    val voteCount: Int = 0,
    @SerialName("week_number")
    val weekNumber: Int? = null,
    @SerialName("winning_option_id")
    val winningOptionId: String? = null,
    @SerialName("resolved_at")
    val resolvedAt: String? = null,
    @SerialName("resolution_metadata")
    val resolutionMetadata: DbResolutionMetadata? = null
)

@Serializable
data class DbResolutionMetadata(
    @SerialName("player1_points")
    val player1Points: Double? = null,
    @SerialName("player2_points")
    val player2Points: Double? = null
)

@Serializable
data class DbPollInsert(
    val id: String,
    @SerialName("creator_id")
    val creatorId: String,
    @SerialName("poll_type")
    val pollType: String,
    val title: String,
    val description: String? = null,
    @SerialName("resolved_at")
    val resolvedAt: String? = null,
    @SerialName("week_number")
    val weekNumber: Int? = null
)

@Serializable
data class DbPollOption(
    val id: String = "",
    @SerialName("poll_id")
    val pollId: String = "",
    @SerialName("option_text")
    val optionText: String = "",
    @SerialName("option_type")
    val optionType: String = "",
    @SerialName("sort_order")
    val sortOrder: Int? = null,
    @SerialName("vote_count")
    val voteCount: Int = 0
)

@Serializable
data class DbPollOptionInsert(
    val id: String,
    @SerialName("poll_id")
    val pollId: String,
    @SerialName("option_text")
    val optionText: String,
    @SerialName("option_type")
    val optionType: String,
    @SerialName("sort_order")
    val sortOrder: Int
)

@Serializable
data class DbPollOptionPlayer(
    val id: String = "",
    @SerialName("poll_option_id")
    val pollOptionId: String = "",
    @SerialName("player_id")
    val playerId: String = "",
    val role: String? = "option",
    @SerialName("sort_order")
    val sortOrder: Int? = null
)

@Serializable
data class DbPollOptionPlayerInsert(
    @SerialName("poll_option_id")
    val pollOptionId: String,
    @SerialName("player_id")
    val playerId: String,
    val role: String = "option",
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

@Serializable
data class DbVote(
    val id: String = "",
    @SerialName("poll_id")
    val pollId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("option_id")
    val optionId: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class DbVoteUpsert(
    @SerialName("poll_id")
    val pollId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("option_id")
    val optionId: String
)

@Serializable
data class DbComment(
    val id: String = "",
    @SerialName("poll_id")
    val pollId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val content: String = "",
    @SerialName("upvote_count")
    val upvoteCount: Int = 0,
    @SerialName("downvote_count")
    val downvoteCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class DbCommentInsert(
    val id: String,
    @SerialName("poll_id")
    val pollId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    val content: String
)

@Serializable
data class DbReaction(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("target_type")
    val targetType: String = "",
    @SerialName("target_id")
    val targetId: String = "",
    @SerialName("reaction_type")
    val reactionType: String = ""
)

@Serializable
data class DbReactionUpsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_type")
    val targetType: String,
    @SerialName("target_id")
    val targetId: String,
    @SerialName("reaction_type")
    val reactionType: String
)

@Serializable
data class DbVoteSnapshot(
    val id: String = "",
    @SerialName("poll_id")
    val pollId: String = "",
    @SerialName("option_id")
    val optionId: String = "",
    @SerialName("vote_count")
    val voteCount: Int = 0,
    @SerialName("snapshot_at")
    val snapshotAt: String = ""
)

@Serializable
data class DbVoteSnapshotInsert(
    val id: String,
    @SerialName("poll_id")
    val pollId: String,
    @SerialName("option_id")
    val optionId: String,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("snapshot_at")
    val snapshotAt: String
)
