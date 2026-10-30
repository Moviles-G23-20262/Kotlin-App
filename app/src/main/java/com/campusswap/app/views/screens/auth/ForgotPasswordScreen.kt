package com.campusswap.app.views.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.auth.EmailCodeResult
import com.campusswap.app.model.auth.ResetPasswordResult
import com.campusswap.app.model.domain.InputLimits
import com.campusswap.app.model.domain.InputValidation
import com.campusswap.app.viewmodel.AuthViewModel
import com.campusswap.app.views.components.NoClipboard
import com.campusswap.app.views.components.typedOnly
import com.campusswap.app.views.themes.AccentBlue
import com.campusswap.app.views.themes.ErrorRed

internal const val OFFLINE_MESSAGE = "Can't reach the server. Check your connection and try again."

/**
 * Two steps on one screen: the email to send a code to, then that code plus the new password.
 * A successful reset signs the user in.
 */
@Composable
fun ForgotPasswordScreen(initialEmail: String, onPasswordReset: (userId: String) -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var codeSentAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val isLoading = authViewModel.isLoading

    fun sendCode() {
        emailError = InputValidation.emailError(email)
        formError = null
        if (emailError != null) return
        authViewModel.forgotPassword(email) { result ->
            when (result) {
                EmailCodeResult.Done, EmailCodeResult.TooSoon -> codeSentAt = System.currentTimeMillis()
                is EmailCodeResult.Rejected -> formError = result.message
                EmailCodeResult.Offline -> formError = OFFLINE_MESSAGE
            }
        }
    }

    fun resetPassword() {
        codeError = if (code.length != EMAIL_CODE_LENGTH) "Enter the $EMAIL_CODE_LENGTH-digit code from the email" else null
        passwordError = InputValidation.passwordError(newPassword)
        confirmError = InputValidation.confirmPasswordError(newPassword, confirmPassword)
        formError = null
        if (listOf(codeError, passwordError, confirmError).any { it != null }) return
        authViewModel.resetPassword(email, code, newPassword) { result ->
            when (result) {
                is ResetPasswordResult.Success -> onPasswordReset(result.session.userId)
                // The password follows the same rules on both sides, so a refusal is about the code.
                is ResetPasswordResult.Rejected -> codeError = result.message
                ResetPasswordResult.Offline -> formError = OFFLINE_MESSAGE
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, enabled = !isLoading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to log in")
                }
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(AccentBlue.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.LockReset, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(38.dp))
            }
            Text(
                text = "Reset your password",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 14.dp),
            )

            val sentAt = codeSentAt
            if (sentAt == null) {
                Text(
                    text = "Enter the email you signed up with and we'll send you a $EMAIL_CODE_LENGTH-digit code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
                FormField(
                    value = email,
                    onValueChange = { email = InputValidation.sanitizeEmail(it); emailError = null },
                    label = "Institutional email",
                    error = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                )
                PrimaryButton("Send code", isLoading, onClick = ::sendCode)
            } else {
                Text(
                    text = "If $email has an account, we sent it a code. Check your inbox and spam folder, then choose a new password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(
                    onClick = { codeSentAt = null; code = ""; codeError = null; formError = null },
                    enabled = !isLoading,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text("Use a different email", color = AccentBlue)
                }

                EmailCodeField(code, { code = it; codeError = null }, codeError, imeAction = ImeAction.Next)

                // Passwords are typed, never pasted (as on Register); the code above can be pasted from the email.
                NoClipboard {
                    Column {
                        val transformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        FormField(
                            value = newPassword,
                            onValueChange = { newPassword = typedOnly(newPassword, InputValidation.sanitizePassword(it)); passwordError = null },
                            label = "New password",
                            error = passwordError,
                            hint = "At least ${InputLimits.PASSWORD_MIN} characters, with letters and numbers",
                            visualTransformation = transformation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    )
                                }
                            },
                        )
                        FormField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = typedOnly(confirmPassword, InputValidation.sanitizePassword(it)); confirmError = null },
                            label = "Confirm new password",
                            error = confirmError,
                            visualTransformation = transformation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        )
                    }
                }

                PrimaryButton("Reset password", isLoading, onClick = ::resetPassword)
                ResendCodeButton(sentAt = sentAt, enabled = !isLoading, onResend = ::sendCode)
            }

            if (formError != null) {
                Text(
                    formError!!,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }
}
