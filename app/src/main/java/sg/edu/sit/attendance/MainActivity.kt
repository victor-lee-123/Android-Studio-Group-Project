package sg.edu.sit.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import sg.edu.sit.attendance.camera.PhotoCaptureScreen
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.LeaveRequestEntity
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.location.LocationFence
import sg.edu.sit.attendance.qr.QrCamerascreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────
//  DigiPen Colour Palette
// ─────────────────────────────────────────────────────────────
val BgPage        = Color(0xFF0D0D0D)
val BgCard        = Color(0xFF1C1C1C)
val BgSurface2    = Color(0xFF242424)
val BgSurface3    = Color(0xFF2E2E2E)
val DigiRed       = Color(0xFFC8102E)
val DigiRedDark   = Color(0xFF9B0B22)
val DigiRedSoft   = Color(0x1AC8102E)
val DigiRedBorder = Color(0x40C8102E)
val SuccessGreen  = Color(0xFF22C55E)
val SuccessSoft   = Color(0x1A22C55E)
val SuccessBorder = Color(0x4022C55E)
val WarningAmber  = Color(0xFFF59E0B)
val WarningBg     = Color(0x1AF59E0B)
val WarningBorder = Color(0x40F59E0B)
val InfoBlue      = Color(0xFF3B82F6)
val InfoBlueSoft  = Color(0x1A3B82F6)
val InfoBlueBorder= Color(0x403B82F6)
val TextPrimary   = Color(0xFFF0F0F0)
val TextSecondary = Color(0xFF888888)
val TextMuted     = Color(0xFF555555)
val DividerColor  = Color(0xFF2A2A2A)
val ShimmerBase   = Color(0xFF1C1C1C)
val ShimmerHigh   = Color(0xFF2E2E2E)

// ─────────────────────────────────────────────────────────────
//  Navigation
// ─────────────────────────────────────────────────────────────
sealed class Screen {
    // Student flow
    object Login           : Screen()
    object Dashboard       : Screen()
    object CheckIn         : Screen()
    object QrScan          : Screen()
    object Photo           : Screen()
    object Success         : Screen()
    object LeaveForm       : Screen()
    object LeaveList       : Screen()
    // Professor flow
    object ProfDashboard   : Screen()
    object ClassAccess     : Screen()
    object AttendanceList  : Screen()
}

// ─────────────────────────────────────────────────────────────
//  MainActivity
// ─────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = BgPage) {
                    DigiCheckApp()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Root App
// ─────────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
@Composable
fun DigiCheckApp(vm: MainViewModel = viewModel()) {
    val ctx           = LocalContext.current
    val isLoggedIn    by vm.isLoggedIn.collectAsState()
    val sessions      by vm.sessions.collectAsState()
    val leaveList     by vm.leaveRequests.collectAsState()
    val checkInResult by vm.lastCheckInResult.collectAsState()

    // Seed demo sessions once
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!seeded) {
            seeded = true
            val dao = DbProvider.get(ctx).dao()
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            val s1Start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 10)
            val s1End = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 14)
            val s3Start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 16)
            val s3End = cal.timeInMillis

            dao.upsertSession(SessionEntity(
                sessionId = "s1", groupId = "g1", courseCode = "CSD2010", title = "Data Structures",
                room = "Room 3A-01", startTimeMs = s1Start, endTimeMs = s1End,
                fenceLat = null, fenceLng = null, fenceRadiusM = null,
                qrCodePayload = "ATTEND:s1", classPassword = "123456", createdByUid = "prof"
            ))
            dao.upsertSession(SessionEntity(
                sessionId = "s2", groupId = "g2", courseCode = "CSD3156", title = "Software Engineering",
                room = "Room 4B-02", startTimeMs = now - 30 * 60_000L, endTimeMs = now + 90 * 60_000L,
                fenceLat = 1.4123, fenceLng = 103.9087, fenceRadiusM = 300f,
                qrCodePayload = "ATTEND:s2", classPassword = "472819", createdByUid = "prof"
            ))
            dao.upsertSession(SessionEntity(
                sessionId = "s3", groupId = "g3", courseCode = "MAT2010", title = "Linear Algebra",
                room = "Room 2C-05", startTimeMs = s3Start, endTimeMs = s3End,
                fenceLat = null, fenceLng = null, fenceRadiusM = null,
                qrCodePayload = "ATTEND:s3", classPassword = "998877", createdByUid = "prof"
            ))
        }
    }

    // Check-in flow state
    var selectedSession  by remember { mutableStateOf<SessionEntity?>(null) }
    var scannedQr        by remember { mutableStateOf<String?>(null) }
    var enteredPin       by remember { mutableStateOf<String?>(null) }
    var lastLocation     by remember { mutableStateOf<Location?>(null) }
    var photoUri         by remember { mutableStateOf<String?>(null) }
    // Testing: spoof location as inside campus so location check always passes in dev
    var spoofLocation    by remember { mutableStateOf(false) }
    val effectiveLocation: Location? = if (spoofLocation) {
        Location("spoof").also { it.latitude = 1.4123; it.longitude = 103.9087 }
    } else lastLocation

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(ctx)
                fused.lastLocation.addOnSuccessListener { loc -> lastLocation = loc }
            } catch (_: Exception) {}
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    fun resetCheckIn() { scannedQr = null; enteredPin = null; photoUri = null; vm.clearCheckInResult() }
    fun goTo(s: Screen) { currentScreen = s }

    AnimatedContent(
        targetState  = currentScreen,
        transitionSpec = {
            val fwd = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(280)) togetherWith
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(200))
            val bck = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(280)) togetherWith
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
            when (targetState) {
                Screen.Login, Screen.Dashboard, Screen.LeaveList -> bck
                else -> fwd
            }
        },
        label = "nav"
    ) { screen ->
        when (screen) {
            Screen.Login -> LoginScreen { id, pwd, role ->
                vm.login(id, pwd, role) { ok, _ ->
                    if (ok) {
                        if (role.uppercase() == "PROFESSOR") goTo(Screen.ProfDashboard)
                        else goTo(Screen.Dashboard)
                    }
                }
            }
            Screen.Dashboard -> DashboardScreen(
                vm           = vm,
                sessions     = sessions,
                leaveList    = leaveList,
                onCheckIn    = { s -> selectedSession = s; resetCheckIn(); goTo(Screen.CheckIn) },
                onNewLeave   = { goTo(Screen.LeaveForm) },
                onViewLeave  = { goTo(Screen.LeaveList) },
                onLogout     = { vm.logout(); goTo(Screen.Login) }
            )
            Screen.CheckIn -> LocationCheckScreen(
                session       = selectedSession!!,
                lastLocation  = effectiveLocation,
                spoofLocation = spoofLocation,
                onToggleSpoof = { spoofLocation = !spoofLocation },
                onProceed     = { goTo(Screen.QrScan) },
                onBack        = { goTo(Screen.Dashboard) }
            )
            Screen.QrScan -> QrPinScreen(
                session    = selectedSession!!,
                onVerified = { qr, pin -> scannedQr = qr; enteredPin = pin; goTo(Screen.Photo) },
                onBack     = { goTo(Screen.CheckIn) }
            )
            Screen.Photo -> PhotoCaptureScreen(
                onPhotoCaptured = { uri ->
                    photoUri = uri
                    selectedSession?.let { s ->
                        vm.submitCheckIn(s, scannedQr, enteredPin, effectiveLocation, uri)
                    }
                    goTo(Screen.Success)
                },
                onBack = { goTo(Screen.QrScan) }
            )
            Screen.Success -> CheckInSuccessScreen(
                session       = selectedSession!!,
                checkInResult = checkInResult,
                photoUri      = photoUri,
                onDone        = { resetCheckIn(); goTo(Screen.Dashboard) }
            )
            Screen.LeaveForm -> LeaveFormScreen(
                vm          = vm,
                sessions    = sessions,
                onBack      = { goTo(Screen.Dashboard) },
                onSubmitted = { goTo(Screen.LeaveList) }
            )
            Screen.LeaveList -> LeaveListScreen(
                leaveList  = leaveList,
                onBack     = { goTo(Screen.Dashboard) },
                onNewLeave = { goTo(Screen.LeaveForm) }
            )
            // ── Professor screens ─────────────────────────────────
            Screen.ProfDashboard -> ProfDashboardScreen(
                vm        = vm,
                sessions  = sessions,
                onOpenClass = { s -> selectedSession = s; goTo(Screen.ClassAccess) },
                onViewAttendance = { s -> selectedSession = s; goTo(Screen.AttendanceList) },
                onLogout  = { vm.logout(); goTo(Screen.Login) }
            )
            Screen.ClassAccess -> ClassAccessScreen(
                session = selectedSession!!,
                onViewAttendance = { goTo(Screen.AttendanceList) },
                onBack  = { goTo(Screen.ProfDashboard) }
            )
            Screen.AttendanceList -> AttendanceListScreen(
                session = selectedSession!!,
                onBack  = { goTo(Screen.ClassAccess) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Helpers & common components
// ─────────────────────────────────────────────────────────────
fun Modifier.shimmerEffect(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "sh")
    val x by t.animateFloat(0f, 800f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "shx")
    background(Brush.linearGradient(listOf(ShimmerBase, ShimmerHigh, ShimmerBase), Offset(x - 200f, 0f), Offset(x, 0f)))
}

fun fmtTime(ms: Long): String     = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
fun fmtDate(ms: Long): String     = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(ms))
fun fmtDateLong(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
fun greeting(): String { val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); return when { h < 12 -> "morning"; h < 17 -> "afternoon"; else -> "evening" } }

fun sessionStatus(s: SessionEntity): String {
    val now = System.currentTimeMillis()
    return when { now > s.endTimeMs -> "Done"; now >= s.startTimeMs -> "Now"; else -> "Later" }
}

@Composable
fun PulsingDot(color: Color = DigiRed, size: Int = 6) {
    val a by rememberInfiniteTransition(label = "d").animateFloat(0.35f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "da")
    Box(Modifier.size(size.dp).background(color.copy(alpha = a), CircleShape))
}

@Composable
fun DigiTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun DigiButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
               enabled: Boolean = true, outlined: Boolean = false) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bs")
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = modifier.scale(scale).height(50.dp),
            enabled = enabled, interactionSource = src,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiRed),
            border = BorderStroke(1.dp, if (enabled) DigiRed else TextMuted),
            shape  = RoundedCornerShape(12.dp)
        ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
    } else {
        Button(onClick = onClick, modifier = modifier.scale(scale).height(50.dp),
            enabled = enabled, interactionSource = src,
            colors = ButtonDefaults.buttonColors(containerColor = DigiRed, disabledContainerColor = BgSurface3),
            shape  = RoundedCornerShape(12.dp)
        ) { Text(text, color = if (enabled) Color.White else TextMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp) }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
fun DigiTextField(value: String, onValueChange: (String) -> Unit, placeholder: String,
                  isPassword: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text,
                  singleLine: Boolean = true, minLines: Int = 1, modifier: Modifier = Modifier.fillMaxWidth(),
                  leadingIcon: @Composable (() -> Unit)? = null) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, singleLine = singleLine,
        minLines = minLines,
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DigiRed, unfocusedBorderColor = DividerColor,
            focusedContainerColor = BgSurface2, unfocusedContainerColor = BgSurface2,
            cursorColor = DigiRed, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !show) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) ({
            TextButton(onClick = { show = !show }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if (show) "Hide" else "Show", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }) else null
    )
}

// ─────────────────────────────────────────────────────────────
//  LOGIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(onLogin: (String, String, String) -> Unit) {
    var studentId by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf("Student") }
    var errorMsg  by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF1A0005), BgPage, BgPage)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {

            Spacer(Modifier.height(52.dp))

            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(DigiRed, RoundedCornerShape(1.dp)))
                Spacer(Modifier.width(8.dp))
                Text("DIGI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("PEN", color = DigiRed,    fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(" SG",  color = TextSecondary, fontSize = 13.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("Welcome\nBack.", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
            Text("Sign in to your account", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(36.dp))

            // Role toggle
            Row(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("Student", "Professor").forEach { r ->
                    Box(Modifier.weight(1f).background(if (role == r) DigiRed else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { role = r }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(r, color = if (role == r) Color.White else TextSecondary,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            FieldLabel("USERNAME")
            DigiTextField(studentId, { studentId = it }, if (role == "Student") "alex.t" else "rajan.a",
                leadingIcon = { Icon(Icons.Default.Person, null, tint = TextMuted) })

            Spacer(Modifier.height(16.dp))

            FieldLabel("PASSWORD")
            DigiTextField(password, { password = it }, "Enter your password",
                isPassword = true, keyboardType = KeyboardType.Password,
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMuted) })

            if (errorMsg.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg, color = DigiRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(28.dp))

            DigiButton("Sign In", modifier = Modifier.fillMaxWidth(), onClick = {
                if (studentId.isBlank() || password.isBlank()) { errorMsg = "Please fill in all fields"; return@DigiButton }
                errorMsg = ""
                onLogin(studentId, password, role)
            })

            Spacer(Modifier.height(16.dp))
            Text("Forgot Password?", color = DigiRed, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            // DEV HINT
            Box(Modifier.fillMaxWidth().background(BgSurface2, RoundedCornerShape(10.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Column {
                    Text("🧪  Test accounts", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Student:   alex.t  /  password123", color = TextSecondary, fontSize = 11.sp)
                    Text("Professor: rajan.a  /  password123", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  DASHBOARD SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    vm: MainViewModel,
    sessions: List<SessionEntity>,
    leaveList: List<LeaveRequestEntity>,
    onCheckIn: (SessionEntity) -> Unit,
    onNewLeave: () -> Unit,
    onViewLeave: () -> Unit,
    onLogout: () -> Unit
) {
    val userName  by vm.currentUserName.collectAsState()
    val studentId by vm.currentStudentId.collectAsState()
    val initials  = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val now       = System.currentTimeMillis()
    var tab by remember { mutableStateOf(0) }

    Scaffold(containerColor = BgPage,
        bottomBar = {
            NavigationBar(containerColor = BgCard, tonalElevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))) {
                listOf(Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.Default.DateRange, "Classes"),
                    Triple(2, Icons.Default.List, "Leave"),
                    Triple(3, Icons.Default.Person, "Profile")).forEach { (idx, icon, label) ->
                    NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                        icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DigiRed, selectedTextColor = DigiRed,
                            unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                            indicatorColor = DigiRedSoft))
                }
            }
        }
    ) { pad ->
        // Shared header
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0005), BgPage)))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Good ${greeting()},", color = TextSecondary, fontSize = 13.sp)
                        Text(userName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(DigiRed, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(studentId, color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Box(Modifier.size(42.dp)
                        .background(Brush.radialGradient(listOf(DigiRedDark, DigiRed)), CircleShape)
                        .clickable { onLogout() }, contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Tab content
            when (tab) {
                // ── HOME: today's classes (compact) + leave summary ──────────────
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text("📅  Today — ${fmtDate(now)}",
                        color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 10.dp))
                    sessions.forEach { s ->
                        val status = sessionStatus(s)
                        ClassRow(s, status == "Now", status == "Done") { onCheckIn(s) }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Leave Requests", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("+ New", color = DigiRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNewLeave() })
                    }
                    Spacer(Modifier.height(10.dp))
                    if (leaveList.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(14.dp)).padding(20.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No leave requests yet", color = TextMuted, fontSize = 13.sp)
                        }
                    } else {
                        leaveList.take(2).forEach { LeaveCardCompact(it); Spacer(Modifier.height(8.dp)) }
                        if (leaveList.size > 2)
                            Text("View all ${leaveList.size} →", color = DigiRed, fontSize = 13.sp,
                                modifier = Modifier.clickable { tab = 2 })
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── CLASSES: full schedule list ───────────────────────────────────
                1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text("YOUR SCHEDULE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                    sessions.forEach { s ->
                        val status = sessionStatus(s)
                        ScheduleCard(s, status == "Now", status == "Done") { onCheckIn(s) }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── LEAVE: full leave list + new button ───────────────────────────
                2 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MY LEAVE REQUESTS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Box(Modifier.background(DigiRed, RoundedCornerShape(8.dp)).clickable { onNewLeave() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (leaveList.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(14.dp)).padding(32.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No leave requests yet", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        leaveList.forEach { LeaveCardFull(it); Spacer(Modifier.height(10.dp)) }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── PROFILE ───────────────────────────────────────────────────────
                else -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(72.dp).background(Brush.radialGradient(listOf(DigiRedDark, DigiRed)), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(userName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(studentId, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Student", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(32.dp))
                    DigiButton("Sign Out", onClick = onLogout, modifier = Modifier.fillMaxWidth(0.6f), outlined = true)
                }
            }
        }
    }
}

@Composable
fun ClassRow(session: SessionEntity, isLive: Boolean, isDone: Boolean, onCheckIn: () -> Unit) {
    val a by animateFloatAsState(if (isDone) 0.4f else if (!isLive) 0.6f else 1f, tween(300), label = "ra")
    Box(Modifier.fillMaxWidth().alpha(a)
        .background(if (isLive) BgSurface3 else BgCard, RoundedCornerShape(12.dp))
        .border(1.dp, if (isLive) Color.White.copy(0.14f) else DividerColor, RoundedCornerShape(12.dp))
        .clickable(enabled = isLive) { onCheckIn() }) {

        if (isLive) {
            val sa by rememberInfiniteTransition(label = "s").animateFloat(0.5f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "sv")
            Box(Modifier.width(3.dp).fillMaxHeight()
                .background(DigiRed.copy(sa), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .align(Alignment.CenterStart))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = if (isLive) 8.dp else 0.dp)) {
                Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                    color = if (isLive) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(session.courseCode, color = if (isLive) Color.White else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(session.title, color = TextSecondary, fontSize = 12.sp)
                if (session.room.isNotBlank()) Text(session.room, color = TextMuted, fontSize = 11.sp)
            }
            when {
                isLive -> Column(horizontalAlignment = Alignment.End) {
                    Row(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(); Spacer(Modifier.width(4.dp))
                        Text("Now", color = DigiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to check in", color = DigiRed, fontSize = 10.sp)
                }
                isDone -> Box(Modifier.background(SuccessSoft, RoundedCornerShape(20.dp))
                    .border(1.dp, SuccessBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Done", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                else   -> Box(Modifier.background(InfoBlueSoft, RoundedCornerShape(20.dp))
                    .border(1.dp, InfoBlueBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Later", color = InfoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LeaveCardCompact(leave: LeaveRequestEntity) {
    val (sc, sb, sbd) = leaveStatusColors(leave.status)
    Row(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(14.dp))
        .border(1.dp, DividerColor, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(leave.affectedCourseCodes.replace(",", " · "), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${leave.leaveType} · ${fmtDateLong(leave.startDateMs)} – ${fmtDateLong(leave.endDateMs)}",
                color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.background(sb, RoundedCornerShape(20.dp)).border(1.dp, sbd, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(leave.status.lowercase().replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun leaveStatusColors(status: String): Triple<Color, Color, Color> = when (status) {
    "APPROVED" -> Triple(SuccessGreen, SuccessSoft, SuccessBorder)
    "REJECTED" -> Triple(DigiRed,      DigiRedSoft, DigiRedBorder)
    else        -> Triple(WarningAmber, WarningBg,  WarningBorder)
}

// ─────────────────────────────────────────────────────────────
//  SCHEDULE CARD (used in Classes tab — more detailed than ClassRow)
// ─────────────────────────────────────────────────────────────
@Composable
fun ScheduleCard(session: SessionEntity, isLive: Boolean, isDone: Boolean, onCheckIn: () -> Unit) {
    val alpha by animateFloatAsState(if (isDone) 0.5f else 1f, tween(300), label = "sca")
    Box(
        Modifier.fillMaxWidth().alpha(alpha)
            .background(BgCard, RoundedCornerShape(14.dp))
            .border(1.dp, if (isLive) DigiRed.copy(0.4f) else DividerColor, RoundedCornerShape(14.dp))
    ) {
        if (isLive) {
            val sa by rememberInfiniteTransition(label = "scl").animateFloat(0.4f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scv")
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(DigiRed.copy(sa), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .align(Alignment.CenterStart))
        }
        Column(Modifier.fillMaxWidth().padding(start = if (isLive) 18.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    // Course code + status badge row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(session.courseCode, color = if (isLive) Color.White else TextPrimary,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        when {
                            isLive -> Row(
                                Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                                    .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(size = 5); Spacer(Modifier.width(4.dp))
                                Text("LIVE", color = DigiRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            isDone -> Box(Modifier.background(SuccessSoft, RoundedCornerShape(20.dp))
                                .border(1.dp, SuccessBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("DONE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            else -> Box(Modifier.background(InfoBlueSoft, RoundedCornerShape(20.dp))
                                .border(1.dp, InfoBlueBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("UPCOMING", color = InfoBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(session.title, color = TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Time
                Column {
                    Text("TIME", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                        color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                // Room
                if (session.room.isNotBlank()) Column {
                    Text("ROOM", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(session.room, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (isLive) {
                Spacer(Modifier.height(12.dp))
                DigiButton("Check In Now", onClick = onCheckIn, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  LOCATION CHECK SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun LocationCheckScreen(
    session: SessionEntity,
    lastLocation: Location?,
    spoofLocation: Boolean = false,
    onToggleSpoof: () -> Unit = {},
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    val hasFence = session.fenceLat != null && session.fenceLng != null && session.fenceRadiusM != null
    val withinFence = remember(lastLocation, spoofLocation) {
        if (spoofLocation) true
        else if (!hasFence || lastLocation == null) true
        else LocationFence.withinFence(lastLocation, session.fenceLat!!, session.fenceLng!!, session.fenceRadiusM!!)
    }
    val locationKnown = lastLocation != null || spoofLocation

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar(title = "Check-In", onBack = onBack) {
            Box(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp)).border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Map area
        Box(Modifier.fillMaxWidth().height(210.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0005), BgSurface2))),
            contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val step = 40.dp.toPx(); val lc = Color.White.copy(0.03f)
                var x = 0f; while (x < size.width) { drawLine(lc, Offset(x, 0f), Offset(x, size.height)); x += step }
                var y = 0f; while (y < size.height) { drawLine(lc, Offset(0f, y), Offset(size.width, y)); y += step }
            }
            val pulse by rememberInfiniteTransition(label = "p").animateFloat(0.6f, 1.4f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pv")
            Box(Modifier.size((80 * pulse).dp).background(DigiRed.copy(0.1f), CircleShape))
            Box(Modifier.size(60.dp).background(DigiRed.copy(0.15f), CircleShape).border(2.dp, DigiRed.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).background(DigiRed, CircleShape))
            }
            Box(Modifier.align(Alignment.TopStart).padding(12.dp).background(Color.Black.copy(0.65f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("📍  SIT@Punggol", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Location status
            val locOk = locationKnown && withinFence
            Row(Modifier.fillMaxWidth()
                .background(when { locOk -> SuccessSoft; !locationKnown -> BgCard; else -> DigiRedSoft }, RoundedCornerShape(14.dp))
                .border(1.dp, when { locOk -> SuccessBorder; !locationKnown -> DividerColor; else -> DigiRedBorder }, RoundedCornerShape(14.dp))
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(if (locOk) SuccessSoft else DigiRedSoft, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(if (locOk) "✅" else if (!locationKnown) "⏳" else "❌", fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(when { !locationKnown -> "Fetching location…"; locOk -> "Within Campus Boundary"; else -> "Outside Campus" },
                        color = if (locOk) SuccessGreen else if (!locationKnown) TextPrimary else DigiRed,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (locationKnown) "SIT Punggol Campus · GPS verified" else "Please wait…",
                        color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Session card
            Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.courseCode, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(session.title, color = TextSecondary, fontSize = 12.sp)
                        Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (session.room.isNotBlank()) Box(Modifier.background(BgSurface2, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Text(session.room, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            if (!withinFence && hasFence && locationKnown) {
                Box(Modifier.fillMaxWidth().background(DigiRedSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, DigiRedBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("⚠️  You appear to be outside the campus boundary. Check-in may be rejected.",
                        color = DigiRed, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── DEV: Location spoof toggle ──────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(if (spoofLocation) Color(0x1A7B1FA2) else BgSurface2, RoundedCornerShape(12.dp))
                    .border(1.dp, if (spoofLocation) Color(0x407B1FA2) else DividerColor, RoundedCornerShape(12.dp))
                    .clickable { onToggleSpoof() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧪", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DEV: Spoof Campus Location", color = if (spoofLocation) Color(0xFFCE93D8) else TextSecondary,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (spoofLocation) "Active — treating you as inside campus" else "Off — using real GPS",
                            color = TextMuted, fontSize = 11.sp)
                    }
                    Box(Modifier.size(22.dp).background(
                        if (spoofLocation) Color(0xFF7B1FA2) else BgSurface3, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center) {
                        if (spoofLocation) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            DigiButton("Proceed to Verification", onClick = onProceed,
                enabled = locationKnown || !hasFence || spoofLocation, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  QR / PIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun QrPinScreen(session: SessionEntity, onVerified: (String?, String?) -> Unit, onBack: () -> Unit) {
    var showQr    by remember { mutableStateOf(false) }
    var scannedQr by remember { mutableStateOf<String?>(null) }
    var pin       by remember { mutableStateOf("") }
    var pinError  by remember { mutableStateOf("") }

    if (showQr) {
        QrCamerascreen(
            onQrScanned = { raw -> scannedQr = raw; showQr = false },
            onBack      = { showQr = false })
        return
    }

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("Verify Class", onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {

            // QR section
            FieldLabel("SCAN QR CODE")
            Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(18.dp))
                .border(2.dp, if (scannedQr != null) SuccessGreen else DigiRed, RoundedCornerShape(18.dp))
                .padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scannedQr != null) {
                        Box(Modifier.size(90.dp).background(SuccessSoft, RoundedCornerShape(14.dp)).border(2.dp, SuccessBorder, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text("✓", color = SuccessGreen, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("QR Code Scanned", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    } else {
                        // QR mock visual
                        Box(Modifier.size(130.dp).background(Color.White, RoundedCornerShape(10.dp)).padding(10.dp)) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val cell = size.width / 7f
                                listOf(0f to 0f, 5f to 0f, 0f to 5f).forEach { (col, row) ->
                                    drawRect(Color(0xFF111111), Offset(col * cell, row * cell), androidx.compose.ui.geometry.Size(cell * 2, cell * 2))
                                    drawRect(Color.White, Offset(col * cell + cell * 0.2f, row * cell + cell * 0.2f), androidx.compose.ui.geometry.Size(cell * 1.6f, cell * 1.6f))
                                    drawRect(Color(0xFF111111), Offset(col * cell + cell * 0.45f, row * cell + cell * 0.45f), androidx.compose.ui.geometry.Size(cell * 1.1f, cell * 1.1f))
                                }
                            }
                        }
                        Text(session.courseCode, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(); Spacer(Modifier.width(6.dp))
                            Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            DigiButton(if (scannedQr != null) "✓ Scanned — Rescan" else "Open QR Scanner",
                onClick = { showQr = true; scannedQr = null },
                modifier = Modifier.fillMaxWidth(), outlined = scannedQr != null)

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                Text("  OR  ", color = TextMuted, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = DividerColor)
            }

            // PIN section
            FieldLabel("6-DIGIT CLASS PASSWORD")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..5).forEach { i ->
                    val ch   = pin.getOrNull(i)?.toString() ?: ""
                    val focus = i == pin.length
                    Box(Modifier.weight(1f).aspectRatio(0.88f).background(BgSurface2, RoundedCornerShape(10.dp))
                        .border(1.5.dp, if (focus) DigiRed else DividerColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Text(if (ch.isNotBlank()) "•" else if (focus) "|" else "",
                            color = if (ch.isNotBlank()) DigiRed else TextMuted,
                            fontSize = if (ch.isNotBlank()) 22.sp else 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Invisible real input — numeric pad
            OutlinedTextField(value = pin, onValueChange = { v ->
                if (v.length <= 6 && v.all(Char::isDigit)) { pin = v; pinError = "" }
            },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                label = { Text("Enter PIN here", color = TextMuted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DigiRed, unfocusedBorderColor = DividerColor,
                    focusedContainerColor = BgSurface2, unfocusedContainerColor = BgSurface2,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = DigiRed
                ), shape = RoundedCornerShape(12.dp)
            )

            if (pinError.isNotBlank()) Text(pinError, color = DigiRed, fontSize = 13.sp)

            DigiButton("Confirm", modifier = Modifier.fillMaxWidth(),
                enabled = scannedQr != null || pin.length == 6,
                onClick = {
                    when {
                        scannedQr != null -> onVerified(scannedQr, null)
                        pin.length == 6   -> onVerified(null, pin)
                        else              -> pinError = "Enter 6-digit password or scan QR"
                    }
                })

            // ── DEV: simulate check-in outcomes ─────────────────────
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
            Text("🧪  DEV — Simulate check-in outcome", color = TextMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Force success: inject correct QR
                Box(Modifier.weight(1f).background(SuccessSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, SuccessBorder, RoundedCornerShape(10.dp))
                    .clickable { onVerified(session.qrCodePayload, null) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 18.sp)
                        Text("Force Pass", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Correct QR", color = TextMuted, fontSize = 10.sp)
                    }
                }
                // Force fail: inject wrong QR
                Box(Modifier.weight(1f).background(DigiRedSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, DigiRedBorder, RoundedCornerShape(10.dp))
                    .clickable { onVerified("WRONG_QR_FAIL", null) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 18.sp)
                        Text("Force Fail", color = DigiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Wrong QR", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  CHECK-IN SUCCESS SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun CheckInSuccessScreen(
    session: SessionEntity,
    checkInResult: sg.edu.sit.attendance.data.AttendanceEntity?,
    photoUri: String?,
    onDone: () -> Unit
) {
    val isOk    = checkInResult?.status in listOf("PRESENT", "LATE")
    val sColor  = if (isOk) SuccessGreen else DigiRed
    val sBg     = if (isOk) SuccessSoft  else DigiRedSoft
    val sBorder = if (isOk) SuccessBorder else DigiRedBorder

    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    val scale by animateFloatAsState(if (show) 1f else 0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium), label = "rs")
    val alpha by animateFloatAsState(if (show) 1f else 0f, tween(400), label = "ca")

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        Box(Modifier.size(120.dp).scale(scale), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(sBg, CircleShape).border(2.dp, sBorder, CircleShape))
            Box(Modifier.size(88.dp).background(sBg, CircleShape))
            Text(if (isOk) "✅" else "❌", fontSize = 50.sp)
        }
        Spacer(Modifier.height(24.dp))

        Column(Modifier.alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isOk) "Check-In Successful" else "Check-In Failed", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (isOk) "Your attendance has been recorded" else (checkInResult?.reason ?: "Please try again"),
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)

            Spacer(Modifier.height(28.dp))

            Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(18.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(18.dp)).padding(20.dp)) {
                mapOf("Class ID" to session.courseCode, "Class" to session.title,
                    "Time" to SimpleDateFormat("dd MMM · h:mm a", Locale.getDefault()).format(Date()),
                    "Status" to (checkInResult?.status ?: "—")).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = TextSecondary, fontSize = 13.sp)
                        Text(value, color = if (label == "Status") sColor else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (label != "Status") HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Photo", color = TextSecondary, fontSize = 13.sp)
                    Box(Modifier.size(46.dp).background(BgSurface2, RoundedCornerShape(10.dp))
                        .border(1.dp, DigiRedBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Text(if (photoUri != null) "🤳" else "—", fontSize = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            DigiButton("Back to Home", onDone, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────────────────────
//  LEAVE FORM
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveFormScreen(vm: MainViewModel, sessions: List<SessionEntity>, onBack: () -> Unit, onSubmitted: () -> Unit) {
    val leaveTypes = listOf("Medical Leave", "Compassionate Leave", "Personal Leave", "Other")
    var leaveType    by remember { mutableStateOf(leaveTypes[0]) }
    var typeExpanded by remember { mutableStateOf(false) }
    val today        = System.currentTimeMillis()
    var startMs      by remember { mutableStateOf(today) }
    var endMs        by remember { mutableStateOf(today + 86_400_000L) }
    var selCodes     by remember { mutableStateOf(setOf<String>()) }
    var remarks      by remember { mutableStateOf("") }

    val submitState by vm.leaveSubmitState.collectAsState()
    LaunchedEffect(submitState) {
        if (submitState is LeaveSubmitState.Success) { vm.clearLeaveSubmitState(); onSubmitted() }
    }

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("Leave Request", onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Leave type
            Column {
                FieldLabel("LEAVE TYPE")
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(value = leaveType, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DigiRed, unfocusedBorderColor = DividerColor,
                            focusedContainerColor = BgSurface2, unfocusedContainerColor = BgSurface2,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(BgCard)) {
                        leaveTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = TextPrimary) }, onClick = { leaveType = t; typeExpanded = false },
                                modifier = Modifier.background(if (t == leaveType) DigiRedSoft else Color.Transparent))
                        }
                    }
                }
            }

            // Duration — tap to advance by 1 day (replace with DatePickerDialog for production)
            Column {
                FieldLabel("DURATION")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("From" to startMs, "To" to endMs).forEachIndexed { i, (label, ms) ->
                        Box(Modifier.weight(1f).background(BgSurface2, RoundedCornerShape(12.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                            .clickable { if (i == 0) startMs += 86_400_000L else endMs += 86_400_000L }
                            .padding(14.dp)) {
                            Column {
                                Text(label, color = TextMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
                                Text(fmtDateLong(ms), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Classes affected
            Column {
                FieldLabel("CLASSES AFFECTED")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessions.forEach { s ->
                        val sel = s.courseCode in selCodes
                        Box(Modifier.background(if (sel) DigiRedSoft else BgSurface2, RoundedCornerShape(20.dp))
                            .border(1.dp, if (sel) DigiRedBorder else DividerColor, RoundedCornerShape(20.dp))
                            .clickable { selCodes = if (sel) selCodes - s.courseCode else selCodes + s.courseCode }
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(s.courseCode, color = if (sel) DigiRed else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Document upload placeholder
            Column {
                FieldLabel("SUPPORTING DOCUMENTS")
                Box(Modifier.fillMaxWidth()
                    .background(BgSurface2, RoundedCornerShape(12.dp))
                    .drawBehind {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                        val r = 12.dp.toPx()
                        drawRoundRect(DigiRedBorder, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
                    }
                    .clickable { /* TODO: file picker */ }.padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📎", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("MC, letter, or other proof", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("Tap to upload", color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Remarks
            Column {
                FieldLabel("REMARKS (OPTIONAL)")
                DigiTextField(remarks, { remarks = it }, "Add any additional notes…",
                    singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth())
            }

            if (submitState is LeaveSubmitState.Error)
                Text((submitState as LeaveSubmitState.Error).message, color = DigiRed, fontSize = 13.sp)

            DigiButton(
                text = if (submitState is LeaveSubmitState.Loading) "Submitting…" else "Submit Request",
                enabled = submitState !is LeaveSubmitState.Loading,
                onClick = { vm.submitLeaveRequest(leaveType, startMs, endMs, selCodes.toList(), remarks, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  LEAVE LIST
// ─────────────────────────────────────────────────────────────
@Composable
fun LeaveListScreen(leaveList: List<LeaveRequestEntity>, onBack: () -> Unit, onNewLeave: () -> Unit) {
    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("My Leave", onBack) {
            IconButton(onClick = onNewLeave) { Icon(Icons.Default.Add, null, tint = DigiRed) }
        }

        if (leaveList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📄", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                    Text("No Leave Requests", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Tap + to submit a new request", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                leaveList.forEach { LeaveCardFull(it) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LeaveCardFull(leave: LeaveRequestEntity) {
    val (sc, sb, sbd) = leaveStatusColors(leave.status)
    Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).border(1.dp, DividerColor, RoundedCornerShape(16.dp))) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(sc, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).align(Alignment.CenterStart))
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(leave.affectedCourseCodes.split(",").joinToString(" · "), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(leave.leaveType, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Box(Modifier.background(sb, RoundedCornerShape(20.dp)).border(1.dp, sbd, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(leave.status.lowercase().replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 13.sp); Spacer(Modifier.width(6.dp))
                Text("${fmtDateLong(leave.startDateMs)} – ${fmtDateLong(leave.endDateMs)}", color = TextSecondary, fontSize = 12.sp)
            }
            if (leave.documentUri != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("📎", fontSize = 13.sp); Spacer(Modifier.width(6.dp)); Text("Document attached", color = TextMuted, fontSize = 12.sp) }
            }
            if (leave.status == "REJECTED" && leave.rejectionReason != null) { Spacer(Modifier.height(6.dp)); Text("✕ ${leave.rejectionReason}", color = DigiRed, fontSize = 12.sp) }
            if (leave.status == "APPROVED" && leave.reviewedBy != null) { Spacer(Modifier.height(6.dp)); Text("✓ Approved by ${leave.reviewedBy}", color = SuccessGreen, fontSize = 12.sp) }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  PROFESSOR DASHBOARD  (P1)
// ─────────────────────────────────────────────────────────────
// Demo leave requests visible to professor
val profDemoLeave = listOf(
    StudentAttendanceRow("Raj Joshi",   "2200305", "LEAVE", "—", "Medical · 20–21 Feb"),
    StudentAttendanceRow("Maya Koh",    "2200147", "LEAVE", "—", "Personal · 22 Feb"),
)

@Composable
fun ProfDashboardScreen(
    vm: MainViewModel,
    sessions: List<SessionEntity>,
    onOpenClass: (SessionEntity) -> Unit,
    onViewAttendance: (SessionEntity) -> Unit,
    onLogout: () -> Unit
) {
    val userName  by vm.currentUserName.collectAsState()
    val profId    by vm.currentStudentId.collectAsState()
    val initials  = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val now       = System.currentTimeMillis()
    var tab by remember { mutableStateOf(0) }

    // Demo attendance numbers per session (replace with real DB query later)
    val demoStats = mapOf("s1" to Triple(18, 2, 1), "s2" to Triple(22, 3, 2), "s3" to Triple(30, 0, 0))

    Scaffold(containerColor = BgPage,
        bottomBar = {
            NavigationBar(containerColor = BgCard, tonalElevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))) {
                listOf(Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.Default.DateRange, "Classes"),
                    Triple(2, Icons.Default.List, "Leave"),
                    Triple(3, Icons.Default.Person, "Profile")).forEach { (idx, icon, label) ->
                    NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                        icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DigiRed, selectedTextColor = DigiRed,
                            unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                            indicatorColor = DigiRedSoft))
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // ── Shared header ────────────────────────────────────────
            Box(Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0005), BgPage)))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Prof. Dashboard", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                        Text(userName, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(DigiRed, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(profId, color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Box(Modifier.size(42.dp)
                        .background(Brush.radialGradient(listOf(DigiRedDark, DigiRed)), CircleShape)
                        .clickable { onLogout() }, contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // ── Tab content ──────────────────────────────────────────
            when (tab) {
                // HOME: today's classes with stats + pending leave summary
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text("📅  TODAY — ${fmtDate(now)}",
                        color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 10.dp))
                    sessions.forEach { session ->
                        val status = sessionStatus(session)
                        val stats  = demoStats[session.sessionId] ?: Triple(0, 0, 0)
                        ProfClassCard(session, status == "Now", status == "Done",
                            stats.first, stats.second, stats.third,
                            onOpen = { onOpenClass(session) },
                            onAttendance = { onViewAttendance(session) })
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("PENDING LEAVE REQUESTS", color = TextMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp))
                    profDemoLeave.forEach { leave ->
                        ProfLeaveRow(leave)
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // CLASSES: full class list (all sessions, expandable to open class access)
                1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text("ALL CLASSES", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                    sessions.forEach { session ->
                        val status = sessionStatus(session)
                        val stats  = demoStats[session.sessionId] ?: Triple(0, 0, 0)
                        ProfClassCard(session, status == "Now", status == "Done",
                            stats.first, stats.second, stats.third,
                            onOpen = { onOpenClass(session) },
                            onAttendance = { onViewAttendance(session) })
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // LEAVE: all student leave requests visible to professor
                2 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text("STUDENT LEAVE REQUESTS", color = TextMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                    if (profDemoLeave.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(14.dp)).padding(24.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No pending leave requests", color = TextMuted, fontSize = 13.sp)
                        }
                    } else {
                        profDemoLeave.forEach { leave ->
                            ProfLeaveRow(leave, showActions = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // PROFILE
                else -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(72.dp).background(Brush.radialGradient(listOf(DigiRedDark, DigiRed)), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(userName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(profId, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Professor", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(32.dp))
                    DigiButton("Sign Out", onClick = onLogout, modifier = Modifier.fillMaxWidth(0.6f), outlined = true)
                }
            }
        }
    }
}

@Composable
fun ProfLeaveRow(student: StudentAttendanceRow, showActions: Boolean = false) {
    val initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    Row(
        Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(InfoBlueSoft, CircleShape).border(1.dp, InfoBlueBorder, CircleShape),
            contentAlignment = Alignment.Center) {
            Text(initials, color = InfoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(student.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${student.studentId} · ${student.note}", color = TextSecondary, fontSize = 12.sp)
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.background(SuccessSoft, RoundedCornerShape(8.dp)).border(1.dp, SuccessBorder, RoundedCornerShape(8.dp))
                    .clickable { }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Approve", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.background(DigiRedSoft, RoundedCornerShape(8.dp)).border(1.dp, DigiRedBorder, RoundedCornerShape(8.dp))
                    .clickable { }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Reject", color = DigiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(Modifier.background(InfoBlueSoft, RoundedCornerShape(8.dp)).border(1.dp, InfoBlueBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("Pending", color = InfoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfClassCard(
    session: SessionEntity,
    isLive: Boolean,
    isDone: Boolean,
    present: Int,
    absent: Int,
    leave: Int,
    onOpen: () -> Unit,
    onAttendance: () -> Unit
) {
    Box(Modifier.fillMaxWidth()
        .background(BgCard, RoundedCornerShape(14.dp))
        .border(1.dp, if (isLive) Color.White.copy(0.12f) else DividerColor, RoundedCornerShape(14.dp))) {

        if (isLive) {
            val sa by rememberInfiniteTransition(label = "ps").animateFloat(0.5f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "sv")
            Box(Modifier.width(3.dp).fillMaxHeight()
                .background(DigiRed.copy(sa), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .align(Alignment.CenterStart))
        }

        Column(Modifier.fillMaxWidth().padding(start = if (isLive) 16.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
            // Top row: time, course, badge
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                        color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(session.courseCode, color = if (isLive) Color.White else TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${session.title} · ${session.room}", color = TextSecondary, fontSize = 12.sp)
                }
                when {
                    isLive -> Row(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(); Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = DigiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    isDone -> Box(Modifier.background(BgSurface3, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("DONE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    else -> Box(Modifier.background(InfoBlueSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, InfoBlueBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("LATER", color = InfoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            // Stats row (only show if live or done)
            if (isLive || isDone) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceStatBox("$present", "PRESENT", SuccessGreen, SuccessSoft, SuccessBorder, Modifier.weight(1f))
                    AttendanceStatBox("$absent",  "ABSENT",  DigiRed,     DigiRedSoft, DigiRedBorder, Modifier.weight(1f))
                    AttendanceStatBox("$leave",   "LEAVE",   WarningAmber, WarningBg,  WarningBorder, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AttendanceStatBox(value: String, label: String, color: Color, bg: Color, border: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(bg, RoundedCornerShape(10.dp)).border(1.dp, border, RoundedCornerShape(10.dp)).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = color.copy(0.7f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  CLASS ACCESS SCREEN  (P2)
// ─────────────────────────────────────────────────────────────
@Composable
fun ClassAccessScreen(
    session: SessionEntity,
    onViewAttendance: () -> Unit,
    onBack: () -> Unit
) {
    var showPassword    by remember { mutableStateOf(false) }
    var extensionMins   by remember { mutableStateOf(10) }
    var extended        by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("CLASS ACCESS", onBack) {
            Box(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // QR Code card
            Column {
                Text("QR CODE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp))
                Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp)).padding(20.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // QR visual
                        Box(Modifier.size(180.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val cell = size.width / 9f
                                // Corner markers
                                listOf(0f to 0f, 7f to 0f, 0f to 7f).forEach { (col, row) ->
                                    drawRect(Color(0xFF0D0D0D), Offset(col * cell, row * cell), androidx.compose.ui.geometry.Size(cell * 2f, cell * 2f))
                                    drawRect(Color.White, Offset(col * cell + cell * 0.22f, row * cell + cell * 0.22f), androidx.compose.ui.geometry.Size(cell * 1.56f, cell * 1.56f))
                                    drawRect(Color(0xFF0D0D0D), Offset(col * cell + cell * 0.5f, row * cell + cell * 0.5f), androidx.compose.ui.geometry.Size(cell * 1.0f, cell * 1.0f))
                                }
                                // Data dots
                                val positions = listOf(3 to 2, 5 to 1, 4 to 3, 6 to 4, 2 to 5, 5 to 6, 3 to 7, 7 to 3, 1 to 4)
                                positions.forEach { (c, r) ->
                                    drawRect(Color(0xFF0D0D0D), Offset(c * cell + cell * 0.1f, r * cell + cell * 0.1f),
                                        androidx.compose.ui.geometry.Size(cell * 0.8f, cell * 0.8f))
                                }
                            }
                        }
                        // Valid badge
                        Row(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(); Spacer(Modifier.width(6.dp))
                            Text("Valid for entire class · ${session.courseCode} ${fmtTime(session.startTimeMs)}–${fmtTime(session.endTimeMs)}",
                                color = DigiRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Session password row
            Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Session Password · fixed for class", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (showPassword) session.classPassword
                            else session.classPassword.take(2) + " " + "●".repeat(session.classPassword.length - 2),
                            color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp
                        )
                    }
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide" else "Reveal", color = DigiRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Extend check-in window
            Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Extend Check-In Window", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Currently: ${extensionMins} min window", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Minus button
                        Box(Modifier.size(36.dp).background(DigiRed, RoundedCornerShape(8.dp))
                            .clickable { if (extensionMins > 5) extensionMins -= 5 },
                            contentAlignment = Alignment.Center) {
                            Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${extensionMins}m", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp), textAlign = TextAlign.Center)
                        Box(Modifier.size(36.dp).background(DigiRed, RoundedCornerShape(8.dp))
                            .clickable { extensionMins += 5 },
                            contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Apply extension button
            DigiButton(
                text = if (extended) "✓ Extension Applied" else "APPLY EXTENSION",
                onClick = { extended = true },
                modifier = Modifier.fillMaxWidth()
            )

            // View attendance button
            DigiButton(
                text = "View Attendance List",
                onClick = onViewAttendance,
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  ATTENDANCE LIST SCREEN  (P3)
// ─────────────────────────────────────────────────────────────

// Demo student data (replace with real DB query when database is ready)
data class StudentAttendanceRow(
    val name: String,
    val studentId: String,
    val status: String,      // "PRESENT", "LATE", "ABSENT", "LEAVE"
    val checkInTime: String, // e.g. "10:03"
    val note: String = ""
)

@Composable
fun AttendanceListScreen(session: SessionEntity, onBack: () -> Unit) {
    // Demo roster — replace with real attendanceDao.observeSessionAttendance(session.sessionId)
    val roster = remember {
        listOf(
            StudentAttendanceRow("Alex Tan",   "2200123", "PRESENT", "10:03", "On time"),
            StudentAttendanceRow("Sarah Lim",  "2200089", "PRESENT", "10:08", "On time"),
            StudentAttendanceRow("Joel Ng",    "2200201", "ABSENT",  "—"),
            StudentAttendanceRow("Maya Koh",   "2200147", "LATE",    "10:19"),
            StudentAttendanceRow("Raj Joshi",  "2200305", "LEAVE",   "—")
        )
    }

    val present = roster.count { it.status == "PRESENT" }
    val absent  = roster.count { it.status == "ABSENT" }
    val leave   = roster.count { it.status == "LEAVE" }

    Column(Modifier.fillMaxSize().background(BgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("ATTENDANCE", onBack) {
            Box(Modifier.background(DigiRedSoft, RoundedCornerShape(20.dp))
                .border(1.dp, DigiRedBorder, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = DigiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Stats strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttendanceStatBox("$present", "PRESENT", SuccessGreen, SuccessSoft, SuccessBorder, Modifier.weight(1f))
            AttendanceStatBox("$absent",  "ABSENT",  DigiRed,     DigiRedSoft, DigiRedBorder, Modifier.weight(1f))
            AttendanceStatBox("$leave",   "LEAVE",   WarningAmber, WarningBg,  WarningBorder, Modifier.weight(1f))
        }

        // Student list
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.height(4.dp))
            roster.forEach { student ->
                StudentAttendanceCard(student)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StudentAttendanceCard(student: StudentAttendanceRow) {
    val (statusColor, statusBg, statusBorder, statusIcon) = when (student.status) {
        "PRESENT" -> Quadruple(SuccessGreen, SuccessSoft,  SuccessBorder, "✓")
        "LATE"    -> Quadruple(WarningAmber, WarningBg,   WarningBorder, "△")
        "ABSENT"  -> Quadruple(DigiRed,      DigiRedSoft,  DigiRedBorder, "✕")
        "LEAVE"   -> Quadruple(InfoBlue,     InfoBlueSoft, InfoBlueBorder,"📄")
        else      -> Quadruple(TextMuted,    BgSurface2,   DividerColor,  "?")
    }

    val initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    Box(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(14.dp))
        .border(1.dp, DividerColor, RoundedCornerShape(14.dp))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(Modifier.size(40.dp).background(
                Brush.radialGradient(listOf(
                    when (student.status) {
                        "PRESENT" -> Color(0xFF1A4A2E)
                        "LATE"    -> Color(0xFF4A3A00)
                        "ABSENT"  -> Color(0xFF4A0010)
                        else      -> Color(0xFF0A2A4A)
                    },
                    BgSurface3
                )), CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            // Name & ID
            Column(Modifier.weight(1f)) {
                Text(student.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(student.studentId, color = TextSecondary, fontSize = 12.sp)
            }

            // Status + time + action
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(statusIcon, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (student.status == "LEAVE") "Leave" else if (student.status == "ABSENT") "Absent" else if (student.status == "LATE") "Late" else student.checkInTime,
                            color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    if (student.note.isNotBlank()) Text(student.note, color = TextSecondary, fontSize = 10.sp)
                    if (student.status == "LATE" && student.checkInTime != "—") Text(student.checkInTime, color = TextSecondary, fontSize = 10.sp)
                }

                // Action button
                when (student.status) {
                    "ABSENT" -> Box(Modifier.background(DigiRed, RoundedCornerShape(8.dp))
                        .clickable { /* TODO: edit attendance */ }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("EDIT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    "LEAVE" -> Box(Modifier.background(InfoBlueSoft, RoundedCornerShape(8.dp))
                        .border(1.dp, InfoBlueBorder, RoundedCornerShape(8.dp))
                        .clickable { /* TODO: view leave doc */ }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("VIEW", color = InfoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    else -> Box(Modifier.background(BgSurface3, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("📞", fontSize = 14.sp) // placeholder contact action
                    }
                }
            }
        }
    }
}

// Helper data class for 4 values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)