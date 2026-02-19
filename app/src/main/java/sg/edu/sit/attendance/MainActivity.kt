package sg.edu.sit.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import sg.edu.sit.attendance.camera.PhotoCaptureScreen
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.qr.QrCamerascreen
import sg.edu.sit.attendance.ui.CheckInResultDialog

// ─────────────────────────────────────────────
//  Light & Clean Professional Palette
// ─────────────────────────────────────────────
private val BgPage        = Color(0xFFF4F6FA)
private val BgCard        = Color(0xFFFFFFFF)
private val BgHeader      = Color(0xFF1A3A6B)
private val BlueAccent    = Color(0xFF1A5DC8)
private val BlueSoft      = Color(0xFFE8F0FE)
private val SuccessGreen  = Color(0xFF1E8A4A)
private val SuccessBg     = Color(0xFFE8F5EE)
private val SuccessBorder = Color(0xFFB7DFC8)
private val TextPrimary   = Color(0xFF0F1F3D)
private val TextSecondary = Color(0xFF5A6D8A)
private val TextMuted     = Color(0xFF9DAFC5)
private val DividerColor  = Color(0xFFE5EAF2)
private val StepDoneBg    = Color(0xFFE8F5EE)
private val StepPendBg    = Color(0xFFF0F3F9)
private val DevPurple     = Color(0xFF7B1FA2)

class MainActivity : ComponentActivity() {

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = BgPage) {
                    BaseDemoScreen()
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BaseDemoScreen(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        val dao = DbProvider.get(ctx).dao()
        val demo = SessionEntity(
            sessionId     = "demo-session",
            groupId       = "demo-group",
            title         = "Demo Session",
            startTimeMs   = System.currentTimeMillis() - 60_000,
            endTimeMs     = System.currentTimeMillis() + 60 * 60_000,
            fenceLat      = null,
            fenceLng      = null,
            fenceRadiusM  = null,
            qrCodePayload = "ATTEND:demo-session",
            createdByUid  = "teacher",
        )
        dao.upsertSession(demo)
    }

    val sessions         by vm.sessions.collectAsState()
    var scannedQr        by remember { mutableStateOf<String?>(null) }
    var lastLocation     by remember { mutableStateOf<Location?>(null) }
    var photoUri         by remember { mutableStateOf<String?>(null) }
    var showQrScanner    by remember { mutableStateOf(false) }
    var showPhotoCapture by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultIsPresent  by remember { mutableStateOf(false) }
    var resultText       by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        fused.lastLocation.addOnSuccessListener { loc -> lastLocation = loc }
    }

    AnimatedContent(
        targetState = when {
            showQrScanner    -> "qr"
            showPhotoCapture -> "photo"
            else             -> "main"
        },
        transitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { if (targetState == "main") -it else it },
                animationSpec  = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { if (targetState == "main") it else -it },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(320)))
        },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            "qr" -> QrCamerascreen(
                onQrScanned = { raw -> scannedQr = raw; showQrScanner = false },
                onBack      = { showQrScanner = false }
            )
            "photo" -> PhotoCaptureScreen(
                onPhotoCaptured = { uri -> photoUri = uri; showPhotoCapture = false },
                onBack          = { showPhotoCapture = false }
            )
            else -> {
                if (showResultDialog) {
                    CheckInResultDialog(
                        isPresent  = resultIsPresent,
                        resultText = resultText,
                        onDismiss  = { showResultDialog = false }
                    )
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(BgPage)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Header ───────────────────────────
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(BgHeader)
                            .statusBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 24.dp)
                    ) {
                        Column {
                            Text("SIT Attendance", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                            Spacer(Modifier.height(3.dp))
                            Text("Mark your presence for today's session", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AnimatedProgressChip(label = "QR",       done = scannedQr != null)
                                AnimatedProgressChip(label = "Photo",    done = photoUri != null)
                                AnimatedProgressChip(label = "Location", done = lastLocation != null)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    sessions.forEach { s ->
                        SessionCard(
                            session      = s,
                            scannedQr    = scannedQr,
                            lastLocation = lastLocation,
                            photoUri     = photoUri,
                            onScanQr     = { showQrScanner = true },
                            onTakePhoto  = { showPhotoCapture = true },
                            onCheckIn    = {
                                vm.submitCheckIn(
                                    session   = s,
                                    scannedQr = scannedQr!!,
                                    location  = lastLocation,
                                    photoUri  = photoUri
                                )
                                resultIsPresent  = vm.lastResult.startsWith("PRESENT")
                                resultText       = vm.lastResult
                                showResultDialog = true
                            },
                            // ── DEV ONLY: simulate check-in without QR ──
                            onDevSimulate = {
                                vm.submitCheckIn(
                                    session   = s,
                                    scannedQr = s.qrCodePayload,
                                    location  = lastLocation,
                                    photoUri  = photoUri
                                )
                                resultIsPresent  = vm.lastResult.startsWith("PRESENT")
                                resultText       = vm.lastResult
                                showResultDialog = true
                            }
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Animated Progress Chip
// ─────────────────────────────────────────────
@Composable
private fun AnimatedProgressChip(label: String, done: Boolean) {
    val animBg by animateColorAsState(
        targetValue   = if (done) SuccessGreen else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(400),
        label         = "chipColor"
    )
    val text = if (done) Color.White else Color.White.copy(alpha = 0.6f)
    val icon = if (done) "✓ " else "○ "
    Box(Modifier.background(animBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text("$icon$label", color = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────
//  Session Card
// ─────────────────────────────────────────────
@Composable
private fun SessionCard(
    session       : SessionEntity,
    scannedQr     : String?,
    lastLocation  : Location?,
    photoUri      : String?,
    onScanQr      : () -> Unit,
    onTakePhoto   : () -> Unit,
    onCheckIn     : () -> Unit,
    onDevSimulate : () -> Unit          // ← DEV ONLY
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val allReady = scannedQr != null

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue  = 0f,
        targetValue   = if (allReady) 0.3f else 0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label         = "glowAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(400)) + slideInVertically(
            animationSpec  = tween(400, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 3 }
        )
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            colors    = CardDefaults.cardColors(containerColor = BgCard),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                // Title row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text("Group · ${session.groupId}", color = TextSecondary, fontSize = 12.sp)
                    }
                    Row(
                        Modifier.background(BlueSoft, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot()
                        Spacer(Modifier.width(5.dp))
                        Text("Active", color = BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(18.dp))

                Text("CHECK-IN STEPS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(12.dp))

                AnimatedStepRow(0, "1", "Scan QR Code", scannedQr != null,   scannedQr ?: "Not scanned yet")
                Spacer(Modifier.height(10.dp))
                AnimatedStepRow(1, "2", "Take Photo",   photoUri != null,    if (photoUri != null) "Photo captured" else "Not taken yet")
                Spacer(Modifier.height(10.dp))
                AnimatedStepRow(2, "3", "Location",     lastLocation != null,
                    if (lastLocation != null)
                        "${lastLocation.latitude.toString().take(8)}, ${lastLocation.longitude.toString().take(8)}"
                    else "Fetching location...")

                Spacer(Modifier.height(22.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(18.dp))

                Text("ACTIONS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScalePressButton(onScanQr,   Modifier.weight(1f), scannedQr != null, if (scannedQr != null) "✓ QR Done" else "Scan QR")
                    ScalePressButton(onTakePhoto, Modifier.weight(1f), photoUri != null,  if (photoUri != null) "✓ Photo" else "Take Photo")
                }

                Spacer(Modifier.height(12.dp))

                // Check In button with glow
                Box(
                    Modifier.fillMaxWidth().shadow(
                        elevation    = if (allReady) (12 * glowAlpha + 2).dp else 0.dp,
                        shape        = RoundedCornerShape(12.dp),
                        ambientColor = BlueAccent.copy(alpha = glowAlpha),
                        spotColor    = BlueAccent.copy(alpha = glowAlpha)
                    )
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue   = if (isPressed) 0.96f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label         = "checkInScale"
                    )
                    Button(
                        onClick           = onCheckIn,
                        enabled           = allReady,
                        modifier          = Modifier.fillMaxWidth().height(52.dp).scale(btnScale),
                        interactionSource = interactionSource,
                        colors            = ButtonDefaults.buttonColors(
                            containerColor         = BlueAccent,
                            disabledContainerColor = DividerColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check In", color = if (allReady) Color.White else TextMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── DEV ONLY: remove before submission ──
                Button(
                    onClick  = onDevSimulate,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = DevPurple),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text("DEV: Simulate Check In", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Animated Step Row
// ─────────────────────────────────────────────
@Composable
private fun AnimatedStepRow(index: Int, stepNumber: String, label: String, isDone: Boolean, detail: String) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 120L); appeared = true }

    val circleColor  by animateColorAsState(targetValue = if (isDone) SuccessGreen else TextMuted,     animationSpec = tween(500), label = "circleColor")
    val circleBg     by animateColorAsState(targetValue = if (isDone) StepDoneBg else StepPendBg,      animationSpec = tween(500), label = "circleBg")
    val circleBorder by animateColorAsState(targetValue = if (isDone) SuccessBorder else DividerColor, animationSpec = tween(500), label = "circleBorder")

    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(isDone) { if (isDone) { popped = false; delay(50); popped = true } }
    val circleScale by animateFloatAsState(
        targetValue   = if (popped) 1f else if (isDone) 1.15f else 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy),
        label         = "circleScale"
    )

    AnimatedVisibility(visible = appeared, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).scale(circleScale)
                    .background(circleBg, RoundedCornerShape(15.dp))
                    .border(1.dp, circleBorder, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isDone) "✓" else stepNumber, color = circleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label,  color = if (isDone) TextPrimary else TextSecondary, fontSize = 14.sp, fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal)
                Text(detail, color = if (isDone) SuccessGreen else TextMuted, fontSize = 11.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Scale-on-press outlined button
// ─────────────────────────────────────────────
@Composable
private fun ScalePressButton(onClick: () -> Unit, modifier: Modifier, done: Boolean, label: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale        by animateFloatAsState(if (isPressed) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "btnScale")
    val borderColor  by animateColorAsState(if (done) SuccessBorder else DividerColor, tween(400), label = "borderColor")
    val contentColor by animateColorAsState(if (done) SuccessGreen else BlueAccent,    tween(400), label = "contentColor")

    OutlinedButton(
        onClick           = onClick,
        modifier          = modifier.scale(scale),
        interactionSource = interactionSource,
        colors            = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        border            = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape             = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────
//  Pulsing dot
// ─────────────────────────────────────────────
@Composable
private fun PulsingDot() {
    val alpha by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "dotAlpha"
    )
    Box(Modifier.size(6.dp).background(BlueAccent.copy(alpha = alpha), RoundedCornerShape(3.dp)))
}