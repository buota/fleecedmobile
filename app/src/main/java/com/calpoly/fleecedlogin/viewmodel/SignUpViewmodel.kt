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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    data class Success(val user: User) : SignUpUiState()
    data class ConfirmEmail(val email: String) : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword
    }

    fun signUp() {
        if (_email.value.isBlank() || _username.value.isBlank() ||
            _password.value.isBlank() || _confirmPassword.value.isBlank()) {
            _uiState.value = SignUpUiState.Error("Please fill in all fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()) {
            _uiState.value = SignUpUiState.Error("Please enter a valid email")
            return
        }

        if (_username.value.length < 3) {
            _uiState.value = SignUpUiState.Error("Username must be at least 3 characters")
            return
        }

        if (_password.value.length < 6) {
            _uiState.value = SignUpUiState.Error("Password must be at least 6 characters")
            return
        }

        if (_password.value != _confirmPassword.value) {
            _uiState.value = SignUpUiState.Error("Passwords do not match")
            return
        }

        _uiState.value = SignUpUiState.Loading

        viewModelScope.launch {
            try {
                val supabase = SupabaseClient.client

                // Check if email is already taken
                val emailExists = supabase.postgrest
                    .rpc("check_email_exists", buildJsonObject {
                        put("email_to_check", _email.value)
                    })
                    .decodeAs<Boolean>()

                if (emailExists) {
                    _uiState.value = SignUpUiState.Error("An account with this email already exists")
                    return@launch
                }

                // Check if username is already taken
                val existingUsers = supabase.postgrest
                    .from("users_profile")
                    .select {
                        filter { eq("username", _username.value) }
                    }
                    .decodeList<UserProfile>()

                if (existingUsers.isNotEmpty()) {
                    _uiState.value = SignUpUiState.Error("Username is already taken")
                    return@launch
                }

                supabase.auth.signUpWith(Email) {
                    email = _email.value
                    password = _password.value
                    data = buildJsonObject {
                        put("username", _username.value)
                    }
                }

                val currentUser = supabase.auth.currentUserOrNull()

                if (currentUser != null) {
                    // Email confirmation disabled — session available immediately
                    _uiState.value = SignUpUiState.Success(
                        User(
                            id = currentUser.id,
                            email = _email.value,
                            username = _username.value
                        )
                    )
                } else {
                    // Email confirmation required
                    _uiState.value = SignUpUiState.ConfirmEmail(_email.value)
                }
            } catch (e: Exception) {
                val msg = e.message?.lowercase() ?: ""
                _uiState.value = SignUpUiState.Error(
                    when {
                        msg.contains("already registered") || msg.contains("already exists") ->
                            "An account with this email already exists"
                        msg.contains("rate limit") || msg.contains("seconds") ->
                            "Too many attempts. Please wait a moment and try again."
                        msg.contains("network") || msg.contains("unable to resolve host") ->
                            "Network error. Please check your connection."
                        msg.contains("weak password") || msg.contains("password") ->
                            "Password is too weak. Please use a stronger password."
                        else -> "Sign up failed. Please try again."
                    }
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }
}
