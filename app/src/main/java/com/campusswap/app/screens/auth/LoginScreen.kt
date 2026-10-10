package com.campusswap.app.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.ErrorRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.data.auth.LoginResult
import com.campusswap.app.components.NoClipboard
import com.campusswap.app.components.typedOnly
import com.campusswap.app.domain.InputLimits
import com.campusswap.app.domain.InputValidation

@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String?) -> Unit,
    onCreateAccount: () -> Unit,
    sessionExpired: Boolean = false,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf(if (sessionExpired) "Your session expired. Log in again to continue." else null) }
    val scope = rememberCoroutineScope()
    val authRepository = (LocalContext.current.applicationContext as CampusSwapApplication).container.authRepository

    fun attemptLogin() {
        emailError = InputValidation.emailError(email)
        passwordError = if (password.length < InputLimits.PASSWORD_MIN) "Password must be at least ${InputLimits.PASSWORD_MIN} characters" else null
        if (emailError == null && passwordError == null) {
            isLoading = true
            scope.launch {
                val result = authRepository.login(email, password)
                isLoading = false
                when (result) {
                    is LoginResult.Success -> onLoginSuccess(result.session.userId)
                    LoginResult.InvalidCredentials -> passwordError = "Incorrect email or password"
                    LoginResult.Offline -> emailError = "Can't reach the server. Check your connection and try again."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(AccentBlue.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = "CampusSwap",
                    tint = AccentBlue,
                    modifier = Modifier.size(44.dp),
                )
            }
            Text(
                text = "CampusSwap",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "The marketplace for verified students",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = InputValidation.sanitizeEmail(it); emailError = null },
                label = { Text("Institutional email") },
                singleLine = true,
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (emailError != null) {
                Text(
                    emailError!!,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                NoClipboard {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = typedOnly(password, InputValidation.sanitizePassword(it)); passwordError = null },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (passwordError != null) {
                    Text(
                        passwordError!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { infoMessage = "Password recovery isn't wired up in this prototype." }) {
                    Text("Forgot password?", color = AccentBlue)
                }
            }

            Button(
                onClick = { attemptLogin() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Log in")
                }
            }

            OutlinedButton(
                onClick = {onLoginSuccess(null) },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
            ) {
                Text("Continue with demo access")
            }

            Row(modifier = Modifier.padding(top = 24.dp)) {
                Text("New to CampusSwap?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCreateAccount) {
                    Text("Create account", color = AccentBlue)
                }
            }

            if (infoMessage != null) {
                Text(
                    infoMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
