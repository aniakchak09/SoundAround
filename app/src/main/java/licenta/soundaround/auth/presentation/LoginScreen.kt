package licenta.soundaround.auth.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.data.AuthResponse

@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showForgotDialog by rememberSaveable { mutableStateOf(false) }
    var resetStep by rememberSaveable { mutableStateOf(1) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetOtp by rememberSaveable { mutableStateOf("") }
    var resetNewPassword by rememberSaveable { mutableStateOf("") }
    var resetConfirmPassword by rememberSaveable { mutableStateOf("") }
    var resetMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isResetLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "SoundAround",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Share what you're listening to",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                scope.launch {
                    authRepo.signIn(email, password).collect { response ->
                        when (response) {
                            is AuthResponse.Success -> {
                                Log.d("LoginScreen", "Success!")
                                onLoginSuccess()
                            }
                            is AuthResponse.Error -> {
                                Log.d("LoginScreen", "Error: ${response.message}")
                                errorMessage = response.message
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Sign In", style = MaterialTheme.typography.labelLarge)
        }

        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Create one")
        }

        TextButton(onClick = {
            resetEmail = email
            resetOtp = ""
            resetNewPassword = ""
            resetConfirmPassword = ""
            resetMessage = null
            resetStep = 1
            showForgotDialog = true
        }) {
            Text("Forgot password?")
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Text(
                    when (resetStep) {
                        1 -> "Forgot Password"
                        2 -> "Enter Code"
                        else -> "New Password"
                    }
                )
            },
            text = {
                Column {
                    when (resetStep) {
                        1 -> {
                            Text(
                                "Enter your email and we'll send you a 6-digit code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it; resetMessage = null },
                                label = { Text("Email") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> {
                            Text(
                                "Enter the 6-digit code sent to $resetEmail.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetOtp,
                                onValueChange = { resetOtp = it; resetMessage = null },
                                label = { Text("Code") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = resetNewPassword,
                                onValueChange = { resetNewPassword = it; resetMessage = null },
                                label = { Text("New Password") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = resetConfirmPassword,
                                onValueChange = { resetConfirmPassword = it; resetMessage = null },
                                label = { Text("Confirm Password") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    resetMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.startsWith("Code sent")) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isResetLoading,
                    onClick = {
                        scope.launch {
                            isResetLoading = true
                            resetMessage = null
                            when (resetStep) {
                                1 -> {
                                    if (resetEmail.isBlank()) {
                                        resetMessage = "Please enter your email."
                                    } else {
                                        when (val r = authRepo.sendResetOtp(resetEmail)) {
                                            is AuthResponse.Success -> {
                                                resetMessage = "Code sent! Check your email."
                                                resetStep = 2
                                            }
                                            is AuthResponse.Error -> resetMessage = r.message
                                        }
                                    }
                                }
                                2 -> {
                                    if (resetOtp.isBlank()) {
                                        resetMessage = "Please enter the code."
                                    } else {
                                        when (val r = authRepo.verifyResetOtp(resetEmail, resetOtp)) {
                                            is AuthResponse.Success -> resetStep = 3
                                            is AuthResponse.Error -> resetMessage = r.message
                                        }
                                    }
                                }
                                else -> {
                                    if (resetNewPassword.isBlank()) {
                                        resetMessage = "Password cannot be empty."
                                    } else if (resetNewPassword != resetConfirmPassword) {
                                        resetMessage = "Passwords do not match."
                                    } else {
                                        when (val r = authRepo.updatePassword(resetNewPassword)) {
                                            is AuthResponse.Success -> showForgotDialog = false
                                            is AuthResponse.Error -> resetMessage = r.message
                                        }
                                    }
                                }
                            }
                            isResetLoading = false
                        }
                    }
                ) {
                    Text(when (resetStep) { 1 -> "Send Code"; 2 -> "Verify"; else -> "Update" })
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
            }
        )
    }
}
