package com.campusswap.app.views.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusswap.app.views.themes.AccentBlue
import com.campusswap.app.views.themes.ErrorRed
import kotlinx.coroutines.delay

const val EMAIL_CODE_LENGTH = 6

/** The server lets a new code go out once a minute. */
private const val RESEND_AFTER_SECONDS = 60

/** Digits only, at most [EMAIL_CODE_LENGTH], shown large and spaced out like the email shows it. */
@Composable
internal fun EmailCodeField(value: String, onValueChange: (String) -> Unit, error: String?, imeAction: ImeAction = ImeAction.Done) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(EMAIL_CODE_LENGTH)) },
        label = { Text("$EMAIL_CODE_LENGTH-digit code") },
        singleLine = true,
        isError = error != null,
        textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 8.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        supportingText = error?.let { { Text(it, color = ErrorRed) } },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

/**
 * "Resend code", disabled while the last code is under a minute old. Changing [sentAt]
 * (e.g. to the time of the last send) restarts the countdown.
 */
@Composable
internal fun ResendCodeButton(sentAt: Long, enabled: Boolean, onResend: () -> Unit) {
    var secondsLeft by rememberSaveable(sentAt) {
        val elapsed = ((System.currentTimeMillis() - sentAt) / 1000).toInt()
        mutableIntStateOf((RESEND_AFTER_SECONDS - elapsed).coerceAtLeast(0))
    }
    LaunchedEffect(sentAt, secondsLeft) {
        if (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }
    TextButton(onClick = onResend, enabled = enabled && secondsLeft == 0) {
        Text(
            if (secondsLeft > 0) "Resend code in ${secondsLeft}s" else "Resend code",
            color = if (enabled && secondsLeft == 0) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.padding(top = 16.dp).fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}
