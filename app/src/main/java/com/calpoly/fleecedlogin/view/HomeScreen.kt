package com.calpoly.fleecedlogin.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import com.calpoly.fleecedlogin.model.Player
import com.calpoly.fleecedlogin.model.PollOption
import com.calpoly.fleecedlogin.model.Post
import com.calpoly.fleecedlogin.model.PostType
import com.calpoly.fleecedlogin.model.TradeSide
import com.calpoly.fleecedlogin.model.VoteType
import com.calpoly.fleecedlogin.model.getRankForPoints
import com.calpoly.fleecedlogin.ui.theme.BarlowCondensedFamily
import com.calpoly.fleecedlogin.ui.theme.Bronze
import com.calpoly.fleecedlogin.ui.theme.DarkSurface
import com.calpoly.fleecedlogin.ui.theme.DarkSurfaceVariant
import com.calpoly.fleecedlogin.ui.theme.LightPurple
import com.calpoly.fleecedlogin.ui.theme.RetroDark
import com.calpoly.fleecedlogin.ui.theme.RetroOrange
import com.calpoly.fleecedlogin.ui.theme.RetroPurple
import com.calpoly.fleecedlogin.ui.theme.RetroYellow
import com.calpoly.fleecedlogin.ui.theme.Sage
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.ui.theme.VoteRed
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.calpoly.fleecedlogin.util.getRelativeTimeString
import com.calpoly.fleecedlogin.viewmodel.FeedSortOrder
import com.calpoly.fleecedlogin.viewmodel.HomeViewModel
import com.calpoly.fleecedlogin.viewmodel.VoteHistoryPoint
import kotlin.math.roundToInt

// Shared header banner — used on every screen
@Composable
fun FleecedScreenHeader(title: String = "FLEECED") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(RetroPurple, RoundedCornerShape(20.dp))
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontStyle = FontStyle.Italic,
                letterSpacing = 5.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.White
        )
    }
}


// Polls screen design tokens
private val PollsBgDeep = Color(0xFF0D1B2A)
private val PollsBgSurface = Color(0xFF1A2740)
private val PollsBgElevated = Color(0xFF243350)
private val PollsAccentBlue = Color(0xFF5B7FD4)
private val PollsGreen = Color(0xFF3D8B40)
private val PollsRed = Color(0xFFC0392B)
private val PollsGreenBright = Color(0xFF4CAF50)
private val PollsRedBright = Color(0xFFE53935)
private val PollsTextMuted = Color(0xFFA0B0C8)
private val PollsBorder = Color(0xFF3A5080)
private val PollsPill = Color(0xFF2A3A55)

@Composable
private fun YourPickBadge(
    color: Color = PollsAccentBlue,
    modifier: Modifier = Modifier
) {
    val labelColor = Color(0xFFEAF2FF)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = labelColor
            )
            Text(
                text = "YOUR PICK",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

private data class MockPollData(
    val leftName: String,
    val leftVotes: Int,
    val leftPct: Int,
    val leftFp: String,
    val rightName: String,
    val rightVotes: Int,
    val rightPct: Int,
    val rightFp: String,
    val badge: String? = null,      // "CORRECT", "INCORRECT", or null
    val leftIsWinner: Boolean = true // true → left=green, right=red
)

private fun voteChartLabel(option: PollOption, postType: PostType): String {
    val playersLabel = option.players.joinToString(" + ") { player ->
        "${player.firstName.firstOrNull() ?: ""}. ${player.lastName}"
    }.ifBlank { "—" }

    return when (postType) {
        PostType.TRADE -> {
            val side = when (option.side) {
                TradeSide.GIVE -> "GIVE"
                TradeSide.RECEIVE -> "RECEIVE"
                null -> "SIDE"
            }
            "$side: $playersLabel"
        }
        PostType.START_SIT -> playersLabel
        else -> playersLabel
    }
}

private fun shouldRenderTradeCard(post: Post): Boolean {
    if (post.postType == PostType.TRADE) return true
    val options = post.pollData?.options ?: return false
    if (options.size < 2) return false
    val hasTradeSides = options.any { it.side == TradeSide.GIVE } &&
        options.any { it.side == TradeSide.RECEIVE }
    val hasMultiPlayerOption = options.any { it.players.size > 1 }
    return hasTradeSides || hasMultiPlayerOption
}


@Composable
private fun TeamJerseyIcon(
    teamColor: Color,
    sizeDp: Int,
    contentDescription: String
) {
    fun blend(base: Color, target: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 1f)
        return Color(
            red = base.red * (1f - t) + target.red * t,
            green = base.green * (1f - t) + target.green * t,
            blue = base.blue * (1f - t) + target.blue * t,
            alpha = 1f
        )
    }

    fun toHexRgb(color: Color): String {
        val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    val context = LocalContext.current
    val svgTemplate = remember(context) {
        context.assets.open("football-jersey-svgrepo-com.svg")
            .bufferedReader()
            .use { it.readText() }
    }
    val jerseyLight = remember(teamColor) { blend(teamColor, Color.White, 0.08f) }
    val jerseyDark = remember(teamColor) { blend(teamColor, Color.Black, 0.12f) }
    val themedSvgBytes = remember(svgTemplate, jerseyLight, jerseyDark) {
        svgTemplate
            .replace("#FF633E", toHexRgb(jerseyLight))
            .replace("#FF3100", toHexRgb(jerseyDark))
            .toByteArray()
    }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val request = remember(context, themedSvgBytes) {
        ImageRequest.Builder(context)
            .data(themedSvgBytes)
            .crossfade(false)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = Modifier.size(sizeDp.dp)
    ) {
        if (painter.state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        } else {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(PollsBgElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size((sizeDp * 0.58f).dp),
                    tint = teamColor
                )
            }
        }
    }
}

// Polls Feed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    userDisplayRanks: List<String> = emptyList(),
    onRefresh: () -> Unit = {},
    onNavigateToAdd: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val visiblePosts = remember(uiState.posts) {
        uiState.posts.filter { (it.pollData?.options?.size ?: 0) >= 2 }
    }
    var selectedItem by remember { mutableIntStateOf(0) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PollsBgDeep,
        // Bottom nav
        bottomBar = {
            FleecedBottomNav(
                selectedItem = selectedItem,
                onHomeClick = { selectedItem = 0 },
                onAddClick = { selectedItem = 1; onNavigateToAdd() },
                onLeaderboardClick = { selectedItem = 2; onNavigateToLeaderboard() },
                onAccountClick = { selectedItem = 3; onNavigateToAccount() }
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(PollsBgDeep),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // POLLS header banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp)
                            .background(PollsAccentBlue, RoundedCornerShape(14.dp))
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "POLLS",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = BarlowCondensedFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Poll type toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PollTypeToggle(
                                label = "START / SIT",
                                active = uiState.postTypeFilter == PostType.START_SIT,
                                onClick = { viewModel.setPostTypeFilter(PostType.START_SIT) }
                            )
                            PollTypeToggle(
                                label = "TRADE",
                                active = uiState.postTypeFilter == PostType.TRADE,
                                onClick = { viewModel.setPostTypeFilter(PostType.TRADE) }
                            )
                        }
                    }
                }

                // Filter dropdowns
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sort order
                        val sortLabel = when (uiState.sortOrder) {
                            FeedSortOrder.TOP_RATED    -> "TOP RATED"
                            FeedSortOrder.NEWEST_FIRST -> "NEWEST FIRST"
                        }
                        FilterDropdownPill(
                            label    = sortLabel,
                            options  = listOf("TOP RATED", "NEWEST FIRST"),
                            selected = uiState.sortOrder.ordinal,
                            modifier = Modifier.width(162.dp),
                            onSelect = { index ->
                                viewModel.setSortOrder(
                                    if (index == 0) FeedSortOrder.TOP_RATED else FeedSortOrder.NEWEST_FIRST
                                )
                            }
                        )
                    }
                }

                // Poll feed
                // uiState.posts: type-filtered → week-filtered (trades bypass week) → sorted
                if (visiblePosts.isNotEmpty()) {
                    items(visiblePosts, key = { it.id }) { post ->
                        if (shouldRenderTradeCard(post)) {
                            TradeFeedCard(
                                post = post,
                                onClick = { selectedPostId = post.id },
                                onVote = { optionId -> viewModel.voteOnPoll(post.id, optionId) }
                            )
                        } else {
                            PollCard(
                                post = post,
                                onClick = { selectedPostId = post.id },
                                onVote = { optionId -> viewModel.voteOnPoll(post.id, optionId) }
                            )
                        }
                    }
                } else if (uiState.posts.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Posts loaded, but poll options are missing.",
                                color = PollsTextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (uiState.totalPostsCount == 0) {
                    // No posts loaded from DB
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = PollsAccentBlue,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Loading polls...",
                                        color = PollsTextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = uiState.loadError ?: "No polls found",
                                        color = PollsTextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else if (uiState.typePostsCount == 0) {
                    // Posts exist but none of this type
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val typeName = if (uiState.postTypeFilter == PostType.START_SIT) "Start/Sit" else "Trade"
                            Text(
                                text = "No $typeName polls yet",
                                color = PollsTextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Posts exist, but none match the currently visible poll card shape
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No polls available for the current filters",
                                    color = PollsTextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fetch fresh comments + vote history from DB
    LaunchedEffect(selectedPostId) {
        selectedPostId?.let {
            viewModel.loadCommentsForPost(it)
            viewModel.loadVoteHistory(it)
        }
    }

    // Post detail dialog
    // Prefer the filtered-list entry so vote updates are reactive, fall back to the
    // full backing list so the dialog stays open when the user changes type/week filters
    selectedPostId?.let { postId ->
        val post = uiState.posts.find { it.id == postId } ?: viewModel.getPostById(postId)
        post?.let {
            PostDetailDialog(
                post = it,
                comments = viewModel.getCommentsForPost(it.id),
                voteHistory = uiState.voteHistory[it.id] ?: emptyList(),
                isVoteHistoryLoading = it.id in uiState.voteHistoryLoadingIds,
                onDismiss = { selectedPostId = null },
                onUpvotePost = { viewModel.votePost(it.id, VoteType.UP) },
                onDownvotePost = { viewModel.votePost(it.id, VoteType.DOWN) },
                onAddComment = { commentText ->
                    viewModel.addCommentToPost(it.id, commentText)
                },
                onLikeComment = { commentId -> viewModel.likeComment(it.id, commentId) },
                onReplyToComment = { parentId, replyText ->
                    viewModel.replyToComment(it.id, parentId, replyText)
                },
                onVote = { optionId -> viewModel.voteOnPoll(it.id, optionId) }
            )
        }
    }
}

// Poll Card
@Composable
fun PollCard(
    post: Post,
    onClick: () -> Unit,
    onVote: (String) -> Unit
) {
    val pollData = post.pollData ?: return
    if (pollData.options.size < 2) return

    val leftOption = pollData.options[0]
    val rightOption = pollData.options[1]
    val hasVoted = pollData.userVote != null
    val isClosed = pollData.status == "closed"
    val isResolved = isClosed && pollData.correctOptionId != null
    val userVotedCorrectly = isResolved && pollData.userVote == pollData.correctOptionId
    val userVotedIncorrectly = isResolved && pollData.userVote != null && pollData.userVote != pollData.correctOptionId
    val votedLeft = hasVoted && pollData.userVote == leftOption.id
    val votedRight = hasVoted && pollData.userVote == rightOption.id

    // Green/red only for closed polls, neutral accent blue for open voted polls
    val leftIsWinner = when {
        isResolved -> pollData.correctOptionId == leftOption.id
        else -> leftOption.votePercentage >= rightOption.votePercentage
    }
    val leftBg = when {
        isClosed -> if (leftIsWinner) PollsGreen else PollsRed
        !votedRight && hasVoted -> PollsAccentBlue.copy(alpha = 0.5f)
        votedRight -> PollsBgElevated.copy(alpha = 0.5f)
        else -> PollsBgElevated
    }
    val rightBg = when {
        isClosed -> if (!leftIsWinner) PollsGreen else PollsRed
        votedRight -> PollsAccentBlue.copy(alpha = 0.5f)
        hasVoted -> PollsBgElevated.copy(alpha = 0.5f)
        else -> PollsBgElevated
    }
    val leftFpColor = if (isClosed && leftIsWinner) PollsGreenBright else if (isClosed) PollsRedBright else PollsAccentBlue
    val rightFpColor = if (isClosed && !leftIsWinner) PollsGreenBright else if (isClosed) PollsRedBright else PollsAccentBlue

    fun playerLabel(opt: PollOption): String =
        opt.players.joinToString(" + ") { p ->
            "${p.firstName.firstOrNull() ?: ""}. ${p.lastName}"
        }.ifBlank { "—" }
    fun playerImageUrl(opt: PollOption): String? = opt.players.firstOrNull()?.imageUrl
    fun playerTeamColor(opt: PollOption): Color =
        opt.players.firstOrNull()?.team?.teamColor?.takeIf { it != Color.Unspecified } ?: PollsAccentBlue
    val useTeamJersey = post.postType == PostType.START_SIT

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = PollsBgSurface,
            border = BorderStroke(1.dp, PollsBorder)
        ) {
            Column {
                // Voting halves
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Left half
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(leftBg)
                            .clickable(enabled = !isClosed) { onVote(leftOption.id) }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                PollHalfContent(
                                    playerName = playerLabel(leftOption),
                                    imageUrl   = playerImageUrl(leftOption),
                                    useTeamJersey = useTeamJersey,
                                    jerseyColor = playerTeamColor(leftOption),
                                    votes     = leftOption.voteCount,
                                    pct       = leftOption.votePercentage.toInt(),
                                    fpText    = if (isResolved) pollData.player1Points?.let { "${it} FP" } else null,
                                    fpColor   = leftFpColor
                                )
                            }
                            if (votedLeft) {
                                YourPickBadge(
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(PollsBgDeep)
                    )

                    // Right half
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(rightBg)
                            .clickable(enabled = !isClosed) { onVote(rightOption.id) }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.align(Alignment.Center)) {
                                PollHalfContent(
                                    playerName = playerLabel(rightOption),
                                    imageUrl = playerImageUrl(rightOption),
                                    useTeamJersey = useTeamJersey,
                                    jerseyColor = playerTeamColor(rightOption),
                                    votes = rightOption.voteCount,
                                    pct = rightOption.votePercentage.toInt(),
                                    fpText = if (isResolved) pollData.player2Points?.let { "${it} FP" } else null,
                                    fpColor = rightFpColor
                                )
                            }
                            if (votedRight) {
                                YourPickBadge(
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PollsBgDeep.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${post.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PollsTextMuted
                        )
                        post.userRanks.forEach { RankBadge(rank = it) }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Outlined.ModeComment, contentDescription = null, modifier = Modifier.size(12.dp), tint = PollsTextMuted)
                            Text("${post.commentCount}", style = MaterialTheme.typography.labelSmall, color = PollsTextMuted)
                        }
                        Text(getRelativeTimeString(post.createdAt), style = MaterialTheme.typography.labelSmall, color = PollsTextMuted.copy(alpha = 0.6f))
                    }
                }
            }
        }

        when {
            userVotedCorrectly -> Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = PollsGreenBright
            ) { Text("CORRECT +10", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            userVotedIncorrectly -> Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = PollsRedBright
            ) { Text("INCORRECT", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            isClosed -> Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = PollsRedBright
            ) { Text("CLOSED", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}

// Trade Feed Card

@Composable
fun TradeFeedCard(
    post: Post,
    onClick: () -> Unit,
    onVote: (String) -> Unit
) {
    val pollData = post.pollData ?: return
    val giveOpt = pollData.options.find { it.side == TradeSide.GIVE }
                  ?: pollData.options.getOrNull(0) ?: return
    val receiveOpt = pollData.options.find { it.side == TradeSide.RECEIVE }
                  ?: pollData.options.getOrNull(1) ?: return

    val hasVoted = pollData.userVote != null
    val isClosed = pollData.status == "closed"
    val votedGive = hasVoted && pollData.userVote == giveOpt.id
    val votedReceive = hasVoted && pollData.userVote == receiveOpt.id
    val giveIsWinner = giveOpt.votePercentage >= receiveOpt.votePercentage

    val giveBg = when {
        isClosed -> if (giveIsWinner) PollsGreen else PollsRed
        votedGive -> PollsAccentBlue.copy(alpha = 0.5f)
        hasVoted -> PollsBgElevated.copy(alpha = 0.5f)
        else -> PollsBgElevated
    }
    val receiveBg = when {
        isClosed -> if (!giveIsWinner) PollsGreen else PollsRed
        votedReceive -> PollsAccentBlue.copy(alpha = 0.5f)
        hasVoted -> PollsBgElevated.copy(alpha = 0.5f)
        else -> PollsBgElevated
    }
    val giveLabelColor = when {
        isClosed -> Color.White.copy(alpha = 0.75f)
        votedGive -> PollsAccentBlue
        else -> PollsTextMuted
    }
    val receiveLabelColor = when {
        isClosed -> Color.White.copy(alpha = 0.75f)
        votedReceive -> PollsAccentBlue
        else -> PollsTextMuted
    }
    val giveBarColor = when {
        isClosed -> if (giveIsWinner) PollsGreenBright else PollsRedBright
        else -> PollsAccentBlue
    }
    val receiveBarColor = when {
        isClosed -> if (!giveIsWinner) PollsGreenBright else PollsRedBright
        else -> PollsAccentBlue
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PollsBgSurface,
        border = BorderStroke(1.dp, PollsBorder)
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Give side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(giveBg)
                    .clickable(enabled = !isClosed) { onVote(giveOpt.id) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "GIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = giveLabelColor
                )
                if (votedGive) {
                    Spacer(Modifier.height(6.dp))
                    YourPickBadge()
                }
                Spacer(Modifier.height(8.dp))
                giveOpt.players.forEach { player ->
                    Text(
                        text = "${player.firstName.firstOrNull() ?: ""}. ${player.lastName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.position} · ${player.team.teamName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = giveLabelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (hasVoted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${giveOpt.votePercentage.toInt()}%",
                        fontFamily = BarlowCondensedFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = giveBarColor
                    )
                    LinearProgressIndicator(
                        progress = { giveOpt.votePercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = giveBarColor.copy(alpha = 0.85f),
                        trackColor = giveBarColor.copy(alpha = 0.2f)
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(PollsBgDeep)
            )

            // Recieve side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(receiveBg)
                    .clickable(enabled = !isClosed) { onVote(receiveOpt.id) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = "RECEIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = receiveLabelColor
                )
                if (votedReceive) {
                    Spacer(Modifier.height(6.dp))
                    YourPickBadge()
                }
                Spacer(Modifier.height(8.dp))
                receiveOpt.players.forEach { player ->
                    Text(
                        text = "${player.firstName.firstOrNull() ?: ""}. ${player.lastName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.position} · ${player.team.teamName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = receiveLabelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (hasVoted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${receiveOpt.votePercentage.toInt()}%",
                        fontFamily = BarlowCondensedFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = receiveBarColor
                    )
                    LinearProgressIndicator(
                        progress = { receiveOpt.votePercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = receiveBarColor.copy(alpha = 0.85f),
                        trackColor = receiveBarColor.copy(alpha = 0.2f)
                    )
                }
            }
        }
        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PollsBgDeep.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("@${post.username}", style = MaterialTheme.typography.labelSmall, color = PollsTextMuted)
                post.userRanks.forEach { RankBadge(rank = it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Outlined.ModeComment, contentDescription = null, modifier = Modifier.size(12.dp), tint = PollsTextMuted)
                    Text("${post.commentCount}", style = MaterialTheme.typography.labelSmall, color = PollsTextMuted)
                }
                Text(getRelativeTimeString(post.createdAt), style = MaterialTheme.typography.labelSmall, color = PollsTextMuted.copy(alpha = 0.6f))
            }
        }
        } // Column
    }
}

// Poll half content
@Composable
private fun PollHalfContent(
    playerName: String,
    votes: Int,
    pct: Int,
    fpText: String?,
    fpColor: Color,
    imageUrl: String? = null,
    useTeamJersey: Boolean = false,
    jerseyColor: Color = PollsAccentBlue
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Start/Sit: always show the shared jersey SVG tinted to team color
        if (useTeamJersey) {
            TeamJerseyIcon(
                teamColor = jerseyColor,
                sizeDp = 56,
                contentDescription = "$playerName jersey"
            )
        } else if (imageUrl != null) {
            // Other post types: player image asset with fallback icon
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = playerName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PollsBgElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = PollsTextMuted
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PollsBgElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = PollsTextMuted
                )
            }
        }

        // Vote count + percentage on same line
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$votes",
                fontFamily = BarlowCondensedFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = " ($pct%)",
                fontFamily = BarlowCondensedFamily,
                fontSize = 14.sp,
                color = PollsTextMuted
            )
        }

        // FP label
        fpText?.let {
            Text(
                text = it,
                fontFamily = BarlowCondensedFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fpColor
            )
        }

        // Player name — all-caps, condensed, centered
        Text(
            text = playerName.uppercase(),
            fontFamily = BarlowCondensedFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = 0.7.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Poll type toggle pill
@Composable
private fun PollTypeToggle(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (active) PollsAccentBlue else PollsPill,
        border = if (active) null else BorderStroke(1.dp, PollsBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else PollsTextMuted,
                fontSize = 15.sp
            )
        }
    }
}
// Filter dropdown pill — shows a dropdown menu with options
@Composable
private fun FilterDropdownPill(
    label: String,
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = if (expanded) PollsAccentBlue.copy(alpha = 0.25f) else PollsPill,
            border = BorderStroke(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) PollsAccentBlue else PollsBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontFamily = BarlowCondensedFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (expanded) Color.White else Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (expanded) "▴" else "▾",
                    color = PollsTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PollsBgSurface)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontFamily = BarlowCondensedFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PollsAccentBlue else Color.White
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isSelected) PollsAccentBlue.copy(alpha = 0.15f) else Color.Transparent
                    )
                )
            }
        }
    }
}

// Bottom Nav
@Composable
fun FleecedBottomNav(
    selectedItem: Int,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 0.dp
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = RetroPurple,
            selectedTextColor = RetroPurple,
            unselectedIconColor = Sage,
            unselectedTextColor = Sage,
            indicatorColor = RetroPurple.copy(alpha = 0.15f)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
            selected = selectedItem == 0,
            onClick = onHomeClick,
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Add, contentDescription = "Post") },
            label = { Text("Post", style = MaterialTheme.typography.labelSmall) },
            selected = selectedItem == 1,
            onClick = onAddClick,
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Ranks") },
            label = { Text("Ranks", style = MaterialTheme.typography.labelSmall) },
            selected = selectedItem == 2,
            onClick = onLeaderboardClick,
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Account") },
            label = { Text("Account", style = MaterialTheme.typography.labelSmall) },
            selected = selectedItem == 3,
            onClick = onAccountClick,
            colors = itemColors
        )
    }
}

// Rank Badge
@Composable
fun RankBadge(rank: String) {
    val bg = when (rank) {
        "GOAT" -> RetroYellow
        "MVP"  -> VoteGreen
        "Hall of Famer" -> RetroYellow.copy(alpha = 0.2f)
        "All-Pro", "Pro Bowler" -> RetroOrange.copy(alpha = 0.2f)
        else   -> DarkSurfaceVariant
    }
    val textColor = when (rank) {
        "GOAT" -> Color(0xFF2A1A00)
        "MVP"  -> Color(0xFF0A2A10)
        "Hall of Famer" -> RetroYellow
        "All-Pro", "Pro Bowler" -> RetroOrange
        else   -> Sage
    }
    val border = when (rank) {
        "GOAT" -> BorderStroke(1.dp, RetroYellow.copy(alpha = 0.8f))
        "MVP"  -> BorderStroke(1.dp, VoteGreen.copy(alpha = 0.8f))
        "Hall of Famer" -> BorderStroke(1.dp, RetroYellow.copy(alpha = 0.5f))
        "All-Pro", "Pro Bowler" -> BorderStroke(1.dp, RetroOrange.copy(alpha = 0.5f))
        else   -> BorderStroke(1.dp, Sage.copy(alpha = 0.3f))
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg, border = border) {
        Text(
            text = rank.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// Post Type Badge
@Composable
fun PostTypeBadge(postType: PostType) {
    if (postType == PostType.GENERAL) return
    val (bg, border, text) = when (postType) {
        PostType.TRADE -> Triple(LightPurple.copy(alpha = 0.2f), LightPurple.copy(alpha = 0.6f), LightPurple)
        PostType.START_SIT -> Triple(RetroPurple.copy(alpha = 0.2f), RetroPurple.copy(alpha = 0.6f), RetroPurple)
        else -> Triple(Sage.copy(alpha = 0.1f), Sage.copy(alpha = 0.3f), Sage)
    }
    val label = when (postType) {
        PostType.TRADE -> "TRADE"
        PostType.START_SIT -> "START / SIT"
        else -> ""
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Bold,
            color = text
        )
    }
}
// Trade Voting
@Composable
fun TradeVotingSection(
    pollData: com.calpoly.fleecedlogin.model.PollData,
    onVote: (String) -> Unit,
    isClosed: Boolean = false
) {
    val giveOption = pollData.options.find { it.side == TradeSide.GIVE }
    val receiveOption = pollData.options.find { it.side == TradeSide.RECEIVE }
    val hasVoted = pollData.userVote != null
    val giveIsWinner = (giveOption?.votePercentage ?: 0f) >= (receiveOption?.votePercentage ?: 0f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        giveOption?.let { option ->
            TradeOptionCard(
                title = "GIVE",
                option = option,
                isSelected = pollData.userVote == option.id,
                hasVoted = hasVoted,
                isClosed = isClosed,
                isWinner = isClosed && giveIsWinner,
                onVote = { onVote(option.id) },
                modifier = Modifier.weight(1f)
            )
        }
        receiveOption?.let { option ->
            TradeOptionCard(
                title = "GET",
                option = option,
                isSelected = pollData.userVote == option.id,
                hasVoted = hasVoted,
                isClosed = isClosed,
                isWinner = isClosed && !giveIsWinner,
                onVote = { onVote(option.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TradeOptionCard(
    title: String,
    option: PollOption,
    isSelected: Boolean,
    hasVoted: Boolean,
    isClosed: Boolean,
    isWinner: Boolean = false,
    onVote: () -> Unit,
    modifier: Modifier = Modifier
) {
    // accentColor drives label, icon, player name, pct text, and bar
    val accentColor = when {
        isClosed && isWinner -> PollsGreenBright
        isClosed -> PollsRedBright
        isSelected -> PollsAccentBlue
        else -> Sage
    }
    val cardBg = when {
        isClosed && isWinner -> PollsGreen.copy(alpha = 0.12f)
        isClosed -> PollsRed.copy(alpha = 0.08f)
        isSelected -> PollsAccentBlue.copy(alpha = 0.12f)
        hasVoted -> DarkSurfaceVariant.copy(alpha = 0.5f)
        else -> DarkSurfaceVariant
    }
    val cardBorder = when {
        isClosed && isWinner -> BorderStroke(2.dp, PollsGreenBright.copy(alpha = 0.7f))
        isClosed -> BorderStroke(1.dp, PollsRedBright.copy(alpha = 0.4f))
        isSelected -> BorderStroke(2.dp, PollsAccentBlue.copy(alpha = 0.7f))
        hasVoted -> BorderStroke(1.dp, Sage.copy(alpha = 0.15f))
        else -> BorderStroke(1.dp, PollsBorder)
    }

    Card(
        modifier = modifier.clickable(enabled = !isClosed, onClick = onVote),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Label row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (isSelected || (isClosed && isWinner)) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = accentColor
                    )
                }
            }
            if (isSelected && hasVoted) {
                Spacer(Modifier.height(6.dp))
                YourPickBadge(color = PollsAccentBlue)
            }

            Spacer(Modifier.height(8.dp))

            // Players — name + position/team on two lines each
            val isHighlighted = isSelected || (isClosed && isWinner)
            option.players.forEach { player ->
                Text(
                    text = "${player.firstName.firstOrNull() ?: ""}. ${player.lastName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighlighted) accentColor else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.position} · ${player.team.teamName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHighlighted) accentColor.copy(alpha = 0.65f) else Sage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }

            // Results (after voting)
            if (hasVoted) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${option.votePercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) accentColor else Sage.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${option.voteCount} votes",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHighlighted) accentColor.copy(alpha = 0.65f) else Sage.copy(alpha = 0.4f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { option.votePercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isHighlighted) accentColor else Sage.copy(alpha = 0.25f),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

// Start/Sit Voting

@Composable
fun StartSitVotingSection(
    pollData: com.calpoly.fleecedlogin.model.PollData,
    onVote: (String) -> Unit,
    isClosed: Boolean = false
) {
    val hasVoted = pollData.userVote != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pollData.options.forEach { option ->
            StartSitOptionCard(
                option = option,
                isSelected = pollData.userVote == option.id,
                hasVoted = hasVoted,
                isClosed = isClosed,
                onVote = { onVote(option.id) }
            )
        }
    }
}

@Composable
fun StartSitOptionCard(
    option: PollOption,
    isSelected: Boolean,
    hasVoted: Boolean,
    isClosed: Boolean,
    onVote: () -> Unit
) {
    val cardBg = when {
        isSelected -> VoteGreen.copy(alpha = 0.12f)
        hasVoted -> DarkSurfaceVariant.copy(alpha = 0.5f)
        else -> DarkSurfaceVariant
    }
    val cardBorder = when {
        isSelected -> BorderStroke(2.dp, VoteGreen.copy(alpha = 0.7f))
        hasVoted -> BorderStroke(1.dp, Sage.copy(alpha = 0.15f))
        else -> BorderStroke(1.dp, RetroPurple.copy(alpha = 0.3f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isClosed, onClick = onVote),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = VoteGreen
                        )
                    }
                    option.players.firstOrNull()?.let { player ->
                        TeamJerseyIcon(
                            teamColor = if (player.team.teamColor == Color.Unspecified) PollsAccentBlue else player.team.teamColor,
                            sizeDp = 34,
                            contentDescription = "${player.team.teamName} jersey"
                        )
                        Column {
                            if (isSelected && hasVoted) {
                                YourPickBadge(color = VoteGreen)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = "${player.firstName.firstOrNull() ?: ""}. ${player.lastName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) VoteGreen else Color.White
                            )
                            Text(
                                text = "${player.position} · ${player.team.teamName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) VoteGreen.copy(alpha = 0.65f) else Sage
                            )
                        }
                    }
                }

                // Percentage + count
                if (hasVoted) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${option.votePercentage.toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) VoteGreen else Sage.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${option.voteCount} votes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Progress bar
            if (hasVoted) {
                LinearProgressIndicator(
                    progress = { option.votePercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isSelected) VoteGreen else Sage.copy(alpha = 0.25f),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

// Post detail full-screen dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailDialog(
    post: Post,
    comments: List<com.calpoly.fleecedlogin.model.Comment>,
    voteHistory: List<VoteHistoryPoint> = emptyList(),
    isVoteHistoryLoading: Boolean = false,
    onDismiss: () -> Unit,
    onUpvotePost: () -> Unit,
    onDownvotePost: () -> Unit,
    onAddComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onReplyToComment: (String, String) -> Unit,
    onVote: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var sortByNew by remember { mutableStateOf(false) }

    val topLevelComments = comments.filter { it.parentCommentId == null }
    val sortedComments = if (sortByNew)
        topLevelComments.sortedByDescending { it.createdAt }
    else
        topLevelComments.sortedByDescending { it.likes }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = RetroDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top bar
                Surface(
                    color = DarkSurface,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = RetroPurple
                            )
                        }
                        Text(
                            text = "POST DETAILS",
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Post card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Type badge + timestamp
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

                                if (post.username.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@${post.username}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Sage
                                        )
                                        post.userRanks.forEach { rank -> RankBadge(rank = rank) }
                                    }
                                }

                                // Poll voting
                                post.pollData?.let { pollData ->
                                    val isClosed = pollData.status == "closed"
                                    val isResolved = isClosed && pollData.correctOptionId != null
                                    val votedCorrect = isResolved && pollData.userVote == pollData.correctOptionId
                                    val votedIncorrect = isResolved && pollData.userVote != null && pollData.userVote != pollData.correctOptionId

                                    when {
                                        votedCorrect -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = VoteGreen.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, VoteGreen)
                                            ) {
                                                Text(
                                                    text = "CORRECT +10",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VoteGreen
                                                )
                                            }
                                        }
                                        votedIncorrect -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = VoteRed.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, VoteRed)
                                            ) {
                                                Text(
                                                    text = "INCORRECT",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VoteRed
                                                )
                                            }
                                        }
                                        isClosed -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = VoteRed.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, VoteRed)
                                            ) {
                                                Text(
                                                    text = "CLOSED",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VoteRed
                                                )
                                            }
                                        }
                                    }

                                    // Vote split bar (if voted)
                                    if (pollData.userVote != null && pollData.totalVotes > 0) {
                                        val leadingOption = pollData.options.maxByOrNull { it.voteCount }
                                        val leadingPct = (leadingOption?.votePercentage ?: 0f) / 100f

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(VoteRed.copy(alpha = 0.35f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(leadingPct)
                                                        .fillMaxHeight()
                                                        .background(VoteGreen)
                                                )
                                            }
                                            Text(
                                                text = "${pollData.totalVotes} TOTAL VOTES",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    letterSpacing = 1.sp
                                                ),
                                                color = Sage,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    when (post.postType) {
                                        PostType.TRADE -> TradeVotingSection(pollData = pollData, onVote = onVote, isClosed = pollData.status == "closed")
                                        PostType.START_SIT -> StartSitVotingSection(pollData = pollData, onVote = onVote, isClosed = pollData.status == "closed")
                                        else -> {}
                                    }
                                }

                                HorizontalDivider(color = RetroPurple.copy(alpha = 0.15f))

                                // Upvote / downvote / comment count
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable(onClick = onUpvotePost)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (post.userVoteType == VoteType.UP) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = "Upvote",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (post.userVoteType == VoteType.UP) RetroPurple else Sage
                                        )
                                        Text(
                                            text = "${post.upvotes}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (post.userVoteType == VoteType.UP) RetroPurple else Sage
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable(onClick = onDownvotePost)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (post.userVoteType == VoteType.DOWN) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                            contentDescription = "Downvote",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (post.userVoteType == VoteType.DOWN) VoteRed else Sage
                                        )
                                        Text(
                                            text = "${post.downvotes}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (post.userVoteType == VoteType.DOWN) VoteRed else Sage
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ModeComment,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Sage
                                        )
                                        Text(
                                            text = "${comments.size} comments",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Sage
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Vote trend chart
                    post.pollData?.let { pollData ->
                        if (pollData.options.size >= 2) {
                            item {
                                val option1 = pollData.options.getOrNull(0)
                                val option2 = pollData.options.getOrNull(1)
                                val opt1Label = option1?.let { voteChartLabel(it, post.postType) } ?: "Option 1"
                                val opt2Label = option2?.let { voteChartLabel(it, post.postType) } ?: "Option 2"
                                val chartDomainStart = if (post.createdAt > 0L) post.createdAt
                                                       else System.currentTimeMillis() - 86_400_000L
                                val pollClose = pollData.resolvedAt ?: System.currentTimeMillis()
                                val chartDomainEnd = if (pollClose > chartDomainStart) pollClose
                                                     else chartDomainStart + 60_000L

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, PollsBorder)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text(
                                            text = "VOTE TREND",
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = PollsTextMuted
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        KalshiVoteChart(
                                            points = voteHistory,
                                            option1Label = opt1Label,
                                            option2Label = opt2Label,
                                            domainStartMs = chartDomainStart,
                                            domainEndMs = chartDomainEnd,
                                            isLoading = isVoteHistoryLoading,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Comments header + sort toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMENTS",
                                style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            // Sort toggle pills
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("BEST" to false, "NEW" to true).forEach { (label, isNew) ->
                                    val active = sortByNew == isNew
                                    Surface(
                                        modifier = Modifier.clickable { sortByNew = isNew },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (active) RetroPurple else DarkSurfaceVariant,
                                        border = if (active) null else BorderStroke(1.dp, Sage.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else Sage
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Empty state
                    if (sortedComments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Be the first!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Sage
                                )
                            }
                        }
                    } else {
                        items(sortedComments) { comment ->
                            val replies = comments
                                .filter { it.parentCommentId == comment.id }
                                .sortedByDescending { it.likes }
                            CommentItem(
                                comment = comment,
                                replies = replies,
                                isReplying = replyingToCommentId == comment.id,
                                replyText = if (replyingToCommentId == comment.id) replyText else "",
                                onReplyTextChange = { replyText = it },
                                onLike = { onLikeComment(comment.id) },
                                onReply = {
                                    replyingToCommentId = if (replyingToCommentId == comment.id) null else comment.id
                                    replyText = ""
                                },
                                onSubmitReply = {
                                    if (replyText.isNotBlank()) {
                                        onReplyToComment(comment.id, replyText)
                                        replyText = ""
                                        replyingToCommentId = null
                                    }
                                },
                                onLikeReply = onLikeComment
                            )
                        }
                    }
                }

                // Sticky comment input
                Surface(
                    color = DarkSurface,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Add a comment…",
                                    color = Sage.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RetroPurple,
                                unfocusedBorderColor = RetroPurple.copy(alpha = 0.3f),
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RetroPurple,
                                contentColor = Color.White,
                                disabledContainerColor = RetroPurple.copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "POST",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Comment Item
@Composable
fun CommentItem(
    comment: com.calpoly.fleecedlogin.model.Comment,
    replies: List<com.calpoly.fleecedlogin.model.Comment>,
    isReplying: Boolean,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onSubmitReply: () -> Unit,
    onLikeReply: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${comment.username}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RetroPurple
                )
                Text(
                    text = getRelativeTimeString(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage
                )
            }

            // Content
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onLike)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (comment.isLiked) VoteRed else Sage
                    )
                    Text(
                        text = "${comment.likes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comment.isLiked) VoteRed else Sage
                    )
                }

                TextButton(
                    onClick = onReply,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isReplying) "CANCEL" else "REPLY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = if (isReplying) Sage else RetroPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Inline reply input
            if (isReplying) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = onReplyTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Reply to @${comment.username}…",
                                color = Sage.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RetroPurple,
                            unfocusedBorderColor = RetroPurple.copy(alpha = 0.3f),
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = onSubmitReply,
                        enabled = replyText.isNotBlank(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetroPurple,
                            contentColor = Color.White,
                            disabledContainerColor = RetroPurple.copy(alpha = 0.3f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("→", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Replies
            if (replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    replies.forEach { reply ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "@${reply.username}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RetroPurple
                                    )
                                    Text(
                                        text = getRelativeTimeString(reply.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Sage
                                    )
                                }
                                Text(
                                    text = reply.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onLikeReply(reply.id) }
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (reply.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (reply.isLiked) VoteRed else Sage
                                    )
                                    Text(
                                        text = "${reply.likes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (reply.isLiked) VoteRed else Sage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
