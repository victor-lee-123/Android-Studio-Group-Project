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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
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
import coil.request.ImageRequest
import java.io.File

private val BgDark      = Color(0xFF0A0E1A)
private val AccentCyan  = Color(0xFF00E5FF)
private val AccentGreen = Color(0xFF00FF9D)
private val TextPrimary = Color(0xFFF0F4FF)
private val TextSecond  = Color(0xFF8A9BC4)
private val SuccessBg   = Color(0xFF0D2B1F)

@Composable
fun PhotoCaptureScreen(onPhotoCaptured: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(BgDark)) {
        if (hasCameraPermission) PhotoCaptureContent(onPhotoCaptured, onBack)
        else PermissionDeniedContent { launcher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun PhotoCaptureContent(onPhotoCaptured: (String) -> Unit, onBack: () -> Unit) {
    val ctx       = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var capturedUri     by remember { mutableStateOf<Uri?>(null) }
    var isCapturing     by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    val shutterInteraction = remember { MutableInteractionSource() }
    val shutterPressed by shutterInteraction.collectIsPressedAsState()
    val shutterScale by animateFloatAsState(
        targetValue   = if (shutterPressed || isCapturing) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "shutter"
    )

    val ringAlpha by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue  = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "ringAlpha"
    )

    var showBadge by remember { mutableStateOf(false) }
    val badgeScale by animateFloatAsState(
        targetValue   = if (showBadge) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
        label         = "badgeScale"
    )
    LaunchedEffect(capturedUri) { if (capturedUri != null) showBadge = true }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary) }
            Text("Take Attendance Photo", modifier = Modifier.align(Alignment.Center), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))
        Text("Take a clear photo for attendance verification", color = TextSecond, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth().height(380.dp).padding(horizontal = 24.dp).clip(RoundedCornerShape(20.dp))) {
            if (capturedUri == null) {
                AndroidView(
                    factory = { context ->
                        val previewView  = PreviewView(context)
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                            .build()
                        imageCaptureRef.value = imageCapture
                        ProcessCameraProvider.getInstance(context).also { future ->
                            future.addListener({
                                val provider = future.get()
                                val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                                } catch (e: Exception) { errorMsg = "Camera error: ${e.message}" }
                            }, ContextCompat.getMainExecutor(context))
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)), radius = 600f)))
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x88000000), Color.Transparent))).padding(12.dp)) {
                    Text("• LIVE", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // ── Fix: load from File directly so Coil can access internal storage ──
                val imageFile = remember(capturedUri) {
                    capturedUri?.path?.let { File(it) }
                }
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(imageFile ?: capturedUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Captured photo",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).scale(badgeScale)
                        .background(SuccessBg, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Saved", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (capturedUri == null) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(84.dp).background(AccentCyan.copy(alpha = ringAlpha * 0.15f), CircleShape))
                Box(Modifier.size(80.dp).border(3.dp, AccentCyan.copy(alpha = 0.5f), CircleShape))
                Button(
                    onClick = {
                        val ic = imageCaptureRef.value ?: return@Button
                        isCapturing = true; errorMsg = null
                        PhotoSaver.takePhoto(ctx, ic,
                            onSaved = { uri -> capturedUri = uri; isCapturing = false; onPhotoCaptured(uri.toString()) },
                            onError = { e -> errorMsg = "Failed: ${e.message}"; isCapturing = false }
                        )
                    },
                    modifier          = Modifier.size((68 * shutterScale).dp),
                    interactionSource = shutterInteraction,
                    shape             = CircleShape,
                    colors            = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    enabled           = !isCapturing
                ) {
                    if (isCapturing) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Tap to capture", color = TextSecond, fontSize = 13.sp)
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val retakeInteraction = remember { MutableInteractionSource() }
                val retakePressed by retakeInteraction.collectIsPressedAsState()
                val retakeScale by animateFloatAsState(if (retakePressed) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "retake")

                OutlinedButton(
                    onClick = { capturedUri = null; showBadge = false },
                    modifier = Modifier.weight(1f).scale(retakeScale),
                    interactionSource = retakeInteraction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retake")
                }

                val useInteraction = remember { MutableInteractionSource() }
                val usePressed by useInteraction.collectIsPressedAsState()
                val useScale by animateFloatAsState(if (usePressed) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "use")

                Button(
                    onClick = { onPhotoCaptured(capturedUri.toString()) },
                    modifier = Modifier.weight(1f).scale(useScale),
                    interactionSource = useInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Use Photo", color = Color.Black, fontWeight = FontWeight.SemiBold) }
            }
        }

        errorMsg?.let {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0D0D)), shape = RoundedCornerShape(10.dp)) {
                Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📸", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Camera Permission Required", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("We need camera access to take your attendance photo.", color = TextSecond, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = AccentCyan), shape = RoundedCornerShape(8.dp)) {
            Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}