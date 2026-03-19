package com.calpoly.fleecedlogin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calpoly.fleecedlogin.data.SupabaseClient
import com.calpoly.fleecedlogin.model.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

enum class FeedWeekFilter { CURRENT_WEEK, ALL_WEEKS }
enum class FeedSortOrder  { TOP_RATED, NEWEST_FIRST }

/** One data point for the Kalshi-style vote trend chart. */
data class VoteHistoryPoint(val timestampMs: Long, val option1Pct: Float)

data class HomeUiState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val comments: Map<String, List<Comment>> = emptyMap(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val weekFilter: FeedWeekFilter = FeedWeekFilter.ALL_WEEKS,
    val sortOrder: FeedSortOrder  = FeedSortOrder.TOP_RATED,
    val postTypeFilter: PostType  = PostType.START_SIT,
    // Posts matching the current type regardless of week
    // apart from "no posts of this type at all"
    val typePostsCount: Int = 0,
    // Total posts in _postsList across all types and weeks
    val totalPostsCount: Int = 0,
    // Vote trend history per poll id
    val voteHistory: Map<String, List<VoteHistoryPoint>> = emptyMap(),
    // Poll ids currently being fetched
    val voteHistoryLoadingIds: Set<String> = emptySet()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _postsList = mutableListOf<Post>()

    private var profileViewModel: ProfileViewModel? = null

    fun setProfileViewModel(viewModel: ProfileViewModel) {
        profileViewModel = viewModel
    }

    fun setUser(user: User) {
        _uiState.value = _uiState.value.copy(user = user)
        loadPostsFromDb()
    }

    // Filter / sort

    companion object {
        private const val TAG = "HomeViewModel"
        private const val BOOST_WEIGHT = 2.0
        private const val MILLIS_PER_HOUR = 3_600_000.0
    }

    private fun feedSortScore(post: Post): Double {
        val recencyScore = post.createdAt / MILLIS_PER_HOUR
        return post.score * BOOST_WEIGHT + recencyScore
    }

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

    /** Derives the display list from [_postsList] by applying type → sort */
    private fun getFilteredAndSortedPosts(
        postTypeFilter: PostType = _uiState.value.postTypeFilter,
        weekFilter: FeedWeekFilter = _uiState.value.weekFilter,
        sortOrder: FeedSortOrder  = _uiState.value.sortOrder
    ): List<Post> {
        val byType = _postsList.filter { it.postType == postTypeFilter }
        return when (sortOrder) {
            FeedSortOrder.TOP_RATED -> byType.sortedByDescending { feedSortScore(it) }
            FeedSortOrder.NEWEST_FIRST -> byType.sortedByDescending { it.createdAt }
        }
    }

    /** Posts matching the given type */
    private fun countByType(type: PostType = _uiState.value.postTypeFilter): Int =
        _postsList.count { it.postType == type }

    /**
     * Re-applies the current filters atomically and updates [uiState]
     * Called after every DB load or in-place mutation
     */
    fun loadPosts() {
        _uiState.value = _uiState.value.copy(
            posts = getFilteredAndSortedPosts(),
            typePostsCount = countByType(),
            totalPostsCount = _postsList.size
        )
    }

    private fun applyFilters(
        postTypeFilter: PostType = _uiState.value.postTypeFilter,
        weekFilter: FeedWeekFilter = _uiState.value.weekFilter,
        sortOrder: FeedSortOrder = _uiState.value.sortOrder
    ) {
        _uiState.value = _uiState.value.copy(
            postTypeFilter = postTypeFilter,
            weekFilter = weekFilter,
            sortOrder = sortOrder,
            posts = getFilteredAndSortedPosts(
                postTypeFilter = postTypeFilter,
                weekFilter = weekFilter,
                sortOrder = sortOrder
            ),
            typePostsCount = countByType(postTypeFilter),
            totalPostsCount = _postsList.size
        )
    }

    fun setPostTypeFilter(type: PostType) {
        if (_uiState.value.postTypeFilter == type) return
        applyFilters(postTypeFilter = type)
    }

    fun setSortOrder(order: FeedSortOrder) {
        if (_uiState.value.sortOrder == order) return
        applyFilters(sortOrder = order)
    }

    // Data access
    fun getPostById(postId: String): Post? = _postsList.firstOrNull { it.id == postId }

    // DB load
    private fun loadPostsFromDb() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val userId = _uiState.value.user?.id
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                val supabase = SupabaseClient.client

                // Fetch all polls
                val polls = supabase.postgrest.from("polls")
                    .select()
                    .decodeList<DbPoll>()
                    .filter {
                        it.pollType == "trade" ||
                        it.pollType == "sit_start" ||
                        it.pollType == "start_sit"
                    }

                // Fetch all poll options for loaded polls
                val pollIds = polls.map { it.id }
                val allOptions = if (pollIds.isNotEmpty()) {
                    supabase.postgrest.from("poll_options")
                        .select {
                            filter { isIn("poll_id", pollIds) }
                        }
                        .decodeList<DbPollOption>()
                } else {
                    emptyList()
                }

                // Fetch all poll_option_players for loaded options
                val optionIds = allOptions.map { it.id }
                val allOptionPlayers = if (optionIds.isNotEmpty()) {
                    supabase.postgrest.from("poll_option_players")
                        .select {
                            filter { isIn("poll_option_id", optionIds) }
                        }
                        .decodeList<DbPollOptionPlayer>()
                } else emptyList()

                // Fetch only the players referenced by this feed
                val playerIds = allOptionPlayers.map { it.playerId }.distinct()
                val allPlayers = if (playerIds.isNotEmpty()) {
                    supabase.postgrest.from("players")
                        .select {
                            filter { isIn("id", playerIds) }
                        }
                        .decodeList<DbPlayer>()
                        .associateBy { it.id }
                } else emptyMap()

                // Fetch user's votes (optional; feed still renders without this)
                val userVotes = runCatching {
                    supabase.postgrest.from("votes")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<DbVote>()
                        .associateBy { it.pollId }
                }.getOrElse { error ->
                    Log.w(TAG, "Failed to load votes for feed", error)
                    emptyMap()
                }

                // Fetch creator usernames (optional fallback to "Unknown")
                val userProfiles = runCatching {
                    supabase.postgrest.from("users_profile")
                        .select()
                        .decodeList<UserProfile>()
                        .associateBy { it.id }
                }.getOrElse { error ->
                    Log.w(TAG, "Failed to load user profiles for feed", error)
                    emptyMap()
                }

                // Fetch comment counts per poll (optional fallback to 0)
                val allComments = runCatching {
                    supabase.postgrest.from("comments")
                        .select()
                        .decodeList<DbComment>()
                }.getOrElse { error ->
                    Log.w(TAG, "Failed to load comments for feed", error)
                    emptyList()
                }
                val commentCountMap = allComments.groupBy { it.pollId }.mapValues { it.value.size }

                // Fetch reactions (optional fallback to 0)
                val pollReactions = runCatching {
                    supabase.postgrest.from("reactions")
                        .select {
                            filter { eq("target_type", "poll") }
                        }
                        .decodeList<DbReaction>()
                }.getOrElse { error ->
                    Log.w(TAG, "Failed to load poll reactions for feed", error)
                    emptyList()
                }

                val upvotesMap = mutableMapOf<String, Int>()
                val downvotesMap = mutableMapOf<String, Int>()
                val userReactionMap = mutableMapOf<String, String>() // pollId -> reaction_type
                for (reaction in pollReactions) {
                    when (reaction.reactionType) {
                        "upvote" -> upvotesMap[reaction.targetId] = (upvotesMap[reaction.targetId]   ?: 0) + 1
                        "downvote" -> downvotesMap[reaction.targetId] = (downvotesMap[reaction.targetId] ?: 0) + 1
                    }
                    if (reaction.userId == userId) {
                        userReactionMap[reaction.targetId] = reaction.reactionType
                    }
                }

                // Build Post objects
                val posts = polls.map { poll ->
                    val options = allOptions.filter { it.pollId == poll.id }.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                    val totalVotes = options.sumOf { it.voteCount }
                    val userVote = userVotes[poll.id]?.optionId
                    val creatorProfile = userProfiles[poll.creatorId]
                    val creatorUsername = creatorProfile?.username ?: "Unknown"
                    val creatorPoints = creatorProfile?.totalPoints ?: 0

                    val postType = when (poll.pollType) {
                        "trade" -> PostType.TRADE
                        "sit_start", "start_sit" -> PostType.START_SIT
                        else -> PostType.GENERAL
                    }

                    val pollData = if (options.isNotEmpty()) {
                        val resolvedAtMillis = parseSupabaseTimestampMs(poll.resolvedAt)

                        PollData(
                            id = poll.id,
                            postId = poll.id,
                            options = options.map { opt ->
                                val optionPlayerLinks = allOptionPlayers
                                    .filter { it.pollOptionId == opt.id }
                                    .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                                val players = optionPlayerLinks.mapNotNull { link ->
                                    allPlayers[link.playerId]?.toPlayer()
                                }
                                val side = when {
                                    postType == PostType.TRADE && opt.sortOrder == 0 -> TradeSide.GIVE
                                    postType == PostType.TRADE && opt.sortOrder == 1 -> TradeSide.RECEIVE
                                    else -> null
                                }
                                PollOption(
                                    id = opt.id,
                                    pollId = poll.id,
                                    players = players,
                                    side = side,
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

                    // Parse createdAt timestamp
                    val createdAtMillis = parseSupabaseTimestampMs(poll.createdAt) ?: 0L

                    val userVoteType = when (userReactionMap[poll.id]) {
                        "upvote" -> VoteType.UP
                        "downvote" -> VoteType.DOWN
                        else -> null
                    }

                    Post(
                        id = poll.id,
                        userId = poll.creatorId,
                        username = creatorUsername,
                        userRanks = listOf(getRankForPoints(creatorPoints).title),
                        title = "",
                        content = poll.description ?: "",
                        timestamp = createdAtMillis.toString(),
                        createdAt = createdAtMillis,
                        upvotes = upvotesMap[poll.id]   ?: 0,
                        downvotes = downvotesMap[poll.id] ?: 0,
                        userVoteType = userVoteType,
                        commentCount = commentCountMap[poll.id] ?: 0,
                        postType = postType,
                        pollData = pollData,
                        weekNumber = poll.weekNumber
                    )
                }

                Log.d(
                    TAG,
                    "Feed query results: polls=${polls.size}, options=${allOptions.size}, optionPlayers=${allOptionPlayers.size}, postsBuilt=${posts.size}"
                )

                _postsList.clear()
                _postsList.addAll(posts)

                // Snapshot current vote counts in the background
                viewModelScope.launch { writeVoteSnapshots(posts) }

                // Build comments map
                val commentsMap   = mutableMapOf<String, List<Comment>>()
                val commentsByPoll = allComments.groupBy { it.pollId }
                for ((pollId, dbComments) in commentsByPoll) {
                    val comments = dbComments.map { dbc ->
                        val commenterProfile = userProfiles[dbc.userId]
                        val createdMillis = try {
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.parse(dbc.createdAt.substringBefore("+").substringBefore("."))?.time ?: 0L
                        } catch (e: Exception) { 0L }
                        Comment(
                            id = dbc.id,
                            postId = dbc.pollId,
                            userId = dbc.userId,
                            username = commenterProfile?.username ?: "Unknown",
                            content = dbc.content,
                            createdAt = createdMillis,
                            likes = dbc.upvoteCount,
                            isLiked = false,
                            parentCommentId = dbc.parentCommentId
                        )
                    }
                    commentsMap[pollId] = comments
                }

                loadPosts()
                _uiState.value = _uiState.value.copy(
                    comments = commentsMap,
                    isLoading = false,
                    loadError = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load posts from Supabase", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = e.message ?: "Unknown feed load error"
                )
            }
        }
    }

    // Mutations

    fun addPost(post: Post) {
        _postsList.add(0, post)
        loadPosts()
    }

    fun updatePostInList(updatedPost: Post) {
        val idx = _postsList.indexOfFirst { it.id == updatedPost.id }
        if (idx != -1) {
            _postsList[idx] = updatedPost
            loadPosts()
        }
    }

    fun voteOnPoll(postId: String, optionId: String) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            val postIndex = _postsList.indexOfFirst { it.id == postId }
            if (postIndex != -1) {
                val post = _postsList[postIndex]
                post.pollData?.let { pollData ->
                    if (pollData.status == "closed") return@launch
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
                    _postsList[postIndex] = updatedPost
                    loadPosts()
                    profileViewModel?.updatePost(updatedPost)

                    // Persist vote to DB
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

    fun resolvePoll(postId: String, correctOptionId: String) {
        val postIndex = _postsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val post = _postsList[postIndex]
            post.pollData?.let { pollData ->
                val updatedPollData = pollData.copy(correctOptionId = correctOptionId)
                val updatedPost     = post.copy(pollData = updatedPollData)
                _postsList[postIndex] = updatedPost
                loadPosts()
                profileViewModel?.updatePost(updatedPost)
            }
        }
    }

    fun awardPointsForPoll(postId: String): Boolean {
        val user     = _uiState.value.user ?: return false
        val post     = _postsList.firstOrNull { it.id == postId } ?: return false
        val pollData = post.pollData ?: return false
        val correctId = pollData.correctOptionId ?: return false

        if (pollData.userVote == correctId) {
            val updatedUser = user.copy(points = user.points + 1)
            _uiState.value = _uiState.value.copy(user = updatedUser)
            return true
        }
        return false
    }

    fun logout() {
        _uiState.value = HomeUiState()
        _postsList.clear()
    }

    fun refreshPosts() {
        loadPostsFromDb()
    }

    private suspend fun writeVoteSnapshots(posts: List<Post>) {
        val snapshotAt = Instant.ofEpochMilli(System.currentTimeMillis()).toString()

        val rows = posts.flatMap { post ->
            val pollData = post.pollData ?: return@flatMap emptyList()
            // Skip polls with no votes — a zero-count row adds no useful chart information
            if (pollData.totalVotes <= 0) return@flatMap emptyList()
            if (pollData.options.size < 2) return@flatMap emptyList()

            pollData.options.map { option ->
                DbVoteSnapshotInsert(
                    id = UUID.randomUUID().toString(),
                    pollId = post.id,
                    optionId = option.id,
                    voteCount = option.voteCount,
                    snapshotAt = snapshotAt
                )
            }
        }

        if (rows.isEmpty()) return

        runCatching {
            SupabaseClient.client.postgrest
                .from("poll_vote_snapshots")
                .insert(rows)
        }.onFailure { e ->
            Log.w(TAG, "writeVoteSnapshots: insert failed", e)
        }
    }

    fun loadVoteHistory(postId: String) {
        viewModelScope.launch {
            // Signal loading started so the chart shows a skeleton immediately
            _uiState.value = _uiState.value.copy(
                voteHistoryLoadingIds = _uiState.value.voteHistoryLoadingIds + postId
            )
            try {
                val post = _postsList.firstOrNull { it.id == postId } ?: return@launch
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

                // Group snapshot rows by timestamp and carry counts forward so every point
                // reflects cumulative totals up to that moment
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

                val inRange  = snapshotPoints.filter { it.timestampMs in startTs..currentTs }
                val startPct = inRange.firstOrNull()?.option1Pct
                    ?: snapshotPoints.firstOrNull { it.timestampMs >= startTs }?.option1Pct
                    ?: snapshotPoints.lastOrNull  { it.timestampMs <= startTs }?.option1Pct
                    ?: if (pollData.totalVotes == 0) 50f else fallbackPct

                val history = mutableListOf<VoteHistoryPoint>()
                if (startTs > 0L) history.add(VoteHistoryPoint(startTs, startPct))
                history.addAll(inRange.filter { it.timestampMs > startTs && it.timestampMs < currentTs })

                // Always end the chart at the actual current vote split so the
                // displayed percentage matches what the user sees on the post
                val currentPct = fallbackPct

                if (currentTs > startTs) history.add(VoteHistoryPoint(currentTs, currentPct))

                val cleanedHistory = history
                    .sortedBy { it.timestampMs }
                    .groupBy { it.timestampMs }
                    .mapNotNull { (_, pts) -> pts.lastOrNull() }
                    .sortedBy { it.timestampMs }

                val updated = _uiState.value.voteHistory.toMutableMap()
                updated[postId] = cleanedHistory   // empty list = loaded with no usable data
                _uiState.value = _uiState.value.copy(voteHistory = updated)
            } catch (_: Exception) {
                // Build 2-point history from existing poll data so the
                // chart still renders even when poll_votes_snapshots has no rows
                val post = _postsList.firstOrNull { it.id == postId }
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

    fun votePost(postId: String, voteType: VoteType) {
        val userId = _uiState.value.user?.id ?: return
        val postIndex = _postsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val post = _postsList[postIndex]
            val previousVote = post.userVoteType

            val updatedPost = when {
                previousVote == voteType -> {
                    if (voteType == VoteType.UP)
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0), userVoteType = null)
                    else
                        post.copy(downvotes = (post.downvotes - 1).coerceAtLeast(0), userVoteType = null)
                }
                previousVote != null -> {
                    if (voteType == VoteType.UP)
                        post.copy(upvotes = post.upvotes + 1, downvotes = (post.downvotes - 1).coerceAtLeast(0), userVoteType = VoteType.UP)
                    else
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0), downvotes = post.downvotes + 1, userVoteType = VoteType.DOWN)
                }
                else -> {
                    if (voteType == VoteType.UP)
                        post.copy(upvotes = post.upvotes + 1, userVoteType = VoteType.UP)
                    else
                        post.copy(downvotes = post.downvotes + 1, userVoteType = VoteType.DOWN)
                }
            }

            _postsList[postIndex] = updatedPost
            loadPosts()
            profileViewModel?.updatePost(updatedPost)

            viewModelScope.launch {
                try {
                    val supabase = SupabaseClient.client
                    val reactionType = when (voteType) {
                        VoteType.UP   -> "upvote"
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

    // Comments
    fun getCommentsForPost(postId: String): List<Comment> =
        _uiState.value.comments[postId] ?: emptyList()

    fun updateComments(postId: String, comments: List<Comment>) {
        val updatedComments = _uiState.value.comments.toMutableMap()
        updatedComments[postId] = comments
        _uiState.value = _uiState.value.copy(comments = updatedComments)
    }

    /** Fetches fresh comments for a specific post from the DB and updates state */
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

                val updatedComments = _uiState.value.comments.toMutableMap()
                updatedComments[postId] = comments
                _uiState.value = _uiState.value.copy(comments = updatedComments)
                profileViewModel?.updateComments(postId, comments)
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

        val updatedCommentsList = (_uiState.value.comments[postId] ?: emptyList()) + newComment
        val updatedComments = _uiState.value.comments.toMutableMap().also { it[postId] = updatedCommentsList }

        _uiState.value = _uiState.value.copy(comments = updatedComments)

        val postIndex = _postsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val updatedPost = _postsList[postIndex].copy(commentCount = _postsList[postIndex].commentCount + 1)
            _postsList[postIndex] = updatedPost
            loadPosts()
            profileViewModel?.updatePost(updatedPost)
            profileViewModel?.updateComments(postId, updatedCommentsList)
        }

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
        val currentComments = _uiState.value.comments[postId] ?: return
        val commentIndex = currentComments.indexOfFirst { it.id == commentId }

        if (commentIndex != -1) {
            val comment = currentComments[commentIndex]
            val updatedComment = if (comment.isLiked)
                comment.copy(isLiked = false, likes = (comment.likes - 1).coerceAtLeast(0))
            else
                comment.copy(isLiked = true, likes = comment.likes + 1)

            val updatedCommentsList = currentComments.toMutableList()
            updatedCommentsList[commentIndex] = updatedComment

            val updatedComments = _uiState.value.comments.toMutableMap()
            updatedComments[postId] = updatedCommentsList

            _uiState.value = _uiState.value.copy(comments = updatedComments)
            profileViewModel?.updateComments(postId, updatedCommentsList)
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

        val updatedCommentsList = (_uiState.value.comments[postId] ?: emptyList()) + replyComment
        val updatedComments = _uiState.value.comments.toMutableMap().also { it[postId] = updatedCommentsList }

        _uiState.value = _uiState.value.copy(comments = updatedComments)

        val postIndex = _postsList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val updatedPost = _postsList[postIndex].copy(commentCount = _postsList[postIndex].commentCount + 1)
            _postsList[postIndex] = updatedPost
            loadPosts()
            profileViewModel?.updatePost(updatedPost)
            profileViewModel?.updateComments(postId, updatedCommentsList)
        }

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
}
