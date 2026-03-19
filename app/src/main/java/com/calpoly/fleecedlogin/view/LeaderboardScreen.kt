package com.calpoly.fleecedlogin.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calpoly.fleecedlogin.model.User
import com.calpoly.fleecedlogin.ui.theme.Bronze
import com.calpoly.fleecedlogin.ui.theme.DarkSurface
import com.calpoly.fleecedlogin.ui.theme.DarkSurfaceVariant
import com.calpoly.fleecedlogin.ui.theme.RetroDark
import com.calpoly.fleecedlogin.ui.theme.RetroPurple
import com.calpoly.fleecedlogin.ui.theme.RetroYellow
import com.calpoly.fleecedlogin.ui.theme.Sage
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.viewmodel.LeaderboardTab
import com.calpoly.fleecedlogin.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = viewModel(),
    onRefresh: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNavItem by remember { mutableIntStateOf(2) }
    var isRefreshing by remember { mutableStateOf(false) }

    val tabs = listOf(
        LeaderboardTab.WEEKLY to "WEEKLY",
        LeaderboardTab.SEASONAL to "SEASONAL",
        LeaderboardTab.ALL_TIME to "ALL TIME"
    )

    Scaffold(
        containerColor = RetroDark,
        bottomBar = {
            FleecedBottomNav(
                selectedItem = selectedNavItem,
                onHomeClick = { selectedNavItem = 0; onNavigateToHome() },
                onAddClick = { selectedNavItem = 1; onNavigateToAdd() },
                onLeaderboardClick = { selectedNavItem = 2 },
                onAccountClick = { selectedNavItem = 3; onNavigateToAccount() }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Banner
                item { FleecedScreenHeader("LEADERBOARD") }

                // Tab pill row
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs.size) { index ->
                            val (tabValue, tabLabel) = tabs[index]
                            val active = uiState.selectedTab == tabValue
                            Surface(
                                modifier = Modifier.clickable { viewModel.selectTab(tabValue) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (active) RetroPurple else DarkSurfaceVariant,
                                border = if (active) null
                                else BorderStroke(1.dp, RetroPurple.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = tabLabel,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 0.5.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Sage
                                )
                            }
                        }
                    }
                }

                // Leaderboard rows
                itemsIndexed(uiState.allUsers) { index, user ->
                    LeaderboardRow(
                        position = index + 1,
                        user = user,
                        displayRanks = viewModel.getDisplayRanks(user.id, user.points),
                        isCurrentUser = user.id == uiState.currentUser?.id,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        )
                    )
                }

                // Empty state
                if (uiState.allUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No rankings yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Sage
                            )
                        }
                    }
                }
                // Bottom padding
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    position: Int,
    user: User,
    displayRanks: List<String>,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val isTopThree = position <= 3

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) RetroPurple.copy(alpha = 0.08f) else DarkSurface
        ),
        border = if (isCurrentUser)
            BorderStroke(1.5.dp, RetroPurple.copy(alpha = 0.7f))
        else
            BorderStroke(1.dp, RetroPurple.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Position badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (position) {
                            1    -> RetroYellow
                            2    -> Sage.copy(alpha = 0.6f)
                            3    -> Bronze
                            else -> DarkSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTopThree) {
                    Text(
                        text = when (position) {
                            1 -> "🥇"
                            2 -> "🥈"
                            else -> "🥉"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Sage
                    )
                }
            }

            // Name + rank badges
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentUser) RetroPurple else Color.White
                    )
                    if (isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = RetroPurple.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "YOU",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.5.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = RetroPurple
                            )
                        }
                    }
                }
                if (displayRanks.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        displayRanks.forEach { rank -> RankBadge(rank = rank) }
                    }
                }
            }

            // Points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${user.points}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isTopThree) VoteGreen else VoteGreen.copy(alpha = 0.75f)
                )
                Text(
                    text = "PTS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = Sage,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
