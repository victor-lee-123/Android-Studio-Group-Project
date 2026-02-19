package sg.edu.sit.attendance.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

private val SuccessGreen  = Color(0xFF1E8A4A)
private val SuccessBg     = Color(0xFFE8F5EE)
private val SuccessBorder = Color(0xFFB7DFC8)
private val ErrorRed      = Color(0xFFCC2D2D)
private val ErrorBg       = Color(0xFFFDECEC)
private val TextPrimary   = Color(0xFF0F1F3D)
private val TextSecondary = Color(0xFF5A6D8A)

@Composable
fun CheckInResultDialog(isPresent: Boolean, resultText: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { delay(3000); onDismiss() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
            if (isPresent) SuccessContent(resultText, onDismiss)
            else FailureContent(resultText, onDismiss)
        }
    }
}

@Composable
private fun SuccessContent(resultText: String, onDismiss: () -> Unit) {
    val ringScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "ringScale"
    )

    var iconVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); iconVisible = true }
    val iconScale by animateFloatAsState(
        targetValue   = if (iconVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label         = "iconScale"
    )

    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(450); textVisible = true }
    val textAlpha by animateFloatAsState(
        targetValue   = if (textVisible) 1f else 0f,
        animationSpec = tween(400),
        label         = "textAlpha"
    )

    val ripple1 by rememberInfiniteTransition(label = "r1").animateFloat(
        initialValue  = 0.7f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "ripple1"
    )
    val ripple1Alpha by rememberInfiniteTransition(label = "ra1").animateFloat(
        initialValue  = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "ripple1Alpha"
    )
    val ripple2 by rememberInfiniteTransition(label = "r2").animateFloat(
        initialValue  = 0.7f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400), RepeatMode.Restart), label = "ripple2"
    )
    val ripple2Alpha by rememberInfiniteTransition(label = "ra2").animateFloat(
        initialValue  = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400), RepeatMode.Restart), label = "ripple2Alpha"
    )

    Card(
        modifier  = Modifier.padding(32.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(120.dp).scale(ripple2).alpha(ripple2Alpha).background(SuccessGreen.copy(alpha = 0.15f), CircleShape))
                Box(Modifier.size(120.dp).scale(ripple1).alpha(ripple1Alpha).background(SuccessGreen.copy(alpha = 0.2f),  CircleShape))
                Box(Modifier.size(100.dp).scale(ringScale).background(SuccessBg, CircleShape))
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp).scale(iconScale))
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.alpha(textAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Check-in Successful!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Your attendance has been recorded.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.background(SuccessBg, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(resultText, color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss) {
                    Text("Done", color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun FailureContent(resultText: String, onDismiss: () -> Unit) {
    val shakeOffset by animateFloatAsState(
        targetValue   = 0f,
        animationSpec = keyframes {
            durationMillis = 500
            0f   at 0
            -18f at 60
            18f  at 120
            -14f at 180
            14f  at 240
            -8f  at 320
            8f   at 380
            0f   at 500
        },
        label = "shake"
    )

    var iconVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); iconVisible = true }
    val iconScale by animateFloatAsState(
        targetValue   = if (iconVisible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "iconScale"
    )

    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); textVisible = true }
    val textAlpha by animateFloatAsState(
        targetValue   = if (textVisible) 1f else 0f,
        animationSpec = tween(400),
        label         = "textAlpha"
    )

    Card(
        modifier  = Modifier.padding(32.dp).fillMaxWidth().offset(x = shakeOffset.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(100.dp).scale(iconScale).background(ErrorBg, CircleShape), contentAlignment = Alignment.Center) {
                Text("✕", color = ErrorRed, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.alpha(textAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Check-in Failed", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Please make sure all steps are completed.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.background(ErrorBg, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(resultText, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss) {
                    Text("Try Again", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}