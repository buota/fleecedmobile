package com.calpoly.fleecedlogin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calpoly.fleecedlogin.data.SupabaseClient
import com.calpoly.fleecedlogin.model.User
import com.calpoly.fleecedlogin.model.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LeaderboardTab {
    WEEKLY, SEASONAL, ALL_TIME
}

data class LeaderboardUiState(
    val currentUser: User? = null,
    val allUsers: List<User> = emptyList(),
    val selectedTab: LeaderboardTab = LeaderboardTab.WEEKLY,
    val seasonalFirstId: String? = null,  // User ID of #1 seasonal player (MVP)
    val allTimeFirstId: String? = null    // User ID of #1 all-time player (GOAT)
)

class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    fun setUser(user: User) {
        _uiState.value = _uiState.value.copy(currentUser = user)
        loadLeaderboard()
    }

    fun selectTab(tab: LeaderboardTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadLeaderboard()
    }

    fun refresh() {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                val profiles = SupabaseClient.client.postgrest
                    .from("users_profile")
                    .select()
                    .decodeList<UserProfile>()

                val users = profiles.map { profile ->
                    User(
                        id = profile.id,
                        username = profile.username,
                        points = profile.totalPoints
                    )
                }.sortedByDescending { it.points }

                val seasonalFirst = users.firstOrNull()?.id
                val allTimeFirst = users.firstOrNull()?.id

                _uiState.value = _uiState.value.copy(
                    allUsers = users,
                    seasonalFirstId = seasonalFirst,
                    allTimeFirstId = allTimeFirst
                )
            } catch (e: Exception) {
                // If DB fetch fails, show just the current user
                val currentUser = _uiState.value.currentUser
                if (currentUser != null) {
                    _uiState.value = _uiState.value.copy(
                        allUsers = listOf(currentUser)
                    )
                }
            }
        }
    }

    /**
     * Returns all display ranks for a user, a user can hold multiple titles
     */
    fun getDisplayRanks(userId: String, points: Int): List<String> {
        val state = _uiState.value
        val ranks = mutableListOf<String>()

        if (userId == state.allTimeFirstId) ranks.add("GOAT")
        if (userId == state.seasonalFirstId) ranks.add("MVP")

        // Always include the point-based rank
        ranks.add(com.calpoly.fleecedlogin.model.getRankForPoints(points).title)

        return ranks
    }
}
