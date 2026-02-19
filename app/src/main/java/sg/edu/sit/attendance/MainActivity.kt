package sg.edu.sit.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import sg.edu.sit.attendance.camera.PhotoCaptureScreen
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.qr.QrCamerascreen

// ─────────────────────────────────────────────
//  Light & Clean Professional Palette
// ─────────────────────────────────────────────
private val BgPage        = Color(0xFFF4F6FA)
private val BgCard        = Color(0xFFFFFFFF)
private val BgHeader      = Color(0xFF1A3A6B)
private val BlueAccent    = Color(0xFF1A5DC8)
private val BlueSoft      = Color(0xFFE8F0FE)
private val BlueText      = Color(0xFF1A3A6B)
private val SuccessGreen  = Color(0xFF1E8A4A)
private val SuccessBg     = Color(0xFFE8F5EE)
private val SuccessBorder = Color(0xFFB7DFC8)
private val ErrorRed      = Color(0xFFCC2D2D)
private val ErrorBg       = Color(0xFFFDECEC)
private val ErrorBorder   = Color(0xFFF5BDBD)
private val TextPrimary   = Color(0xFF0F1F3D)
private val TextSecondary = Color(0xFF5A6D8A)
private val TextMuted     = Color(0xFF9DAFC5)
private val DividerColor  = Color(0xFFE5EAF2)
private val StepDoneBg    = Color(0xFFE8F5EE)
private val StepPendBg    = Color(0xFFF0F3F9)

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

    val sessions     by vm.sessions.collectAsState()
    var scannedQr        by remember { mutableStateOf<String?>(null) }
    var lastLocation     by remember { mutableStateOf<Location?>(null) }
    var photoUri         by remember { mutableStateOf<String?>(null) }
    var showQrScanner    by remember { mutableStateOf(false) }
    var showPhotoCapture by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        fused.lastLocation.addOnSuccessListener { loc -> lastLocation = loc }
    }

    if (showQrScanner) {
        QrCamerascreen(
            onQrScanned = { raw -> scannedQr = raw; showQrScanner = false },
            onBack      = { showQrScanner = false }
        )
        return
    }

    if (showPhotoCapture) {
        PhotoCaptureScreen(
            onPhotoCaptured = { uri -> photoUri = uri; showPhotoCapture = false },
            onBack          = { showPhotoCapture = false }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgPage)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ───────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .background(BgHeader)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "SIT Attendance",
                    color         = Color.White,
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Mark your presence for today's session",
                    color    = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(18.dp))

                // Progress chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressChip(label = "QR",       done = scannedQr != null)
                    ProgressChip(label = "Photo",    done = photoUri != null)
                    ProgressChip(label = "Location", done = lastLocation != null)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Session cards ─────────────────────────
        sessions.forEach { s ->
            SessionCard(
                session      = s,
                scannedQr    = scannedQr,
                lastLocation = lastLocation,
                photoUri     = photoUri,
                lastResult   = vm.lastResult,
                onScanQr     = { showQrScanner = true },
                onTakePhoto  = { showPhotoCapture = true },
                onCheckIn    = {
                    vm.submitCheckIn(
                        session   = s,
                        scannedQr = scannedQr!!,
                        location  = lastLocation,
                        photoUri  = photoUri
                    )
                }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────
//  Progress chip (header)
// ─────────────────────────────────────────────
@Composable
private fun ProgressChip(label: String, done: Boolean) {
    val bg   = if (done) SuccessGreen else Color.White.copy(alpha = 0.15f)
    val text = if (done) Color.White else Color.White.copy(alpha = 0.6f)
    val icon = if (done) "✓ " else "○ "

    Box(
        Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            "$icon$label",
            color      = text,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────
//  Session Card
// ─────────────────────────────────────────────
@Composable
private fun SessionCard(
    session      : SessionEntity,
    scannedQr    : String?,
    lastLocation : Location?,
    photoUri     : String?,
    lastResult   : String,
    onScanQr     : () -> Unit,
    onTakePhoto  : () -> Unit,
    onCheckIn    : () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            colors    = CardDefaults.cardColors(containerColor = BgCard),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                // ── Session title & status ────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            session.title,
                            color      = TextPrimary,
                            fontSize   = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Group · ${session.groupId}",
                            color    = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Active badge
                    Row(
                        Modifier
                            .background(BlueSoft, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot()
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Active",
                            color      = BlueAccent,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(18.dp))

                // ── Steps label ──────────────────────
                Text(
                    "CHECK-IN STEPS",
                    color         = TextMuted,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(12.dp))

                StepRow("1", "Scan QR Code", scannedQr != null,
                    scannedQr ?: "Not scanned yet")
                Spacer(Modifier.height(10.dp))
                StepRow("2", "Take Photo", photoUri != null,
                    if (photoUri != null) "Photo captured" else "Not taken yet")
                Spacer(Modifier.height(10.dp))
                StepRow("3", "Location", lastLocation != null,
                    if (lastLocation != null)
                        "${lastLocation.latitude.toString().take(8)}, ${lastLocation.longitude.toString().take(8)}"
                    else "Fetching location...")

                Spacer(Modifier.height(22.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(18.dp))

                // ── Action buttons ───────────────────
                Text(
                    "ACTIONS",
                    color         = TextMuted,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onScanQr,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (scannedQr != null) SuccessGreen else BlueAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (scannedQr != null) SuccessBorder else DividerColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (scannedQr != null) "✓ QR Done" else "Scan QR",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick  = onTakePhoto,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (photoUri != null) SuccessGreen else BlueAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (photoUri != null) SuccessBorder else DividerColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (photoUri != null) "✓ Photo" else "Take Photo",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Check In button
                Button(
                    onClick  = onCheckIn,
                    enabled  = scannedQr != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = BlueAccent,
                        disabledContainerColor = DividerColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Check In",
                        color      = if (scannedQr != null) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }

                // ── Result banner ────────────────────
                if (lastResult.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    val isPresent = lastResult.startsWith("PRESENT")
                    val bgColor   = if (isPresent) SuccessBg else ErrorBg
                    val borderCol = if (isPresent) SuccessBorder else ErrorBorder
                    val fgColor   = if (isPresent) SuccessGreen else ErrorRed
                    val icon      = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Warning

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = fgColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                if (isPresent) "Check-in Successful" else "Check-in Failed",
                                color      = fgColor,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                lastResult,
                                color    = fgColor.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Step row
// ─────────────────────────────────────────────
@Composable
private fun StepRow(stepNumber: String, label: String, isDone: Boolean, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .background(if (isDone) StepDoneBg else StepPendBg, RoundedCornerShape(15.dp))
                .border(1.dp, if (isDone) SuccessBorder else DividerColor, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isDone) "✓" else stepNumber,
                color      = if (isDone) SuccessGreen else TextMuted,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                label,
                color      = if (isDone) TextPrimary else TextSecondary,
                fontSize   = 14.sp,
                fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                detail,
                color    = if (isDone) SuccessGreen else TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Pulsing dot
// ─────────────────────────────────────────────
@Composable
private fun PulsingDot() {
    val alpha by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        Modifier
            .size(6.dp)
            .background(BlueAccent.copy(alpha = alpha), RoundedCornerShape(3.dp))
    )
}