package com.calpoly.fleecedlogin.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calpoly.fleecedlogin.ui.theme.DarkSurface
import com.calpoly.fleecedlogin.ui.theme.DarkSurfaceVariant
import com.calpoly.fleecedlogin.ui.theme.RetroDark
import com.calpoly.fleecedlogin.ui.theme.RetroPurple
import com.calpoly.fleecedlogin.ui.theme.Sage
import com.calpoly.fleecedlogin.ui.theme.VoteGreen
import com.calpoly.fleecedlogin.viewmodel.SignUpUiState
import com.calpoly.fleecedlogin.viewmodel.SignUpViewModel

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = viewModel(),
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is SignUpUiState.Success) {
            onSignUpSuccess()
        }
    }

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

    // Confirm Email State
    if (uiState is SignUpUiState.ConfirmEmail) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RetroDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetroPurple, RoundedCornerShape(20.dp))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FLEECED",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 6.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, VoteGreen.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = VoteGreen
                        )
                        Text(
                            text = "CHECK YOUR EMAIL",
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                            fontWeight = FontWeight.Bold,
                            color = VoteGreen
                        )
                        Text(
                            text = "We sent a confirmation link to\n${(uiState as SignUpUiState.ConfirmEmail).email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Sage,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Verify your email, then log in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sage.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "GO TO LOGIN",
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    // Sign Up Form
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RetroPurple, RoundedCornerShape(20.dp))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FLEECED",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "JOIN THE LEAGUE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                color = Sage,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, RetroPurple.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is SignUpUiState.Loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChange(it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is SignUpUiState.Loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is SignUpUiState.Loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = uiState !is SignUpUiState.Loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.signUp()
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors
                    )

                    if (uiState is SignUpUiState.Error) {
                        Text(
                            text = (uiState as SignUpUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign Up Button
            Button(
                onClick = { viewModel.signUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState !is SignUpUiState.Loading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroPurple,
                    contentColor = Color.White,
                    disabledContainerColor = RetroPurple.copy(alpha = 0.4f)
                )
            ) {
                if (uiState is SignUpUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "SIGN UP",
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToLogin, enabled = uiState !is SignUpUiState.Loading) {
                Text(
                    "Already have an account? ",
                    color = Sage,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "LOGIN",
                    color = RetroPurple,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
