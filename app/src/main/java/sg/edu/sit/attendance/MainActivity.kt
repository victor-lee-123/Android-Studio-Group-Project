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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
private val WarningOrange = Color(0xFFE65100)
private val WarningBg     = Color(0xFFFFF3E0)
private val ShimmerBase   = Color(0xFFE8ECF0)
private val ShimmerHighlight = Color(0xFFF5F7FA)

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

// ─────────────────────────────────────────────
//  Shimmer effect modifier
// ─────────────────────────────────────────────
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    background(
        Brush.linearGradient(
            colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
            start  = Offset(translateAnim - 200f, 0f),
            end    = Offset(translateAnim, 0f)
        )
    )
}

// ─────────────────────────────────────────────
//  Skeleton card shown while sessions load
// ─────────────────────────────────────────────
@Composable
private fun SkeletonCard() {
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

            // Title placeholder
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
                Box(Modifier.width(60.dp).height(26.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
            }

            Spacer(Modifier.height(16.dp))

            // Time box placeholder
            Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(16.dp))

            // Step placeholders
            repeat(3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).shimmerEffect())
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    }
                }
                if (it < 2) Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(16.dp))

            // Button placeholders
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).shimmerEffect())
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        }
    }
}

// ─────────────────────────────────────────────
//  Time formatter helpers
// ─────────────────────────────────────────────
private fun formatTime(ms: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ms))

private fun formatDate(ms: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))

private fun timeRemaining(endMs: Long): String {
    val diff = endMs - System.currentTimeMillis()
    if (diff <= 0) return "Closed"
    val mins = diff / 60000
    return if (mins < 60) "${mins}m remaining" else "${mins / 60}h ${mins % 60}m remaining"
}

@SuppressLint("MissingPermission")
@Composable
private fun BaseDemoScreen(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current

    // Track if DB has been seeded and sessions have loaded
    var isLoading by remember { mutableStateOf(true) }

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
        delay(800) // small delay so shimmer is visible
        isLoading = false
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

                    // ── Skeleton / Empty / Sessions ───────
                    when {
                        isLoading -> {
                            // Show 1 skeleton card while loading
                            SkeletonCard()
                        }
                        sessions.isEmpty() -> {
                            EmptyState()
                        }
                        else -> {
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
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Empty State
// ─────────────────────────────────────────────
@Composable
private fun EmptyState() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(80.dp).background(BlueSoft, RoundedCornerShape(40.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DateRange, null, tint = BlueAccent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("No Sessions Available", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "There are no active sessions right now.\nPlease check back later or contact your teacher.",
                color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
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
        animationSpec = tween(400), label = "chipColor"
    )
    Box(Modifier.background(animBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text("${if (done) "✓ " else "○ "}$label", color = if (done) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    onDevSimulate : () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val allReady  = scannedQr != null
    val isClosed  = System.currentTimeMillis() > session.endTimeMs
    val remaining = timeRemaining(session.endTimeMs)

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue  = 0f,
        targetValue   = if (allReady) 0.3f else 0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label         = "glowAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(400)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 }
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).shadow(2.dp, RoundedCornerShape(16.dp)),
            colors    = CardDefaults.cardColors(containerColor = BgCard),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text("Group · ${session.groupId}", color = TextSecondary, fontSize = 12.sp)
                    }
                    Row(
                        Modifier.background(if (isClosed) WarningBg else BlueSoft, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isClosed) { PulsingDot(); Spacer(Modifier.width(5.dp)) }
                        Text(if (isClosed) "Closed" else "Active", color = if (isClosed) WarningOrange else BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Session time info box
                Box(Modifier.fillMaxWidth().background(BgPage, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Column {
                        Text(formatDate(session.startTimeMs), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatTime(session.startTimeMs), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("  →  ", color = TextMuted, fontSize = 13.sp)
                                Text(formatTime(session.endTimeMs), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.background(if (isClosed) WarningBg else SuccessBg, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(remaining, color = if (isClosed) WarningOrange else SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(16.dp))

                Text("CHECK-IN STEPS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(12.dp))

                AnimatedStepRow(0, "1", "Scan QR Code", scannedQr != null,   scannedQr ?: "Not scanned yet")
                Spacer(Modifier.height(10.dp))
                AnimatedStepRow(1, "2", "Take Photo",   photoUri != null,    if (photoUri != null) "Photo captured" else "Not taken yet")
                Spacer(Modifier.height(10.dp))
                AnimatedStepRow(2, "3", "Location",     lastLocation != null,
                    when {
                        lastLocation == null     -> "Fetching location..."
                        session.fenceLat == null -> "Location verified ✓"
                        else                     -> "Within range ✓"
                    }
                )

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

                Box(Modifier.fillMaxWidth().shadow(if (allReady) (12 * glowAlpha + 2).dp else 0.dp, RoundedCornerShape(12.dp), ambientColor = BlueAccent.copy(alpha = glowAlpha), spotColor = BlueAccent.copy(alpha = glowAlpha))) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "checkInScale")
                    Button(
                        onClick           = onCheckIn,
                        enabled           = allReady && !isClosed,
                        modifier          = Modifier.fillMaxWidth().height(52.dp).scale(btnScale),
                        interactionSource = interactionSource,
                        colors            = ButtonDefaults.buttonColors(containerColor = BlueAccent, disabledContainerColor = DividerColor),
                        shape             = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isClosed) "Session Closed" else "Check In", color = if (allReady && !isClosed) Color.White else TextMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

    val circleColor  by animateColorAsState(if (isDone) SuccessGreen else TextMuted,     tween(500), label = "circleColor")
    val circleBg     by animateColorAsState(if (isDone) StepDoneBg else StepPendBg,      tween(500), label = "circleBg")
    val circleBorder by animateColorAsState(if (isDone) SuccessBorder else DividerColor, tween(500), label = "circleBorder")

    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(isDone) { if (isDone) { popped = false; delay(50); popped = true } }
    val circleScale by animateFloatAsState(if (popped) 1f else if (isDone) 1.15f else 1f, spring(Spring.DampingRatioLowBouncy), label = "circleScale")

    AnimatedVisibility(visible = appeared, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).scale(circleScale).background(circleBg, RoundedCornerShape(15.dp)).border(1.dp, circleBorder, RoundedCornerShape(15.dp)),
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
    val isPressed    by interactionSource.collectIsPressedAsState()
    val scale        by animateFloatAsState(if (isPressed) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "btnScale")
    val borderColor  by animateColorAsState(if (done) SuccessBorder else DividerColor, tween(400), label = "borderColor")
    val contentColor by animateColorAsState(if (done) SuccessGreen else BlueAccent,    tween(400), label = "contentColor")

    OutlinedButton(
        onClick = onClick, modifier = modifier.scale(scale), interactionSource = interactionSource,
        colors  = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        border  = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape   = RoundedCornerShape(10.dp)
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