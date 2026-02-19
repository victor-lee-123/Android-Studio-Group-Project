package sg.edu.sit.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.SessionEntity
import com.google.android.gms.location.LocationServices

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
                Surface(Modifier.fillMaxSize()) {
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
    val scope = rememberCoroutineScope()

    // For base demo: ensure at least 1 session exists (insert once)
    LaunchedEffect(Unit) {
        val dao = DbProvider.get(ctx).dao()
        val demo = SessionEntity(
            sessionId = "demo-session",
            groupId = "demo-group",
            title = "Demo Session",
            startTimeMs = System.currentTimeMillis() - 60_000,
            endTimeMs = System.currentTimeMillis() + 60 * 60_000,
            fenceLat = null, // set to real lat/lng to enforce fence
            fenceLng = null,
            fenceRadiusM = null,
            qrCodePayload = "ATTEND:demo-session",
            createdByUid = "teacher",
        )
        dao.upsertSession(demo)
    }

    val sessions by vm.sessions.collectAsState()

    var scannedQr by remember { mutableStateOf<String?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    // Grab last known location (simple base version)
    LaunchedEffect(Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        fused.lastLocation.addOnSuccessListener { loc ->
            lastLocation = loc
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Attendance Base (QR + Location + Camera + Room + Sync)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Text("Sessions:")
        sessions.forEach { s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall)
                    Text("QR payload: ${s.qrCodePayload}")
                    Spacer(Modifier.height(8.dp))

                    Row {
                        Button(
                            onClick = {
                                // For base demo, we "simulate" scanning by setting it directly.
                                // Your next step is to hook QrCameraPreview + analyzer (below).
                                scannedQr = s.qrCodePayload
                            }
                        ) { Text("Simulate QR Scan") }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                // For base demo, we "simulate" a photo URI.
                                photoUri = "file://demo.jpg"
                            }
                        ) { Text("Simulate Photo") }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        enabled = scannedQr != null,
                        onClick = {
                            vm.submitCheckIn(
                                session = s,
                                scannedQr = scannedQr!!,
                                location = lastLocation,
                                photoUri = photoUri
                            )
                        }
                    ) { Text("Check In") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Scanned QR: ${scannedQr ?: "-"}")
        Text("Location: ${lastLocation?.latitude?.toString()?.take(8) ?: "-"}, ${lastLocation?.longitude?.toString()?.take(8) ?: "-"}")
        Text("Photo URI: ${photoUri ?: "-"}")
        Spacer(Modifier.height(10.dp))
        Text("Result: ${vm.lastResult}")
    }
}
