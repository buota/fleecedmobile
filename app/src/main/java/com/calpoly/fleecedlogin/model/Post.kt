package com.calpoly.fleecedlogin.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userRanks: List<String> = emptyList(),
    val title: String = "",
    val content: String = "",
    val timestamp: String = "",
    val createdAt: Long = 0L,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val userVoteType: VoteType? = null,
    val commentCount: Int = 0,
    val postType: PostType = PostType.GENERAL,
    val pollData: PollData? = null,
    val weekNumber: Int? = null
) {
    val score: Int get() = upvotes - downvotes
}

enum class VoteType {
    UP, DOWN
}

enum class PostType {
    GENERAL,
    TRADE,
    START_SIT
}

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val parentCommentId: String? = null
)
