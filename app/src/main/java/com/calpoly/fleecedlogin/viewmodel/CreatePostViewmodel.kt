package com.calpoly.fleecedlogin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calpoly.fleecedlogin.data.SupabaseClient
import com.calpoly.fleecedlogin.model.*
import com.calpoly.fleecedlogin.util.isNflInSeason
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class PostViewModel : ViewModel() {

    private val _playerSearchResults = MutableStateFlow<List<Player>>(emptyList())
    val playerSearchResults: StateFlow<List<Player>> = _playerSearchResults.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    // Current user info
    private var currentUserId: String = ""
    private var currentUsername: String = ""
    private var currentUserPoints: Int = 0
    private var currentUserDisplayRanks: List<String> = emptyList()
    private val allowedFantasyPositions = setOf("QB", "RB", "WR", "TE")

    private fun isAllowedPosition(position: String): Boolean {
        val tokens = position.uppercase()
            .split(Regex("[^A-Z]+"))
            .filter { it.isNotBlank() }
        return tokens.any { it in allowedFantasyPositions }
    }

    fun setUser(user: User) {
        currentUserId = user.id
        currentUsername = user.username
        currentUserPoints = user.points
    }

    fun setUserDisplayRanks(ranks: List<String>) {
        currentUserDisplayRanks = ranks
    }
    private fun getPostDisplayRanks(): List<String> {
        val special = currentUserDisplayRanks.filter { it == "GOAT" || it == "MVP" }
        return if (special.isNotEmpty()) special
        else listOf(getRankForPoints(currentUserPoints).title)
    }

    fun searchPlayers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _playerSearchResults.value = emptyList()
                return@launch
            }

            try {
                val results = SupabaseClient.client.postgrest
                    .from("players")
                    .select {
                        filter {
                            or {
                                ilike("name", "%$query%")
                                ilike("team", "%$query%")
                            }
                        }
                    }
                    .decodeList<DbPlayer>()
                    .filter { isAllowedPosition(it.position) }
                    .map { it.toPlayer() }

                _playerSearchResults.value = results
            } catch (e: Exception) {
                _playerSearchResults.value = emptyList()
            }
        }
    }

    fun clearSearch() {
        _playerSearchResults.value = emptyList()
    }

    suspend fun createTradePost(
        givingPlayers: List<Player>,
        receivingPlayers: List<Player>
    ): Post? {
        val giveOptionId = UUID.randomUUID().toString()
        val receiveOptionId = UUID.randomUUID().toString()

        val options = listOf(
            OptionToInsert(
                id = giveOptionId,
                optionText = givingPlayers.joinToString(", ") { "${it.firstName} ${it.lastName}" },
                optionType = "option_1",
                sortOrder = 0,
                players = givingPlayers,
                side = TradeSide.GIVE
            ),
            OptionToInsert(
                id = receiveOptionId,
                optionText = receivingPlayers.joinToString(", ") { "${it.firstName} ${it.lastName}" },
                optionType = "option_2",
                sortOrder = 1,
                players = receivingPlayers,
                side = TradeSide.RECEIVE
            )
        )

        return createPostInDb(
            pollType = "trade",
            weekNumber = null,
            options = options
        )
    }

    suspend fun createStartSitPost(
        players: List<Player>,
        weekNumber: Int?
    ): Post? {
        val options = players.mapIndexed { index, player ->
            OptionToInsert(
                id = UUID.randomUUID().toString(),
                optionText = "${player.firstName} ${player.lastName}",
                optionType = "option_${index + 1}",
                sortOrder = index,
                players = listOf(player),
                side = null
            )
        }

        return createPostInDb(
            pollType = "sit_start",
            weekNumber = weekNumber,
            options = options
        )
    }

    private suspend fun createPostInDb(
        pollType: String,
        weekNumber: Int?,
        options: List<OptionToInsert>
    ): Post? {
        _isCreating.value = true
        _createError.value = null

        return try {
            val supabase = SupabaseClient.client
            val pollId = UUID.randomUUID().toString()
            val autoCloseAtIso = computeAutoCloseAtIso(
                pollType = pollType,
                optionPlayers = options.flatMap { it.players }.distinctBy { it.id }
            )

            // Insert the poll
            supabase.postgrest.from("polls").insert(
                DbPollInsert(
                    id = pollId,
                    creatorId = currentUserId,
                    pollType = pollType,
                    title = "",
                    description = "",
                    resolvedAt = autoCloseAtIso,
                    weekNumber = weekNumber
                )
            )

            // Insert poll options
            if (options.isNotEmpty()) {
                val dbOptions = options.map { opt ->
                    DbPollOptionInsert(
                        id = opt.id,
                        pollId = pollId,
                        optionText = opt.optionText,
                        optionType = opt.optionType,
                        sortOrder = opt.sortOrder
                    )
                }
                supabase.postgrest.from("poll_options").insert(dbOptions)

                // Insert poll_option_players
                val playerLinks = options.flatMap { opt ->
                    opt.players.mapIndexed { idx, player ->
                        DbPollOptionPlayerInsert(
                            pollOptionId = opt.id,
                            playerId = player.id,
                            sortOrder = idx
                        )
                    }
                }
                if (playerLinks.isNotEmpty()) {
                    supabase.postgrest.from("poll_option_players").insert(playerLinks)
                }
            }

            // Build the local Post object to return
            val now = System.currentTimeMillis()
            val pollData = if (options.isNotEmpty()) {
                PollData(
                    id = pollId,
                    postId = pollId,
                    options = options.map { opt ->
                        PollOption(
                            id = opt.id,
                            pollId = pollId,
                            players = opt.players,
                            side = opt.side,
                            voteCount = 0,
                            votePercentage = 0f
                        )
                    },
                    totalVotes = 0,
                    userVote = null
                )
            } else null

            val postType = when (pollType) {
                "trade" -> PostType.TRADE
                "sit_start", "start_sit" -> PostType.START_SIT
                else -> PostType.GENERAL
            }

            _isCreating.value = false

            Post(
                id = pollId,
                userId = currentUserId,
                username = currentUsername,
                userRanks = getPostDisplayRanks(),
                title = "",
                content = "",
                timestamp = now.toString(),
                createdAt = now,
                commentCount = 0,
                postType = postType,
                pollData = pollData,
                weekNumber = weekNumber
            )
        } catch (e: Exception) {
            _isCreating.value = false
            _createError.value = "Failed to create post. Please try again."
            null
        }
    }

    private data class OptionToInsert(
        val id: String,
        val optionText: String,
        val optionType: String,
        val sortOrder: Int,
        val players: List<Player>,
        val side: TradeSide?
    )

    private suspend fun computeAutoCloseAtIso(
        pollType: String,
        optionPlayers: List<Player>
    ): String? {
        val closeAtMillis = when (pollType) {
            "trade" -> if (isNflInSeason()) endOfUpcomingSundayEt() else offSeasonTradeClose()
            "sit_start", "start_sit" -> {
                if (!isNflInSeason()) return null
                earliestKickoffForPlayers(optionPlayers.map { it.id }) ?: nextSundayKickoffEt()
            }
            else -> null
        } ?: return null

        return Instant.ofEpochMilli(closeAtMillis).toString()
    }

    private fun endOfUpcomingSundayEt(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
        cal.set(Calendar.MILLISECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun nextSundayKickoffEt(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
        val now = cal.timeInMillis
        cal.set(Calendar.MILLISECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 13)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 7)
        }
        return cal.timeInMillis
    }

    private fun offSeasonTradeClose(): Long =
        System.currentTimeMillis() + (21L * 24L * 60L * 60L * 1000L)

    private suspend fun earliestKickoffForPlayers(playerIds: List<String>): Long? {
        if (playerIds.isEmpty()) return null

        val rows = runCatching {
            SupabaseClient.client.postgrest.from("players")
                .select { filter { isIn("id", playerIds) } }
                .decodeList<JsonObject>()
        }.getOrElse { return null }

        val candidateKeys = listOf(
            "next_game_at",
            "game_time",
            "kickoff_at",
            "start_time",
            "next_game_time"
        )

        val now = System.currentTimeMillis()
        return rows.mapNotNull { row ->
            candidateKeys
                .asSequence()
                .mapNotNull { key -> row[key]?.toString()?.trim('"') }
                .firstOrNull { it.isNotBlank() }
                ?.let { parseDateTimeToMillis(it) }
        }.filter { it >= now }.minOrNull()
    }

    private fun parseDateTimeToMillis(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
    }
}
