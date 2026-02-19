package sg.edu.sit.attendance.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

// ─────────────────────────────────────────────
//  Color palette
// ─────────────────────────────────────────────
private val BgDark      = Color(0xFF0A0E1A)
private val AccentCyan  = Color(0xFF00E5FF)
private val AccentGreen = Color(0xFF00FF9D)
private val TextPrimary = Color(0xFFF0F4FF)
private val TextSecond  = Color(0xFF8A9BC4)
private val CardBg      = Color(0xFF131929)
private val SuccessBg   = Color(0xFF0D2B1F)

// ─────────────────────────────────────────────
//  Entry point composable
// ─────────────────────────────────────────────
@Composable
fun QrCamerascreen(
    onQrScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (hasCameraPermission) {
            QrScannerContent(onQrScanned = onQrScanned, onBack = onBack)
        } else {
            PermissionDeniedContent(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
        }
    }
}

// ─────────────────────────────────────────────
//  Main scanner content
// ─────────────────────────────────────────────
@Composable
private fun QrScannerContent(
    onQrScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx       = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var scannedValue by remember { mutableStateOf<String?>(null) }
    var scanComplete by remember { mutableStateOf(false) }

    // Animated scanner line
    val scanLineY by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    // Corner pulse animation
    val cornerAlpha by rememberInfiniteTransition(label = "corner").animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cornerPulse"
    )

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Top bar ──────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                "Scan QR Code",
                modifier = Modifier.align(Alignment.Center),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Point your camera at the session QR code",
            color     = TextSecond,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Camera viewfinder ────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Live camera preview
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context)
                    val executor    = Executors.newSingleThreadExecutor()
                    val future      = ProcessCameraProvider.getInstance(context)

                    future.addListener({
                        val provider = future.get()
                        val preview  = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { ia ->
                                ia.setAnalyzer(executor, QrAnalyzer { raw ->
                                    if (!scanComplete) {
                                        scanComplete = true
                                        scannedValue = raw
                                        onQrScanned(raw)
                                    }
                                })
                            }

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycle,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (e: Exception) { /* ignore */ }

                    }, ContextCompat.getMainExecutor(context))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dim overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x88000000)),
                            radius = 400f
                        )
                    )
            )

            // Animated scan line
            if (!scanComplete) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopStart)
                        .offset(y = (280 * scanLineY).dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AccentCyan,
                                    AccentCyan,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Corner brackets
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val stroke     = Stroke(width = 4.dp.toPx())
                        val cornerSize = 40.dp.toPx()
                        val color      = AccentCyan.copy(alpha = cornerAlpha)
                        val r          = 12.dp.toPx()

                        // Top-left
                        drawRoundRect(color, Offset.Zero, Size(cornerSize, cornerSize), CornerRadius(r), stroke)
                        // Top-right
                        drawRoundRect(color, Offset(size.width - cornerSize, 0f), Size(cornerSize, cornerSize), CornerRadius(r), stroke)
                        // Bottom-left
                        drawRoundRect(color, Offset(0f, size.height - cornerSize), Size(cornerSize, cornerSize), CornerRadius(r), stroke)
                        // Bottom-right
                        drawRoundRect(color, Offset(size.width - cornerSize, size.height - cornerSize), Size(cornerSize, cornerSize), CornerRadius(r), stroke)
                    }
            )

            // Success overlay
            if (scanComplete) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xCC0A2E1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint     = AccentGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("QR Detected!", color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Result card ──────────────────────────
        if (scannedValue != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SuccessBg),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "✓  QR Code Scanned",
                        color      = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        scannedValue!!,
                        color    = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    scanComplete = false
                    scannedValue = null
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text("Scan Again")
            }

        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(AccentCyan, shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Searching for QR code...",
                        color    = TextSecond,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Permission denied state
// ─────────────────────────────────────────────
@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📷", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera Permission Required",
            color      = TextPrimary,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We need camera access to scan QR codes for attendance.",
            color     = TextSecond,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors  = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape   = RoundedCornerShape(8.dp)
        ) {
            Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}