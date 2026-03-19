# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

Android project, JDK 17 required. Build from Android Studio or:
```
./gradlew assembleDebug
./gradlew test                    # unit tests
./gradlew connectedAndroidTest    # instrumented tests (requires emulator/device)
```

## Architecture

**MVVM with Jetpack Compose** — single-activity app (`MainActivity`) using Compose navigation with route strings: `"login"`, `"signup"`, `"home"`, `"profile"`, `"createPost"`, `"leaderboard"`.

### ViewModel lifecycle and wiring

All ViewModels are created in the top-level `AppNavigation()` composable in `MainActivity.kt` via `viewModel()` — they are **not** scoped to individual screens and survive navigation.

After login/signup, user data must be manually propagated to every ViewModel via `setUser()`. `LeaderboardViewModel.getDisplayRanks()` is called in `AppNavigation` and the result is passed down to screens as a parameter — screens do not call it themselves.

**HomeViewModel ↔ ProfileViewModel bi-directional sync**: Each holds a reference to the other set via `setProfileViewModel()`/`setHomeViewModel()`. Any mutation (vote, comment, reaction) calls the sibling ViewModel's `updatePost()` / `updateComments()` to keep both in sync. `HomeViewModel` has both `updatePost()` and `updatePostInList()` — they are equivalent; prefer `updatePostInList()` when calling from ProfileViewModel.

### Data flow for posts

1. On `setUser()`, each ViewModel calls `loadPostsFromDb()` which fetches all related tables in a single coroutine and builds `Post` objects locally.
2. `_postsList` / `_userPostsList` is a `mutableListOf<Post>` (the backing store). `uiState.posts` is derived from it and never directly mutated.
3. `HomeViewModel.loadPosts()` filters `_postsList` to the current Mon–Sun window and sorts by `feedSortScore()` = `score * 2.0 + createdAt_in_hours`. Call it after any mutation to re-apply the filter.
4. `addPost()` prepends locally for instant feedback; `refreshPosts()` re-fetches from DB.

### Optimistic UI pattern

All user actions (votes, reactions, comments) update local state immediately, then persist to Supabase in a `viewModelScope.launch` block. DB failures are silently caught with `catch (_: Exception) {}`. Comment likes are **local-only** and are never persisted to DB.

### Backend: Supabase

Singleton at `data/SupabaseClient.kt`. Uses supabase-kt v3 with `Auth` and `Postgrest`.

**The app calls things "posts" but the DB schema uses "polls"** throughout all table names and column names.

DB tables:
- `polls` — `poll_type` Postgres enum: `trade`, `sit_start`, `general`
- `poll_options` — `sort_order` 0=give/start, 1=receive/sit for trade/start-sit posts
- `poll_option_players` — M2M between options and players, `sort_order` preserved
- `votes` — one row per (user, poll); updating a vote mutates the existing row
- `reactions` — upvotes/downvotes on polls (`target_type: "poll"`, `reaction_type: "upvote"/"downvote"`)
- `comments` — threaded via `parent_comment_id`; `upvote_count` column exists but like toggling is local-only
- `users_profile` — `id` (matches Supabase auth uid), `username`, `total_points`
- `players` — NFL player database searched by name/position/team via `ilike`

**Auth flow**: Email/password with email confirmation. On signup, username goes into Supabase auth metadata. On first post-confirmation login, `users_profile` row is created using that metadata.

### Models

Two separate model layers — do not mix them:

- **UI models** (not serializable): `Post`, `Comment`, `VoteType`, `PostType`, `PollData`, `PollOption`, `TradeSide`, `Player`, `Team`, `User`
- **DB models** (`@Serializable`): `Db*` classes in `DbModels.kt`, plus `DbPlayer` in `Player.kt` and `UserProfile` in `User.kt`

For writes, there are separate insert/upsert classes (e.g., `DbPollInsert`, `DbVoteUpsert`, `DbCommentInsert`, `DbReactionUpsert`) that only carry the fields needed for insertion. Read models have default values on all fields.

### Rank system

`getRankForPoints()` in `Rank.kt`: Rookie (0), Practice Squad (150), Starter (300), Pro Bowler (450), All-Pro (600), Hall of Famer (750).

Special titles computed by `LeaderboardViewModel.getDisplayRanks()`: GOAT (#1 all-time), MVP (#1 seasonal). When a user holds a special title, only the special title(s) appear on posts — the normal rank is suppressed. `PostViewModel.getPostDisplayRanks()` implements this rule.

## Package layout

```
com.calpoly.fleecedlogin/
├── MainActivity.kt              # Navigation graph, ViewModel wiring, AppTheme
├── data/SupabaseClient.kt       # Supabase singleton
├── model/
│   ├── DbModels.kt              # @Serializable DB row classes (read + insert variants)
│   ├── Post.kt                  # Post, Comment, VoteType, PostType
│   ├── Poll.kt                  # PollData, PollOption, TradeSide, Vote
│   ├── Player.kt                # Player, DbPlayer, toPlayer()
│   ├── User.kt                  # User, UserProfile
│   ├── Rank.kt                  # Rank enum, getRankForPoints()
│   └── Team.kt                  # NFL team list
├── view/                        # Composable screens (one per route)
├── viewmodel/                   # ViewModels (one per screen)
└── util/TimeUtils.kt            # Relative time formatting
```

## Theme

Defined in `ui/theme/`:
- **`Theme.kt`** — color palette matching the web app. Key named constants: `RetroDark` (background), `DarkSurface`/`DarkSurfaceVariant` (surface layers), `RetroPurple` (primary), `RetroOrange` (tertiary), `RetroYellow` (gold/ranks), `VoteGreen`/`VoteRed`/`VoteBlue` (vote indicators), `Sage` (muted text). Legacy names (`SkyBlue`, `Navy`, `LightBlue`, `Gold`, `Teal`, etc.) are kept as aliases so existing screens compile.
- **`Type.kt`** — `TomorrowFontFamily` (Google Fonts, loaded via GMS provider) applied to all Typography slots. `PixelFontFamily` is an alias for backward compat.

## Tech stack

- Kotlin 2.1.0, AGP 8.7.3, compileSdk/targetSdk 35, minSdk 24
- Jetpack Compose with Material 3 (BOM 2024.12.01)
- supabase-kt 3.1.1 (auth-kt, postgrest-kt) + Ktor Android engine
- kotlinx-serialization-json 1.7.3, Navigation Compose 2.7.7

## Gotchas

- `CreatePostScreen.kt` is in `view/` but its package is `com.calpoly.fleecedlogin.ui.screens` and the composable is named `PostScreen`. The ViewModel is `PostViewModel` (in `CreatePostViewmodel.kt`).
- `PullToRefreshBox` requires explicit import `androidx.compose.material3.pulltorefresh.PullToRefreshBox` — the `material3.*` wildcard does NOT cover it.
- The `poll_type` DB column is a Postgres enum. Adding new post types requires altering the enum in Supabase before changing Kotlin code.
- `createdAt` on `Post` is a `Long` (epoch ms). DB timestamps are ISO-8601 strings parsed manually with `SimpleDateFormat` in each ViewModel's `loadPostsFromDb()`.
