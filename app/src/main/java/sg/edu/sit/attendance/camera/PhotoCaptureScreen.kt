package sg.edu.sit.attendance.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

// ─────────────────────────────────────────────
//  Color palette (matches QR screen)
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
fun PhotoCaptureScreen(
    onPhotoCaptured: (String) -> Unit,
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
            PhotoCaptureContent(
                onPhotoCaptured = onPhotoCaptured,
                onBack = onBack
            )
        } else {
            PermissionDeniedContent(
                onRequest = { launcher.launch(Manifest.permission.CAMERA) }
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Main capture content
// ─────────────────────────────────────────────
@Composable
private fun PhotoCaptureContent(
    onPhotoCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx       = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    // Hold reference to ImageCapture use case
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    // Shutter pulse animation
    val shutterScale by animateFloatAsState(
        targetValue  = if (isCapturing) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shutter"
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
                "Take Attendance Photo",
                modifier   = Modifier.align(Alignment.Center),
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Take a clear photo for attendance verification",
            color     = TextSecond,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Camera preview or captured photo ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            if (capturedUri == null) {
                // Live camera preview
                AndroidView(
                    factory = { context ->
                        val previewView    = PreviewView(context)
                        val future         = ProcessCameraProvider.getInstance(context)
                        val imageCapture   = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCaptureRef.value = imageCapture

                        future.addListener({
                            val provider = future.get()
                            val preview  = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycle,
                                    CameraSelector.DEFAULT_FRONT_CAMERA, // front cam for selfie attendance
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                errorMsg = "Camera error: ${e.message}"
                            }
                        }, ContextCompat.getMainExecutor(context))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle vignette overlay
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color(0x55000000)),
                                radius = 600f
                            )
                        )
                )

                // Top label
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x88000000), Color.Transparent)
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        "• LIVE",
                        color    = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            } else {
                // Show captured photo preview
                AsyncImage(
                    model             = capturedUri,
                    contentDescription = "Captured photo",
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier.fillMaxSize()
                )

                // Success badge overlay
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(SuccessBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint     = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Saved", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Buttons ──────────────────────────────
        if (capturedUri == null) {
            // Shutter button
            Box(contentAlignment = Alignment.Center) {
                // Outer ring
                Box(
                    Modifier
                        .size(80.dp)
                        .border(3.dp, AccentCyan.copy(alpha = 0.5f), CircleShape)
                )
                // Inner capture button
                Button(
                    onClick = {
                        val ic = imageCaptureRef.value ?: return@Button
                        isCapturing = true
                        errorMsg    = null
                        PhotoSaver.takePhoto(
                            context     = ctx,
                            imageCapture = ic,
                            onSaved     = { uri ->
                                capturedUri = uri
                                isCapturing = false
                                onPhotoCaptured(uri.toString())
                            },
                            onError = { e ->
                                errorMsg    = "Failed: ${e.message}"
                                isCapturing = false
                            }
                        )
                    },
                    modifier = Modifier.size((68 * shutterScale).dp),
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    enabled  = !isCapturing
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color    = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Tap to capture", color = TextSecond, fontSize = 13.sp)

        } else {
            // Retake / Use photo buttons
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retake
                OutlinedButton(
                    onClick = { capturedUri = null },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retake")
                }

                // Use this photo
                Button(
                    onClick = { onPhotoCaptured(capturedUri.toString()) },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text("Use Photo", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Error message
        errorMsg?.let {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0D0D)),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Text(
                    it,
                    color    = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
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
        Text("📸", fontSize = 48.sp)
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
            "We need camera access to take your attendance photo.",
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