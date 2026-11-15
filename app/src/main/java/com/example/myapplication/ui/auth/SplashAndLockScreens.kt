package com.example.myapplication.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.75f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splash_scale",
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1250)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E9F8E),
                        Color(0xFF16A378),
                        Color(0xFF22B157),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale),
        ) {
            Surface(
                modifier = Modifier.size(116.dp),
                shape = RoundedCornerShape(30.dp),
                color = Color.White.copy(alpha = 0.16f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
fun PinLockScreen(
    expectedPin: String,
    onUnlocked: () -> Unit,
    biometricAvailable: Boolean = false,
    onBiometricClick: () -> Unit = {},
    onMigrateLegacyPin: ((String) -> Unit)? = null,
) {
    var entered by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var lockoutSecondsLeft by remember { mutableStateOf(0) }

    LaunchedEffect(lockoutSecondsLeft) {
        if (lockoutSecondsLeft > 0) {
            delay(1_000L)
            lockoutSecondsLeft -= 1
            if (lockoutSecondsLeft == 0) {
                isError = false
            }
        }
    }

    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) {
            onBiometricClick()
        }
    }

    fun onDigit(digit: String) {
        if (lockoutSecondsLeft > 0) return
        if (entered.length < 4) {
            entered += digit
            isError = false
            if (entered.length == 4) {
                if (com.example.myapplication.security.SecurityCryptoHelper.verifyPin(entered, expectedPin)) {
                    if (!com.example.myapplication.security.SecurityCryptoHelper.isHashedPin(expectedPin)) {
                        onMigrateLegacyPin?.invoke(entered)
                    }
                    failedAttempts = 0
                    onUnlocked()
                } else {
                    failedAttempts += 1
                    isError = true
                    entered = ""
                    if (failedAttempts >= 5) {
                        val multiplier = (failedAttempts / 5).coerceAtMost(4)
                        lockoutSecondsLeft = 30 * multiplier
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (lockoutSecondsLeft > 0) return
        if (entered.isNotEmpty()) {
            entered = entered.dropLast(1)
            isError = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(16.dp))

            // Top Icon & Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E9F8E), Color(0xFF22B157)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.pin_enter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                val subtitleText = when {
                    lockoutSecondsLeft > 0 -> "Köplab xato urinişlar! Xavfsizlik uçun $lockoutSecondsLeft soniya kuting..."
                    isError -> "${stringResource(R.string.pin_wrong_error)} (${5 - (failedAttempts % 5)} ta uriniş qoldi)"
                    else -> stringResource(R.string.pin_enter_subtitle)
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError || lockoutSecondsLeft > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                // 4 PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(4) { index ->
                        val filled = index < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError || lockoutSecondsLeft > 0 -> MaterialTheme.colorScheme.error
                                        filled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                        )
                    }
                }
            }

            // Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫"),
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                if (biometricAvailable) {
                                    Surface(
                                        onClick = onBiometricClick,
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_fingerprint),
                                                contentDescription = "Barmoq izi",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.size(72.dp))
                                }
                            } else {
                                PinKeyButton(
                                    label = key,
                                    onClick = {
                                        if (key == "⌫") onBackspace() else onDigit(key)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
