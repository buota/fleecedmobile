package com.calpoly.fleecedlogin.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calpoly.fleecedlogin.model.Post
import com.calpoly.fleecedlogin.model.PostType
import com.calpoly.fleecedlogin.model.TradeSide
import com.calpoly.fleecedlogin.model.VoteType
import com.calpoly.fleecedlogin.model.getRankForPoints
import com.calpoly.fleecedlogin.ui.theme.DarkSurface
import com.calpoly.fleecedlogin.ui.theme.DarkSurfaceVariant
import com.calpoly.fleecedlogin.ui.theme.RetroDark
import com.calpoly.fleecedlogin.ui.theme.RetroPurple
import com.calpoly.fleecedlogin.ui.theme.RetroRed
import com.calpoly.fleecedlogin.ui.theme.RetroYellow
import com.calpoly.fleecedlogin.ui.theme.Sage
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.util.getRelativeTimeString
import com.calpoly.fleecedlogin.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    userDisplayRanks: List<String> = emptyList(),
    onRefresh: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(3) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshPosts()
    }

    LaunchedEffect(uiState.usernameChangeSuccess) {
        if (uiState.usernameChangeSuccess) {
            showChangeUsernameDialog = false
            viewModel.clearUsernameChangeState()
        }
    }

    // Change Username Dialog
    if (showChangeUsernameDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeUsernameDialog = false
                viewModel.clearUsernameChangeState()
            },
            containerColor = DarkSurface,
            title = {
                Text(
                    "CHANGE USERNAME",
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = RetroPurple
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("New Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RetroPurple,
                            unfocusedBorderColor = RetroPurple.copy(alpha = 0.4f),
                            focusedLabelColor = RetroPurple,
                            unfocusedLabelColor = Sage,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    uiState.usernameChangeError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.changeUsername(newUsername) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "SAVE",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangeUsernameDialog = false
                    viewModel.clearUsernameChangeState()
                }) {
                    Text("Cancel", color = Sage)
                }
            }
        )
    }

    Scaffold(
        containerColor = RetroDark,
        bottomBar = {
            FleecedBottomNav(
                selectedItem = selectedItem,
                onHomeClick = { selectedItem = 0; onNavigateToHome() },
                onAddClick = { selectedItem = 1; onNavigateToAdd() },
                onLeaderboardClick = { selectedItem = 2; onNavigateToLeaderboard() },
                onAccountClick = { selectedItem = 3 }
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
                item { FleecedScreenHeader("PROFILE") }

                // Profile Header Card
                item {
                    uiState.user?.let { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.5.dp, RetroPurple.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(RetroPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(44.dp),
                                        tint = RetroPurple
                                    )
                                }

                                // Username + edit button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            newUsername = user.username
                                            showChangeUsernameDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Change username",
                                            modifier = Modifier.size(18.dp),
                                            tint = Sage
                                        )
                                    }
                                }

                                // Email row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Sage
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Sage
                                    )
                                }

                                // Rank badges
                                if (userDisplayRanks.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        userDisplayRanks.forEach { rank -> RankBadge(rank = rank) }
                                    }
                                }
                            }
                        }
                    }
                }

                // Stats Grid
                item {
                    uiState.user?.let { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Posts stat
                            ProfileStatCard(
                                modifier = Modifier.weight(1f),
                                value = "${uiState.recentPosts.size}",
                                label = "POSTS",
                                valueColor = RetroPurple
                            )

                            // Points stat
                            ProfileStatCard(
                                modifier = Modifier.weight(1f),
                                value = "${user.points}",
                                label = "POINTS",
                                valueColor = VoteGreen
                            )

                            // Rank stat
                            val hasGoat = userDisplayRanks.contains("GOAT")
                            val hasMvp = userDisplayRanks.contains("MVP")
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(
                                    width = if (hasGoat || hasMvp) 1.5.dp else 1.dp,
                                    color = when {
                                        hasGoat -> RetroYellow
                                        hasMvp  -> VoteGreen
                                        else    -> RetroPurple.copy(alpha = 0.25f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = getRankForPoints(user.points).title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            hasGoat -> RetroYellow
                                            hasMvp  -> VoteGreen
                                            else    -> RetroPurple
                                        }
                                    )
                                    Text(
                                        text = "RANK",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        color = Sage,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Posts label
                item {
                    Text(
                        text = "RECENT POSTS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = Sage,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp,
                            top = 16.dp, bottom = 4.dp
                        )
                    )
                }

                // Empty state
                if (uiState.recentPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No posts yet. Tap + to create one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Sage
                            )
                        }
                    }
                } else {
                    items(uiState.recentPosts) { post ->
                        PostCard(
                            post = post,
                            onClick = { selectedPostId = post.id }
                        )
                    }
                }

                // Logout Button
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RetroRed),
                        border = BorderStroke(1.5.dp, RetroRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "LOGOUT",
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    // Load fresh comments + vote history from DB whenever a dialog opens
    LaunchedEffect(selectedPostId) {
        selectedPostId?.let {
            viewModel.loadCommentsForPost(it)
            viewModel.loadVoteHistory(it)
        }
    }

    // Post Detail Dialog
    selectedPostId?.let { postId ->
        val currentPost = uiState.recentPosts.find { it.id == postId }
        currentPost?.let { post ->
            PostDetailDialog(
                post = post,
                comments = viewModel.getCommentsForPost(post.id),
                voteHistory = uiState.voteHistory[post.id] ?: emptyList(),
                isVoteHistoryLoading = post.id in uiState.voteHistoryLoadingIds,
                onDismiss = { selectedPostId = null },
                onUpvotePost = { viewModel.votePost(post.id, VoteType.UP) },
                onDownvotePost = { viewModel.votePost(post.id, VoteType.DOWN) },
                onAddComment = { commentText -> viewModel.addCommentToPost(post.id, commentText) },
                onLikeComment = { commentId -> viewModel.likeComment(post.id, commentId) },
                onReplyToComment = { parentCommentId, replyText ->
                    viewModel.replyToComment(post.id, parentCommentId, replyText)
                },
                onVote = { optionId -> viewModel.voteOnPoll(post.id, optionId) }
            )
        }
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = Sage,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PostCard(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: type badge + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostTypeBadge(postType = post.postType)
                Text(
                    text = getRelativeTimeString(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage
                )
            }

            // Username + rank badges
            if (post.username.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${post.username}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                    post.userRanks.forEach { rank -> RankBadge(rank = rank) }
                }
            }

            // Poll player summary
            post.pollData?.let { pollData ->
                val summary = when (post.postType) {
                    PostType.TRADE -> {
                        val give = pollData.options.find { it.side == TradeSide.GIVE }?.players?.size ?: 0
                        val receive = pollData.options.find { it.side == TradeSide.RECEIVE }?.players?.size ?: 0
                        "$give player${if (give != 1) "s" else ""} ⇄ $receive player${if (receive != 1) "s" else ""}"
                    }
                    PostType.START_SIT -> "${pollData.options.size} player options"
                    else -> ""
                }
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage.copy(alpha = 0.7f)
                    )
                }
            }

            // Stats footer
            HorizontalDivider(
                thickness = 1.dp,
                color = RetroPurple.copy(alpha = 0.08f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upvotes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Upvotes",
                        modifier = Modifier.size(14.dp),
                        tint = VoteGreen
                    )
                    Text(
                        text = "${post.upvotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoteGreen
                    )
                }

                // Downvotes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbDown,
                        contentDescription = "Downvotes",
                        modifier = Modifier.size(14.dp),
                        tint = RetroRed
                    )
                    Text(
                        text = "${post.downvotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroRed
                    )
                }

                // Comments
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ModeComment,
                        contentDescription = "Comments",
                        modifier = Modifier.size(14.dp),
                        tint = Sage
                    )
                    Text(
                        text = "${post.commentCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }

                // Poll votes
                post.pollData?.let { pollData ->
                    Text(
                        text = "${pollData.totalVotes} votes",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage
                    )
                }
            }
        }
    }
}
