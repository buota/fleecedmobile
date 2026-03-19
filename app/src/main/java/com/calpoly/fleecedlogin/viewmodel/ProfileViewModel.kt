package com.calpoly.fleecedlogin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calpoly.fleecedlogin.data.SupabaseClient
import com.calpoly.fleecedlogin.model.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime

data class ProfileUiState(
    val user: User? = null,
    val recentPosts: List<Post> = emptyList(),
    val comments: Map<String, List<Comment>> = emptyMap(),
    val voteHistory: Map<String, List<VoteHistoryPoint>> = emptyMap(),
    val voteHistoryLoadingIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val usernameChangeError: String? = null,
    val usernameChangeSuccess: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _userPostsList = mutableListOf<Post>()

    private var homeViewModel: HomeViewModel? = null

    fun setHomeViewModel(viewModel: HomeViewModel) {
        homeViewModel = viewModel
    }

    fun setUser(user: User) {
        _uiState.value = _uiState.value.copy(user = user)
        loadPostsFromDb()
    }

    fun changeUsername(newUsername: String) {
        val user = _uiState.value.user ?: return

        if (newUsername.isBlank() || newUsername.length < 3) {
            _uiState.value = _uiState.value.copy(
                usernameChangeError = "Username must be at least 3 characters"
            )
            return
        }

        if (newUsername == user.username) {
            _uiState.value = _uiState.value.copy(
                usernameChangeError = "That's already your username"
            )
            return
        }

        viewModelScope.launch {
            try {
                val supabase = SupabaseClient.client

                val existing = supabase.postgrest
                    .from("users_profile")
                    .select {
                        filter { eq("username", newUsername) }
                    }
                    .decodeList<UserProfile>()

                if (existing.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        usernameChangeError = "Username is already taken"
                    )
                    return@launch
                }

                supabase.postgrest
                    .from("users_profile")
                    .update({ set("username", newUsername) }) {
                        filter { eq("id", user.id) }
                    }

                val updatedUser = user.copy(username = newUsername)
                _uiState.value = _uiState.value.copy(
                    user = updatedUser,
                    usernameChangeError = null,
                    usernameChangeSuccess = true
                )

                homeViewModel?.setUser(updatedUser)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    usernameChangeError = "Failed to change username. Please try again."
                )
            }
        }
    }

    fun clearUsernameChangeState() {
        _uiState.value = _uiState.value.copy(
            usernameChangeError = null,
            usernameChangeSuccess = false
        )
    }

    private fun loadPostsFromDb() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val supabase = SupabaseClient.client

                // Fetch only this users polls
                val polls = supabase.postgrest.from("polls")
                    .select {
                        filter { eq("creator_id", userId) }
                    }
                    .decodeList<DbPoll>()
                    .filter {
                        it.pollType == "trade" ||
                        it.pollType == "sit_start" ||
                        it.pollType == "start_sit"
                    }

                if (polls.isEmpty()) {
                    _userPostsList.clear()
                    _uiState.value = _uiState.value.copy(recentPosts = emptyList(), isLoading = false)
                    return@launch
                }

                val pollIds = polls.map { it.id }

                // Fetch options for these polls
                val allOptions = supabase.postgrest.from("poll_options")
                    .select {
                        filter { isIn("poll_id", pollIds) }
                    }
                    .decodeList<DbPollOption>()

                val optionIds = allOptions.map { it.id }

                // Fetch poll_option_players
                val allOptionPlayers = if (optionIds.isNotEmpty()) {
                    supabase.postgrest.from("poll_option_players")
                        .select {
                            filter { isIn("poll_option_id", optionIds) }
                        }
                        .decodeList<DbPollOptionPlayer>()
                } else emptyList()

                // Fetch only the players referenced by these posts
                val playerIds = allOptionPlayers.map { it.playerId }.distinct()
                val allPlayers = if (playerIds.isNotEmpty()) {
                    supabase.postgrest.from("players")
                        .select {
                            filter { isIn("id", playerIds) }
                        }
                        .decodeList<DbPlayer>()
                        .associateBy { it.id }
                } else emptyMap()

                // Fetch users votes
                val userVotes = supabase.postgrest.from("votes")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<DbVote>()
                    .associateBy { it.pollId }

                // Comments count
                val allComments = supabase.postgrest.from("comments")
                    .select()
                    .decodeList<DbComment>()
                    .filter { it.pollId in pollIds }
                val commentCountMap = allComments.groupBy { it.pollId }.mapValues { it.value.size }

                // Reactions
                val pollReactions = supabase.postgrest.from("reactions")
                    .select {
                        filter { eq("target_type", "poll") }
                    }
                    .decodeList<DbReaction>()
                    .filter { it.targetId in pollIds }

                val upvotesMap = mutableMapOf<String, Int>()
                val downvotesMap = mutableMapOf<String, Int>()
                val userReactionMap = mutableMapOf<String, String>()
                for (reaction in pollReactions) {
                    when (reaction.reactionType) {
                        "upvote" -> upvotesMap[reaction.targetId] = (upvotesMap[reaction.targetId] ?: 0) + 1
                        "downvote" -> downvotesMap[reaction.targetId] = (downvotesMap[reaction.targetId] ?: 0) + 1
                    }
                    if (reaction.userId == userId) {
                        userReactionMap[reaction.targetId] = reaction.reactionType
                    }
                }

                val user = _uiState.value.user!!
                val posts = polls.map { poll ->
                    val options = allOptions.filter { it.pollId == poll.id }.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                    val totalVotes = options.sumOf { it.voteCount }
                    val userVote = userVotes[poll.id]?.optionId

                    val postType = when (poll.pollType) {
                        "trade" -> PostType.TRADE
                        "sit_start", "start_sit" -> PostType.START_SIT
                        else -> PostType.GENERAL
                    }

                    val pollData = if (options.isNotEmpty()) {
                        val resolvedAtMillis = try {
                            poll.resolvedAt?.let { resolvedAt ->
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.parse(resolvedAt.substringBefore("+").substringBefore("."))?.time
                            }
                        } catch (e: Exception) { null }

                        PollData(
                            id = poll.id,
                            postId = poll.id,
                            options = options.map { opt ->
                                val optionPlayerLinks = allOptionPlayers.filter { it.pollOptionId == opt.id }.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                                val players = optionPlayerLinks.mapNotNull { link -> allPlayers[link.playerId]?.toPlayer() }
                                val side = when {
                                    postType == PostType.TRADE && opt.sortOrder == 0 -> TradeSide.GIVE
                                    postType == PostType.TRADE && opt.sortOrder == 1 -> TradeSide.RECEIVE
                                    else -> null
                                }
                                PollOption(
                                    id = opt.id, pollId = poll.id, players = players, side = side,
                                    voteCount = opt.voteCount,
                                    votePercentage = if (totalVotes > 0) (opt.voteCount.toFloat() / totalVotes) * 100 else 0f
                                )
                            },
                            totalVotes = totalVotes,
                            userVote = userVote,
                            correctOptionId = poll.winningOptionId,
                            status = poll.status,
                            resolvedAt = resolvedAtMillis,
                            player1Points = poll.resolutionMetadata?.player1Points,
                            player2Points = poll.resolutionMetadata?.player2Points
                        )
                    } else null

                    val createdAtMillis = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.parse(poll.createdAt.substringBefore("+").substringBefore("."))?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    val userVoteType = when (userReactionMap[poll.id]) {
                        "upvote" -> VoteType.UP
                        "downvote" -> VoteType.DOWN
                        else -> null
                    }

                    Post(
                        id = poll.id,
                        userId = poll.creatorId,
                        username = user.username,
                        userRanks = listOf(getRankForPoints(user.points).title),
                        title = "",
                        content = poll.description ?: "",
                        timestamp = createdAtMillis.toString(),
                        createdAt = createdAtMillis,
                        upvotes = upvotesMap[poll.id] ?: 0,
                        downvotes = downvotesMap[poll.id] ?: 0,
                        userVoteType = userVoteType,
                        commentCount = commentCountMap[poll.id] ?: 0,
                        postType = postType,
                        pollData = pollData,
                        weekNumber = poll.weekNumber
                    )
                }.sortedByDescending { it.createdAt }

                _userPostsList.clear()
                _userPostsList.addAll(posts)

                // Load comments
                val commentsMap = mutableMapOf<String, List<Comment>>()
                val userProfiles = supabase.postgrest.from("users_profile").select().decodeList<UserProfile>().associateBy { it.id }
                val commentsByPoll = allComments.groupBy { it.pollId }
                for ((pollId, dbComments) in commentsByPoll) {
                    commentsMap[pollId] = dbComments.map { dbc ->
                        val commenterProfile = userProfiles[dbc.userId]
                        val createdMillis = try {
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.parse(dbc.createdAt.substringBefore("+").substringBefore("."))?.time ?: 0L
                        } catch (e: Exception) { 0L }
                        Comment(
                            id = dbc.id, postId = dbc.pollId, userId = dbc.userId,
                            username = commenterProfile?.username ?: "Unknown",
                            content = dbc.content, createdAt = createdMillis,
                            likes = dbc.upvoteCount, isLiked = false,
                            parentCommentId = dbc.parentCommentId
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(
                    recentPosts = _userPostsList.toList(),
                    comments = commentsMap,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun loadRecentPosts() {
        _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
    }

    fun addPost(post: Post) {
        _userPostsList.add(0, post)
        _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
    }

    fun updatePost(updatedPost: Post) {
        val postIndex = _userPostsList.indexOfFirst { it.id == updatedPost.id }
        if (postIndex != -1) {
            _userPostsList[postIndex] = updatedPost
            _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
        }
    }

    fun syncComments(postId: String, comments: List<Comment>) {
        val updatedComments = _uiState.value.comments.toMutableMap()
        updatedComments[postId] = comments
        _uiState.value = _uiState.value.copy(comments = updatedComments)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (_: Exception) { }
        }
        _uiState.value = ProfileUiState()
        _userPostsList.clear()
    }

    fun refreshPosts() {
        loadPostsFromDb()
    }

    fun getCommentsForPost(postId: String): List<Comment> {
        val localComments = _uiState.value.comments[postId]
        return if (localComments != null && localComments.isNotEmpty()) {
            localComments
        } else {
            homeViewModel?.getCommentsForPost(postId) ?: emptyList()
        }
    }

    fun updateComments(postId: String, comments: List<Comment>) {
        val updatedComments = _uiState.value.comments.toMutableMap()
        updatedComments[postId] = comments
        _uiState.value = _uiState.value.copy(comments = updatedComments)
    }

    /** Fetches fresh comments for a specific post from the DB and updates state */
    private fun parseSupabaseTimestampMs(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(value.substringBefore("+").substringBefore(".").substringBefore("Z"))?.time
            }.getOrNull()
    }

    fun loadVoteHistory(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                voteHistoryLoadingIds = _uiState.value.voteHistoryLoadingIds + postId
            )
            try {
                val post = _userPostsList.firstOrNull { it.id == postId } ?: return@launch
                val pollData = post.pollData ?: return@launch
                if (pollData.options.size < 2) return@launch
                val option1Id = pollData.options[0].id

                val snapshots = SupabaseClient.client.postgrest
                    .from("poll_vote_snapshots")
                    .select { filter { eq("poll_id", postId) } }
                    .decodeList<DbVoteSnapshot>()
                    .filter { it.snapshotAt.isNotBlank() }
                    .sortedBy { it.snapshotAt }

                fun parseTs(str: String): Long = parseSupabaseTimestampMs(str) ?: 0L

                val startTs = post.createdAt.takeIf { it > 0L } ?: 0L
                val pollEndTs = pollData.resolvedAt?.takeIf { it > startTs } ?: System.currentTimeMillis()
                val currentTs = minOf(System.currentTimeMillis(), pollEndTs)
                val fallbackPct = pollData.options[0].votePercentage.coerceIn(0f, 100f)

                val pollOptionIds = pollData.options.map { it.id }.toSet()
                val snapshotsByTs = snapshots
                    .groupBy { parseTs(it.snapshotAt) }
                    .filterKeys { it > 0L }
                    .toSortedMap()

                val runningCounts = mutableMapOf<String, Int>()
                pollOptionIds.forEach { runningCounts[it] = 0 }

                val snapshotPoints = mutableListOf<VoteHistoryPoint>()
                snapshotsByTs.forEach { (ts, rowsAtTs) ->
                    rowsAtTs.forEach { row ->
                        if (row.optionId in pollOptionIds) {
                            runningCounts[row.optionId] = row.voteCount.coerceAtLeast(0)
                        }
                    }
                    val totalVotes = runningCounts.values.sum()
                    if (totalVotes > 0) {
                        val opt1Votes = runningCounts[option1Id] ?: 0
                        val pct = (opt1Votes.toFloat() / totalVotes) * 100f
                        snapshotPoints.add(VoteHistoryPoint(ts, pct.coerceIn(0f, 100f)))
                    }
                }

                val inRange = snapshotPoints.filter { it.timestampMs in startTs..currentTs }
                val startPct = inRange.firstOrNull()?.option1Pct
                    ?: snapshotPoints.firstOrNull { it.timestampMs >= startTs }?.option1Pct
                    ?: snapshotPoints.lastOrNull  { it.timestampMs <= startTs }?.option1Pct
                    ?: if (pollData.totalVotes == 0) 50f else fallbackPct

                val history = mutableListOf<VoteHistoryPoint>()
                if (startTs > 0L) history.add(VoteHistoryPoint(startTs, startPct))
                history.addAll(inRange.filter { it.timestampMs > startTs && it.timestampMs < currentTs })

                // Always end the chart at the actual current vote split
                val currentPct = fallbackPct

                if (currentTs > startTs) history.add(VoteHistoryPoint(currentTs, currentPct))

                val cleanedHistory = history
                    .sortedBy { it.timestampMs }
                    .groupBy { it.timestampMs }
                    .mapNotNull { (_, pts) -> pts.lastOrNull() }
                    .sortedBy { it.timestampMs }

                val updated = _uiState.value.voteHistory.toMutableMap()
                updated[postId] = cleanedHistory
                _uiState.value = _uiState.value.copy(voteHistory = updated)
            } catch (_: Exception) {
                val post = _userPostsList.firstOrNull { it.id == postId }
                val pollData = post?.pollData
                val updated = _uiState.value.voteHistory.toMutableMap()
                if (post != null && pollData != null && pollData.options.size >= 2 && post.createdAt > 0L) {
                    val fbPct = pollData.options[0].votePercentage.coerceIn(0f, 100f)
                    val sPct = if (pollData.totalVotes == 0) 50f else fbPct
                    val startTs = post.createdAt
                    val nowTs = System.currentTimeMillis()
                    updated[postId] = listOf(VoteHistoryPoint(startTs, sPct), VoteHistoryPoint(nowTs, fbPct))
                } else {
                    updated[postId] = emptyList()
                }
                _uiState.value = _uiState.value.copy(voteHistory = updated)
            } finally {
                _uiState.value = _uiState.value.copy(
                    voteHistoryLoadingIds = _uiState.value.voteHistoryLoadingIds - postId
                )
            }
        }
    }

    fun loadCommentsForPost(postId: String) {
        viewModelScope.launch {
            try {
                val supabase = SupabaseClient.client
                val dbComments = supabase.postgrest.from("comments")
                    .select { filter { eq("poll_id", postId) } }
                    .decodeList<DbComment>()

                val userProfiles = supabase.postgrest.from("users_profile")
                    .select()
                    .decodeList<UserProfile>()
                    .associateBy { it.id }

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }

                val comments = dbComments.map { dbc ->
                    val createdMillis = try {
                        sdf.parse(dbc.createdAt.substringBefore("+").substringBefore("."))?.time ?: 0L
                    } catch (_: Exception) { 0L }
                    Comment(
                        id = dbc.id,
                        postId = dbc.pollId,
                        userId = dbc.userId,
                        username = userProfiles[dbc.userId]?.username ?: "Unknown",
                        content = dbc.content,
                        createdAt = createdMillis,
                        likes = dbc.upvoteCount,
                        isLiked = false,
                        parentCommentId = dbc.parentCommentId
                    )
                }.sortedBy { it.createdAt }

                syncComments(postId, comments)
                homeViewModel?.updateComments(postId, comments)
            } catch (_: Exception) { }
        }
    }

    fun addCommentToPost(postId: String, commentText: String) {
        val currentUser = _uiState.value.user ?: return

        val commentId = java.util.UUID.randomUUID().toString()
        val newComment = Comment(
            id = commentId,
            postId = postId,
            userId = currentUser.id,
            username = currentUser.username,
            content = commentText,
            createdAt = System.currentTimeMillis(),
            likes = 0,
            isLiked = false,
            parentCommentId = null
        )

        val updatedCommentsList = getCommentsForPost(postId) + newComment
        val updatedComments = _uiState.value.comments.toMutableMap().also { it[postId] = updatedCommentsList }

        // Always update comments in state immediately
        _uiState.value = _uiState.value.copy(comments = updatedComments)

        val postIndex = _userPostsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val updatedPost = _userPostsList[postIndex].copy(commentCount = _userPostsList[postIndex].commentCount + 1)
            _userPostsList[postIndex] = updatedPost
            _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
            homeViewModel?.updateComments(postId, updatedCommentsList)
            homeViewModel?.updatePostInList(updatedPost)
        }

        // Persist to DB with same ID as optimistic comment
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest.from("comments").insert(
                    DbCommentInsert(
                        id = commentId,
                        pollId = postId,
                        userId = currentUser.id,
                        content = commentText
                    )
                )
            } catch (_: Exception) { }
        }
    }

    fun likeComment(postId: String, commentId: String) {
        val currentComments = getCommentsForPost(postId).toMutableList()
        val commentIndex = currentComments.indexOfFirst { it.id == commentId }

        if (commentIndex != -1) {
            val comment = currentComments[commentIndex]
            val updatedComment = if (comment.isLiked) {
                comment.copy(isLiked = false, likes = (comment.likes - 1).coerceAtLeast(0))
            } else {
                comment.copy(isLiked = true, likes = comment.likes + 1)
            }

            currentComments[commentIndex] = updatedComment

            val updatedComments = _uiState.value.comments.toMutableMap()
            updatedComments[postId] = currentComments

            _uiState.value = _uiState.value.copy(comments = updatedComments)
            homeViewModel?.updateComments(postId, currentComments)
        }
    }

    fun replyToComment(postId: String, parentCommentId: String, replyText: String) {
        val currentUser = _uiState.value.user ?: return

        val replyId = java.util.UUID.randomUUID().toString()
        val replyComment = Comment(
            id = replyId,
            postId = postId,
            userId = currentUser.id,
            username = currentUser.username,
            content = replyText,
            createdAt = System.currentTimeMillis(),
            likes = 0,
            isLiked = false,
            parentCommentId = parentCommentId
        )

        val updatedCommentsList = getCommentsForPost(postId) + replyComment
        val updatedComments = _uiState.value.comments.toMutableMap().also { it[postId] = updatedCommentsList }

        // Always update comments in state immediately
        _uiState.value = _uiState.value.copy(comments = updatedComments)

        val postIndex = _userPostsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val updatedPost = _userPostsList[postIndex].copy(commentCount = _userPostsList[postIndex].commentCount + 1)
            _userPostsList[postIndex] = updatedPost
            _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
            homeViewModel?.updateComments(postId, updatedCommentsList)
            homeViewModel?.updatePostInList(updatedPost)
        }

        // Persist to DB with same ID as optimistic reply
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest.from("comments").insert(
                    DbCommentInsert(
                        id = replyId,
                        pollId = postId,
                        userId = currentUser.id,
                        parentCommentId = parentCommentId,
                        content = replyText
                    )
                )
            } catch (_: Exception) { }
        }
    }

    fun votePost(postId: String, voteType: VoteType) {
        val userId = _uiState.value.user?.id ?: return
        val postIndex = _userPostsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val post = _userPostsList[postIndex]
            val previousVote = post.userVoteType

            val updatedPost = when {
                previousVote == voteType -> {
                    if (voteType == VoteType.UP) {
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0), userVoteType = null)
                    } else {
                        post.copy(downvotes = (post.downvotes - 1).coerceAtLeast(0), userVoteType = null)
                    }
                }
                previousVote != null -> {
                    if (voteType == VoteType.UP) {
                        post.copy(upvotes = post.upvotes + 1, downvotes = (post.downvotes - 1).coerceAtLeast(0), userVoteType = VoteType.UP)
                    } else {
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0), downvotes = post.downvotes + 1, userVoteType = VoteType.DOWN)
                    }
                }
                else -> {
                    if (voteType == VoteType.UP) {
                        post.copy(upvotes = post.upvotes + 1, userVoteType = VoteType.UP)
                    } else {
                        post.copy(downvotes = post.downvotes + 1, userVoteType = VoteType.DOWN)
                    }
                }
            }

            _userPostsList[postIndex] = updatedPost
            _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
            homeViewModel?.updatePostInList(updatedPost)

            // Persist reaction to DB
            viewModelScope.launch {
                try {
                    val supabase = SupabaseClient.client
                    val reactionType = when (voteType) {
                        VoteType.UP -> "upvote"
                        VoteType.DOWN -> "downvote"
                    }

                    if (previousVote == voteType) {
                        supabase.postgrest.from("reactions").delete {
                            filter {
                                eq("user_id", userId)
                                eq("target_type", "poll")
                                eq("target_id", postId)
                            }
                        }
                    } else if (previousVote != null) {
                        supabase.postgrest.from("reactions").update(
                            { set("reaction_type", reactionType) }
                        ) {
                            filter {
                                eq("user_id", userId)
                                eq("target_type", "poll")
                                eq("target_id", postId)
                            }
                        }
                    } else {
                        supabase.postgrest.from("reactions").insert(
                            DbReactionUpsert(
                                userId = userId,
                                targetType = "poll",
                                targetId = postId,
                                reactionType = reactionType
                            )
                        )
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun voteOnPoll(postId: String, optionId: String) {
        val userId = _uiState.value.user?.id ?: return
        val postIndex = _userPostsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val post = _userPostsList[postIndex]
            post.pollData?.let { pollData ->
                if (pollData.status == "closed") return
                val previousVote = pollData.userVote

                val updatedOptions = pollData.options.map { option ->
                    when {
                        option.id == previousVote -> option.copy(voteCount = (option.voteCount - 1).coerceAtLeast(0))
                        option.id == optionId -> option.copy(voteCount = option.voteCount + 1)
                        else -> option
                    }
                }

                val newTotalVotes = if (previousVote == null) pollData.totalVotes + 1 else pollData.totalVotes

                val optionsWithPercentages = updatedOptions.map { option ->
                    option.copy(
                        votePercentage = if (newTotalVotes > 0) {
                            (option.voteCount.toFloat() / newTotalVotes) * 100
                        } else 0f
                    )
                }

                val updatedPollData = pollData.copy(
                    options = optionsWithPercentages,
                    totalVotes = newTotalVotes,
                    userVote = optionId
                )

                val updatedPost = post.copy(pollData = updatedPollData)
                _userPostsList[postIndex] = updatedPost
                _uiState.value = _uiState.value.copy(recentPosts = _userPostsList.toList())
                homeViewModel?.updatePostInList(updatedPost)

                // Persist vote to DB
                viewModelScope.launch {
                    try {
                        val supabase = SupabaseClient.client
                        if (previousVote == null) {
                            supabase.postgrest.from("votes").insert(
                                DbVoteUpsert(pollId = postId, userId = userId, optionId = optionId)
                            )
                        } else {
                            supabase.postgrest.from("votes").update(
                                { set("option_id", optionId) }
                            ) {
                                filter {
                                    eq("poll_id", postId)
                                    eq("user_id", userId)
                                }
                            }
                        }
                        for (opt in optionsWithPercentages) {
                            supabase.postgrest.from("poll_options").update(
                                { set("vote_count", opt.voteCount) }
                            ) {
                                filter { eq("id", opt.id) }
                            }
                        }
                        supabase.postgrest.from("polls").update(
                            { set("vote_count", newTotalVotes) }
                        ) {
                            filter { eq("id", postId) }
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }
}
