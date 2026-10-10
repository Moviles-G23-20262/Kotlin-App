package com.campusswap.app.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.components.NoClipboard
import com.campusswap.app.components.typedOnly
import com.campusswap.app.data.auth.RegisterResult
import com.campusswap.app.domain.InputLimits
import com.campusswap.app.domain.InputValidation
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.ErrorRed
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(onRegistered: (userId: String) -> Unit, onBackToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var majorError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val authRepository = (LocalContext.current.applicationContext as CampusSwapApplication).container.authRepository

    fun attemptRegister() {
        nameError = InputValidation.nameError(fullName)
        majorError = InputValidation.majorError(major)
        emailError = InputValidation.emailError(email, requireInstitutional = true)
        passwordError = InputValidation.passwordError(password)
        confirmError = InputValidation.confirmPasswordError(password, confirmPassword)
        formError = null
        if (listOf(nameError, majorError, emailError, passwordError, confirmError).any { it != null }) return

        isLoading = true
        scope.launch {
            val result = authRepository.register(email, password, fullName, major)
            isLoading = false
            when (result) {
                is RegisterResult.Success -> onRegistered(result.session.userId)
                RegisterResult.EmailTaken -> emailError = "An account with this email already exists"
                is RegisterResult.Rejected -> formError = result.message
                RegisterResult.Offline -> formError = "Can't reach the server. Check your connection and try again."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NoClipboard {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBackToLogin, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to log in")
                    }
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(AccentBlue.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(38.dp))
                }
                Text(
                    text = "Create your account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = "Only students with an institutional email can join",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )

                FormField(
                    value = fullName,
                    onValueChange = { fullName = InputValidation.sanitizeName(it); nameError = null },
                    label = "Full name",
                    error = nameError,
                    counter = "${fullName.length}/${InputLimits.NAME_MAX}",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                FormField(
                    value = major,
                    onValueChange = { major = InputValidation.sanitizeName(it, InputLimits.MAJOR_MAX); majorError = null },
                    label = "Major",
                    error = majorError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                FormField(
                    value = email,
                    onValueChange = { email = InputValidation.sanitizeEmail(it); emailError = null },
                    label = "Institutional email",
                    error = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )

                val transformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                FormField(
                    value = password,
                    onValueChange = { password = typedOnly(password, InputValidation.sanitizePassword(it)); passwordError = null },
                    label = "Password",
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
                    label = "Confirm password",
                    error = confirmError,
                    visualTransformation = transformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )

                if (formError != null) {
                    Text(
                        formError!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }

                Button(
                    onClick = { attemptRegister() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Create account")
                    }
                }

                Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onBackToLogin, enabled = !isLoading) {
                        Text("Log in", color = AccentBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardOptions: KeyboardOptions,
    hint: String? = null,
    counter: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        supportingText = when {
            error != null -> ({ Text(error, color = ErrorRed) })
            hint != null -> ({ Text(hint) })
            counter != null -> ({ Text(counter, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) })
            else -> null
        },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}
