package com.calpoly.fleecedlogin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calpoly.fleecedlogin.ui.screens.PostScreen
import com.calpoly.fleecedlogin.view.HomeScreen
import com.calpoly.fleecedlogin.view.LoginScreen
import com.calpoly.fleecedlogin.view.LeaderboardScreen
import com.calpoly.fleecedlogin.view.ProfileScreen
import com.calpoly.fleecedlogin.view.SignUpScreen
import com.calpoly.fleecedlogin.viewmodel.HomeViewModel
import com.calpoly.fleecedlogin.viewmodel.LeaderboardViewModel
import com.calpoly.fleecedlogin.viewmodel.LoginViewModel
import com.calpoly.fleecedlogin.viewmodel.PostViewModel
import com.calpoly.fleecedlogin.viewmodel.ProfileViewModel
import com.calpoly.fleecedlogin.ui.theme.FleecedTheme
import com.calpoly.fleecedlogin.viewmodel.SignUpViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FleecedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()
    val signUpViewModel: SignUpViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val postViewModel: PostViewModel = viewModel()
    val leaderboardViewModel: LeaderboardViewModel = viewModel()

    // Link homeViewModel with profileViewModel for syncing posts
    homeViewModel.setProfileViewModel(profileViewModel)
    profileViewModel.setHomeViewModel(homeViewModel)

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    // Pass user data to home screen
                    val user = (loginViewModel.uiState.value as? com.calpoly.fleecedlogin.viewmodel.LoginUiState.Success)?.user
                    user?.let {
                        homeViewModel.setUser(it)
                        profileViewModel.setUser(it)
                        postViewModel.setUser(it)
                        leaderboardViewModel.setUser(it)
                        postViewModel.setUserDisplayRanks(leaderboardViewModel.getDisplayRanks(it.id, it.points))
                    }

                    navController.navigate("home") {
                        // Clear login from back stack
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = signUpViewModel,
                onSignUpSuccess = {
                    // Pass user data to home screen
                    val user = (signUpViewModel.uiState.value as? com.calpoly.fleecedlogin.viewmodel.SignUpUiState.Success)?.user
                    user?.let {
                        homeViewModel.setUser(it)
                        profileViewModel.setUser(it)
                        postViewModel.setUser(it)
                        leaderboardViewModel.setUser(it)
                        postViewModel.setUserDisplayRanks(leaderboardViewModel.getDisplayRanks(it.id, it.points))
                    }

                    navController.navigate("home") {
                        // Clear signup and login from back stack
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            val user = homeViewModel.uiState.collectAsState().value.user
            val ranks = if (user != null) leaderboardViewModel.getDisplayRanks(user.id, user.points) else emptyList()
            HomeScreen(
                viewModel = homeViewModel,
                userDisplayRanks = ranks,
                onRefresh = {
                    homeViewModel.refreshPosts()
                    leaderboardViewModel.refresh()
                },
                onNavigateToAdd = {
                    navController.navigate("createPost")
                },
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAccount = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            val user = profileViewModel.uiState.collectAsState().value.user
            val ranks = if (user != null) leaderboardViewModel.getDisplayRanks(user.id, user.points) else emptyList()
            ProfileScreen(
                viewModel = profileViewModel,
                userDisplayRanks = ranks,
                onRefresh = {
                    profileViewModel.refreshPosts()
                    leaderboardViewModel.refresh()
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAdd = {
                    navController.navigate("createPost")
                },
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    // profileViewModel.logout() is already called by ProfileScreen
                    homeViewModel.logout()
                    loginViewModel.resetState()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("createPost") {
            PostScreen(
                postViewModel = postViewModel,
                homeViewModel = homeViewModel,
                profileViewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToLeaderboard = {
                    navController.navigate("leaderboard") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAccount = {
                    navController.navigate("profile")
                }
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                viewModel = leaderboardViewModel,
                onRefresh = {
                    leaderboardViewModel.refresh()
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAdd = {
                    navController.navigate("createPost")
                },
                onNavigateToAccount = {
                    navController.navigate("profile")
                }
            )
        }
    }
}

