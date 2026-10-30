package com.campusswap.app.views.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.auth.EmailCodeResult
import com.campusswap.app.viewmodel.AuthViewModel
import com.campusswap.app.views.themes.AccentBlue
import com.campusswap.app.views.themes.ErrorRed

/** Shown right after registering, when the server has just emailed the first code. Skippable. */
@Composable
fun VerifyEmailScreen(email: String?, onDone: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    // Register already sent a code, so the resend countdown starts now.
    var codeSentAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val isLoading = authViewModel.isLoading

    fun verify() {
        codeError = if (code.length != EMAIL_CODE_LENGTH) "Enter the $EMAIL_CODE_LENGTH-digit code from the email" else null
        infoMessage = null
        if (codeError != null) return
        authViewModel.verifyEmail(code) { result ->
            when (result) {
                EmailCodeResult.Done -> onDone()
                EmailCodeResult.TooSoon -> codeError = "Wait a minute and try again."
                is EmailCodeResult.Rejected -> codeError = result.message
                EmailCodeResult.Offline -> codeError = OFFLINE_MESSAGE
            }
        }
    }

    fun resend() {
        codeError = null
        authViewModel.sendVerificationCode { result ->
            when (result) {
                EmailCodeResult.Done -> {
                    codeSentAt = System.currentTimeMillis()
                    code = ""
                    infoMessage = "We sent you a new code. The previous one no longer works."
                }
                EmailCodeResult.TooSoon -> infoMessage = "We just sent you a code. Wait a minute before asking for another one."
                is EmailCodeResult.Rejected -> infoMessage = result.message
                EmailCodeResult.Offline -> infoMessage = OFFLINE_MESSAGE
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
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(AccentBlue.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(38.dp))
            }
            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "We sent a $EMAIL_CODE_LENGTH-digit code to ${email ?: "your email"}. Check your inbox and spam folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )

            EmailCodeField(code, { code = it; codeError = null }, codeError)
            PrimaryButton("Verify email", isLoading, onClick = ::verify)
            ResendCodeButton(sentAt = codeSentAt, enabled = !isLoading, onResend = ::resend)
            TextButton(onClick = onDone, enabled = !isLoading) {
                Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (infoMessage != null) {
                Text(
                    infoMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (infoMessage == OFFLINE_MESSAGE) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
