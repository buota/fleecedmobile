package com.calpoly.fleecedlogin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calpoly.fleecedlogin.data.SupabaseClient
import com.calpoly.fleecedlogin.model.User
import com.calpoly.fleecedlogin.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun login() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            try {
                val supabase = SupabaseClient.client

                supabase.auth.signInWith(Email) {
                    email = _email.value
                    password = _password.value
                }

                val currentUser = supabase.auth.currentUserOrNull()
                    ?: throw Exception("No user in session")
                val userId = currentUser.id
                val userEmail = currentUser.email ?: _email.value

                // Try to fetch existing profile
                val existingProfiles = supabase.postgrest
                    .from("users_profile")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeList<UserProfile>()

                val profile = if (existingProfiles.isNotEmpty()) {
                    existingProfiles.first()
                } else {
                    // First login after email confirmation — create the profile
                    val username = currentUser.userMetadata
                        ?.jsonObject?.get("username")
                        ?.jsonPrimitive?.content
                        ?: userEmail.substringBefore("@")

                    val newProfile = UserProfile(
                        id = userId,
                        username = username,
                        totalPoints = 0
                    )

                    supabase.postgrest
                        .from("users_profile")
                        .insert(newProfile)

                    newProfile
                }

                val user = User(
                    id = userId,
                    email = userEmail,
                    username = profile.username,
                    points = profile.totalPoints
                )

                _uiState.value = LoginUiState.Success(user)
            } catch (e: Exception) {
                val msg = e.message?.lowercase() ?: ""
                _uiState.value = LoginUiState.Error(
                    when {
                        msg.contains("invalid login credentials") || msg.contains("invalid_credentials") ->
                            "Invalid email or password"
                        msg.contains("email not confirmed") ->
                            "Please confirm your email before logging in"
                        msg.contains("network") || msg.contains("unable to resolve host") ->
                            "Network error. Please check your connection."
                        else -> "Login failed. Please try again."
                    }
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
        _email.value = ""
        _password.value = ""
    }
}
