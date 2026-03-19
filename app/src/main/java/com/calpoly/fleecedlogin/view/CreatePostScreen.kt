package com.calpoly.fleecedlogin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.calpoly.fleecedlogin.model.Player
import com.calpoly.fleecedlogin.model.PostType
import com.calpoly.fleecedlogin.ui.theme.DarkSurface
import com.calpoly.fleecedlogin.ui.theme.DarkSurfaceVariant
import com.calpoly.fleecedlogin.ui.theme.LightPurple
import com.calpoly.fleecedlogin.ui.theme.RetroDark
import com.calpoly.fleecedlogin.ui.theme.RetroOrange
import com.calpoly.fleecedlogin.ui.theme.RetroPurple
import com.calpoly.fleecedlogin.ui.theme.Sage
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.ui.theme.VoteRed
import com.calpoly.fleecedlogin.util.getCurrentNflWeek
import com.calpoly.fleecedlogin.view.FleecedBottomNav
import com.calpoly.fleecedlogin.view.FleecedScreenHeader
import com.calpoly.fleecedlogin.viewmodel.HomeViewModel
import com.calpoly.fleecedlogin.viewmodel.PostViewModel
import com.calpoly.fleecedlogin.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    postViewModel: PostViewModel = viewModel(),
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {}
) {
    var selectedPostType by remember { mutableStateOf(PostType.START_SIT) }
    var selectedNavItem by remember { mutableIntStateOf(1) }

    var givingPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var receivingPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var startSitPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val isCreating by postViewModel.isCreating.collectAsState()
    val createError by postViewModel.createError.collectAsState()

    val isPostEnabled = !isCreating && when (selectedPostType) {
        PostType.TRADE     -> givingPlayers.isNotEmpty() && receivingPlayers.isNotEmpty()
        PostType.START_SIT -> startSitPlayers.size == 2
        PostType.GENERAL   -> false
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RetroPurple,
        unfocusedBorderColor = RetroPurple.copy(alpha = 0.4f),
        focusedLabelColor = RetroPurple,
        unfocusedLabelColor = Sage,
        focusedContainerColor = DarkSurfaceVariant,
        unfocusedContainerColor = DarkSurfaceVariant,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLeadingIconColor = RetroPurple,
        unfocusedLeadingIconColor = Sage,
        focusedTrailingIconColor = Sage,
        unfocusedTrailingIconColor = Sage
    )

    Scaffold(
        containerColor = RetroDark,
        bottomBar = {
            FleecedBottomNav(
                selectedItem = selectedNavItem,
                onHomeClick = { selectedNavItem = 0; onNavigateToHome() },
                onAddClick = { selectedNavItem = 1 },
                onLeaderboardClick = { selectedNavItem = 2; onNavigateToLeaderboard() },
                onAccountClick = { selectedNavItem = 3; onNavigateToAccount() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Banner
            FleecedScreenHeader("CREATE POST")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Post Type Selector ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PostType.TRADE     to "TRADE",
                        PostType.START_SIT to "START/SIT"
                    ).forEach { (type, label) ->
                        val active = selectedPostType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    selectedPostType = type
                                    when (type) {
                                        PostType.TRADE -> {
                                            startSitPlayers = emptyList()
                                        }
                                        PostType.START_SIT -> {
                                            givingPlayers = emptyList()
                                            receivingPlayers = emptyList()
                                        }
                                        PostType.GENERAL -> Unit
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) RetroPurple else DarkSurfaceVariant,
                            border = if (active) null else BorderStroke(1.dp, RetroPurple.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Sage
                                )
                            }
                        }
                    }
                }

                // ── Trade Sections ───────────────────────────────────────
                if (selectedPostType == PostType.TRADE) {
                    PlayerSection(
                        label = "GIVING AWAY",
                        accentColor = LightPurple,
                        players = givingPlayers,
                        onPlayersChanged = { givingPlayers = it },
                        viewModel = postViewModel,
                        fieldColors = fieldColors
                    )
                    PlayerSection(
                        label = "RECEIVING",
                        accentColor = RetroOrange,
                        players = receivingPlayers,
                        onPlayersChanged = { receivingPlayers = it },
                        viewModel = postViewModel,
                        fieldColors = fieldColors
                    )
                }

                // ── Start/Sit Section ────────────────────────────────────
                if (selectedPostType == PostType.START_SIT) {
                    PlayerSection(
                        label = "PLAYER OPTIONS",
                        accentColor = RetroPurple,
                        hint = "Add exactly 2 players",
                        players = startSitPlayers,
                        onPlayersChanged = { startSitPlayers = it },
                        viewModel = postViewModel,
                        fieldColors = fieldColors,
                        maxPlayers = 2
                    )
                }

                // ── Error ────────────────────────────────────────────────
                createError?.let {
                    Text(
                        text = it,
                        color = VoteRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // ── POST Button ──────────────────────────────────────────
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val createdPost = when (selectedPostType) {
                                PostType.TRADE     -> postViewModel.createTradePost(givingPlayers, receivingPlayers)
                                PostType.START_SIT -> postViewModel.createStartSitPost(startSitPlayers, getCurrentNflWeek())
                                PostType.GENERAL   -> null
                            }
                            if (createdPost != null) {
                                homeViewModel.addPost(createdPost)
                                profileViewModel.addPost(createdPost)
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = isPostEnabled,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroPurple,
                        contentColor = Color.White,
                        disabledContainerColor = RetroPurple.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "POST",
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Player Section (Trade sides + Start/Sit)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlayerSection(
    label: String,
    accentColor: Color,
    hint: String = "",
    players: List<Player>,
    onPlayersChanged: (List<Player>) -> Unit,
    viewModel: PostViewModel,
    fieldColors: TextFieldColors,
    maxPlayers: Int = Int.MAX_VALUE
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchResults by viewModel.playerSearchResults.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (hint.isNotBlank()) {
                    Text(
                        text = "· $hint",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }
            }

            // Selected players
            if (players.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    players.forEach { player ->
                        SelectedPlayerItem(
                            player = player,
                            onRemove = { onPlayersChanged(players.filter { it.id != player.id }) }
                        )
                    }
                }
            }

            // Search field (hidden once player limit is reached)
            if (players.size < maxPlayers) OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchPlayers(it)
                    isSearchExpanded = it.isNotEmpty()
                },
                placeholder = { Text("Search players…", color = Sage.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors
            )

            // Search results dropdown
            if (isSearchExpanded && searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.3f))
                ) {
                    LazyColumn {
                        items(searchResults.filter { result ->
                            !players.any { it.id == result.id }
                        }) { player ->
                            PlayerSearchItem(
                                player = player,
                                accentColor = accentColor,
                                onClick = {
                                    onPlayersChanged(players + player)
                                    searchQuery = ""
                                    isSearchExpanded = false
                                    viewModel.clearSearch()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selected Player Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SelectedPlayerItem(
    player: Player,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = VoteGreen.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, VoteGreen.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.firstName} ${player.lastName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${player.position} · ${player.team.teamName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = Sage
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player Search Result Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayerSearchItem(
    player: Player,
    accentColor: Color = RetroPurple,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${player.firstName} ${player.lastName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "${player.position} · ${player.team.teamName}",
                style = MaterialTheme.typography.bodySmall,
                color = Sage
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = accentColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier
                    .size(28.dp)
                    .padding(6.dp),
                tint = accentColor
            )
        }
    }
    HorizontalDivider(
        color = RetroPurple.copy(alpha = 0.10f),
        thickness = 1.dp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Keep legacy composables so other callers still compile
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PostTypeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) RetroPurple else DarkSurfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, RetroPurple.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Sage
            )
        }
    }
}

@Composable
fun TradeSection(
    title: String,
    players: List<Player>,
    onPlayersChanged: (List<Player>) -> Unit,
    viewModel: PostViewModel
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RetroPurple,
        unfocusedBorderColor = RetroPurple.copy(alpha = 0.4f),
        focusedLabelColor = RetroPurple,
        unfocusedLabelColor = Sage,
        focusedContainerColor = DarkSurfaceVariant,
        unfocusedContainerColor = DarkSurfaceVariant,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )
    PlayerSection(
        label = title.uppercase(),
        accentColor = LightPurple,
        players = players,
        onPlayersChanged = onPlayersChanged,
        viewModel = viewModel,
        fieldColors = fieldColors
    )
}

@Composable
fun StartSitSection(
    players: List<Player>,
    onPlayersChanged: (List<Player>) -> Unit,
    viewModel: PostViewModel
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RetroPurple,
        unfocusedBorderColor = RetroPurple.copy(alpha = 0.4f),
        focusedLabelColor = RetroPurple,
        unfocusedLabelColor = Sage,
        focusedContainerColor = DarkSurfaceVariant,
        unfocusedContainerColor = DarkSurfaceVariant,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )
    PlayerSection(
        label = "PLAYER OPTIONS",
        accentColor = RetroPurple,
        hint = "Add at least 2 players",
        players = players,
        onPlayersChanged = onPlayersChanged,
        viewModel = viewModel,
        fieldColors = fieldColors
    )
}
