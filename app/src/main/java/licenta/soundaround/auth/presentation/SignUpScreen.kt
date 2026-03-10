package licenta.soundaround.auth.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.data.AuthResponse
import androidx.compose.ui.Alignment

@Composable
fun SignUpScreen(
    authRepo: AuthRepository,
    onSignUpSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var lastFmUsername by rememberSaveable { mutableStateOf("") }
    var usernameError by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var passwordError by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                usernameError = false
            },
            label = { Text("Username *") },
            isError = usernameError,
            supportingText = { if (usernameError) Text("Username is required") }
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Brief description (optional)") }
        )
        OutlinedTextField(
            value = lastFmUsername,
            onValueChange = { lastFmUsername = it },
            label = { Text("Last.fm username (optional)") }
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = false
            },
            label = { Text("Email *") },
            isError = emailError,
            supportingText = { if (emailError) Text("Email is required") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
            },
            label = { Text("Password *") },
            isError = passwordError,
            supportingText = { if (passwordError) Text("Password is required") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password *") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            usernameError = username.isBlank()
            emailError = email.isBlank()
            passwordError = password.isBlank()

            if (!username.isBlank() && !email.isBlank() && !password.isBlank()) {
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        authRepo.signUp(
                            email,
                            password,
                            username,
                            bio,
                            lastFmUsername
                        ).collect { response ->
                            when (response) {
                                is AuthResponse.Success -> onSignUpSuccess()
                                is AuthResponse.Error -> {
                                    Log.d("SignUpScreen", "Registration failed: ${response.message}")
                                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }) { Text("Register") }
    }
}