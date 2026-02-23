// This is the main file for the DigiPen SG attendance app.
// It contains all the screens, components, theme colors, and navigation logic.
package sg.edu.sit.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

//  Theme System
// ThemeMode controls which color set the app uses.
// DARK uses the dark navy theme, LIGHT uses the light theme, SYSTEM follows the phone setting.
enum class ThemeMode { DARK, LIGHT, SYSTEM }

// AppColors holds all the color values used across the app.
// Instead of hardcoding colors everywhere, every composable reads from this data class.
// This makes it easy to switch between light and dark mode consistently.
data class AppColors(
    val bgPage: Color,
    val bgCard: Color,
    val bgSurface2: Color,
    val bgSurface3: Color,
    val digiRed: Color,
    val digiRedDark: Color,
    val digiRedSoft: Color,
    val digiRedBorder: Color,
    val successGreen: Color,
    val successSoft: Color,
    val successBorder: Color,
    val warningAmber: Color,
    val warningBg: Color,
    val warningBorder: Color,
    val infoBlue: Color,
    val infoBlueSoft: Color,
    val infoBlueBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val dividerColor: Color,
    val shimmerBase: Color,
    val shimmerHigh: Color,
    val headerGradientTop: Color,
    val navBarColor: Color,
    val navBarBorder: Color,
    val isDark: Boolean
)

// DarkColors is the color set used when the app is in dark mode.
// All colors use a navy-tinted dark palette so the whole app feels unified.
// Cards and surfaces get progressively lighter so they lift off the dark page background.
val DarkColors = AppColors(
    // Page stack true black base, navy-tinted layers so cards lift clearly
    bgPage          = Color(0xFF0A0E17),   // very dark navy-black (not pure black avoids harsh contrast)
    bgCard          = Color(0xFF131C2E),   // dark navy card clearly above page
    bgSurface2      = Color(0xFF1A2540),   // input field surface clearly above card
    bgSurface3      = Color(0xFF223050),   // pressed/hover state
    digiRed         = Color(0xFFC8102E),
    digiRedDark     = Color(0xFF9B0B22),
    digiRedSoft     = Color(0x1FC8102E),
    digiRedBorder   = Color(0x45C8102E),
    successGreen    = Color(0xFF22C55E),
    successSoft     = Color(0x1A22C55E),
    successBorder   = Color(0x4022C55E),
    warningAmber    = Color(0xFFF59E0B),
    warningBg       = Color(0x1AF59E0B),
    warningBorder   = Color(0x40F59E0B),
    infoBlue        = Color(0xFF3B82F6),
    infoBlueSoft    = Color(0x1A3B82F6),
    infoBlueBorder  = Color(0x403B82F6),
    textPrimary     = Color(0xFFF0F4FF),   // slightly cool white harmonises with navy
    textSecondary   = Color(0xFF8A9BB8),   // cool mid-grey readable, not warm
    textMuted       = Color(0xFF4A5A72),   // clearly dimmer than secondary
    dividerColor    = Color(0xFF1E2D45),   // visible navy separator
    shimmerBase     = Color(0xFF131C2E),
    shimmerHigh     = Color(0xFF1A2540),
    headerGradientTop = Color(0xFF0A1A36),
    navBarColor     = Color(0xFF0A1A36),
    navBarBorder    = Color(0xFF1A3055),
    isDark          = true
)

// LightColors is the color set used when the app is in light mode.
// All colors use a cool white and grey palette with the same navy header as dark mode.
// Cards are pure white so they stand out clearly against the light grey page.
val LightColors = AppColors(
    // Page stack cool white base, clear elevation steps
    bgPage          = Color(0xFFF0F2F5),   // cool light grey base clearly "light"
    bgCard          = Color(0xFFFFFFFF),   // pure white cards lift off page
    bgSurface2      = Color(0xFFEBEEF5),   // cool-tinted input fields distinct from white cards
    bgSurface3      = Color(0xFFDDE2EE),   // pressed/hover
    // Red accent same as dark mode for brand consistency
    digiRed         = Color(0xFFC8102E),
    digiRedDark     = Color(0xFF9B0B22),
    digiRedSoft     = Color(0x1AC8102E),
    digiRedBorder   = Color(0x40C8102E),
    // Status colours slightly adjusted for light backgrounds
    successGreen    = Color(0xFF16A34A),
    successSoft     = Color(0x1A16A34A),
    successBorder   = Color(0x4016A34A),
    warningAmber    = Color(0xFFD97706),
    warningBg       = Color(0x1AD97706),
    warningBorder   = Color(0x40D97706),
    infoBlue        = Color(0xFF2563EB),
    infoBlueSoft    = Color(0x1A2563EB),
    infoBlueBorder  = Color(0x402563EB),
    // Text dark on light backgrounds
    textPrimary     = Color(0xFF0F172A),   // deep navy-black consistent with navy brand
    textSecondary   = Color(0xFF5A6B84),   // cool mid-tone readable on white
    textMuted       = Color(0xFF94A3B8),   // clearly lighter than secondary
    dividerColor    = Color(0xFFD4D9E8),   // cool-tinted divider visible on white cards
    shimmerBase     = Color(0xFFE2E6F0),
    shimmerHigh     = Color(0xFFF0F2F8),
    // Deep navy header solid block, institutional feel
    headerGradientTop = Color(0xFF0A1A36),
    navBarColor     = Color(0xFF0A1A36),
    navBarBorder    = Color(0xFF1A3055),
    isDark          = false
)

// LocalColors is a Compose composition local that provides the current color set to any composable.
// Any composable can read the current theme colors by accessing LocalColors.current.
val LocalColors = compositionLocalOf { DarkColors }

// Convenience accessor use C.xxx anywhere inside a Composable
val C @Composable get() = LocalColors.current

// These top-level color variables are kept here for backward compatibility.
// They always point to the dark mode values and should not be used in new code.
// All new code should use C.colorName instead so the theme is applied correctly.
val BgPage        = DarkColors.bgPage
val BgCard        = DarkColors.bgCard
val BgSurface2    = DarkColors.bgSurface2
val BgSurface3    = DarkColors.bgSurface3
val DigiRed       = DarkColors.digiRed
val DigiRedDark   = DarkColors.digiRedDark
val DigiRedSoft   = DarkColors.digiRedSoft
val DigiRedBorder = DarkColors.digiRedBorder
val SuccessGreen  = DarkColors.successGreen
val SuccessSoft   = DarkColors.successSoft
val SuccessBorder = DarkColors.successBorder
val WarningAmber  = DarkColors.warningAmber
val WarningBg     = DarkColors.warningBg
val WarningBorder = DarkColors.warningBorder
val InfoBlue      = DarkColors.infoBlue
val InfoBlueSoft  = DarkColors.infoBlueSoft
val InfoBlueBorder= DarkColors.infoBlueBorder
val TextPrimary   = DarkColors.textPrimary
val TextSecondary = DarkColors.textSecondary
val TextMuted     = DarkColors.textMuted
val DividerColor  = DarkColors.dividerColor
val ShimmerBase   = DarkColors.shimmerBase
val ShimmerHigh   = DarkColors.shimmerHigh

//  Navigation
// Screen defines all the possible screens in the app.
// The app navigates between these by changing the currentScreen state variable.
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

//  MainActivity
// MainActivity is the entry point of the app.
// It requests camera and location permissions when the app first opens,
// then loads the main composable which handles everything else.
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
            DigiCheckApp()
        }
    }
}

//  Root App
@SuppressLint("MissingPermission")
@Composable
// DigiCheckApp is the root composable that sets up the theme and navigation.
// It reads the saved theme preference, applies the correct color set,
// seeds the demo class sessions into the database on first run,
// and renders the correct screen based on the current navigation state.
fun DigiCheckApp(vm: MainViewModel = viewModel()) {
    val ctx           = LocalContext.current
    val isLoggedIn    by vm.isLoggedIn.collectAsState()
    val sessions      by vm.sessions.collectAsState()
    val leaveList     by vm.leaveRequests.collectAsState()
    val checkInResult by vm.lastCheckInResult.collectAsState()

    // Theme
    // Read the saved theme preference from shared preferences
    // If no preference is saved yet, default to dark mode
    val prefs = remember { ctx.getSharedPreferences("digi_prefs", android.content.Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(
            when (prefs.getString("theme_mode", "DARK")) {
                "LIGHT"  -> ThemeMode.LIGHT
                "SYSTEM" -> ThemeMode.SYSTEM
                else     -> ThemeMode.DARK
            }
        )
    }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    // Pick the correct color set based on the current theme mode
    val appColors = when (themeMode) {
        ThemeMode.DARK   -> DarkColors
        ThemeMode.LIGHT  -> LightColors
        ThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
    }
    // Set the status bar icon color based on the current theme
    // Light theme gets dark icons and dark theme gets light icons so they are always readable
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as android.app.Activity).window
        SideEffect {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !appColors.isDark
        }
    }
    // saveTheme updates the theme state and saves the choice to shared preferences
    // so the user gets the same theme next time they open the app
    fun saveTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // seeded makes sure we only insert the demo sessions one time
    // LaunchedEffect runs this block once when the composable first appears
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

            // Session s1 is an 8am class that has already ended today
            dao.upsertSession(SessionEntity(
                sessionId = "s1", groupId = "g1", courseCode = "CSD2010", title = "Data Structures",
                room = "Room 3A-01", startTimeMs = s1Start, endTimeMs = s1End,
                fenceLat = null, fenceLng = null, fenceRadiusM = null,
                qrCodePayload = "ATTEND:s1", classPassword = "123456", createdByUid = "prof"
            ))
            // Session s2 is set to be live right now so the check-in flow can be tested
            // It also has a geofence set to SIT Punggol campus coordinates
            dao.upsertSession(SessionEntity(
                sessionId = "s2", groupId = "g2", courseCode = "CSD3156", title = "Software Engineering",
                room = "Room 4B-02", startTimeMs = now - 30 * 60_000L, endTimeMs = now + 90 * 60_000L,
                fenceLat = 1.4123, fenceLng = 103.9087, fenceRadiusM = 300f,
                qrCodePayload = "ATTEND:s2", classPassword = "472819", createdByUid = "prof"
            ))
            // Session s3 is an afternoon class that has not started yet
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
    // effectiveLocation uses the spoofed campus coordinates during dev testing
    // In a real build this would always use the actual device GPS location
    val effectiveLocation: Location? = if (spoofLocation) {
        Location("spoof").also { it.latitude = 1.4123; it.longitude = 103.9087 }
    } else lastLocation

    // Start fetching the device location as soon as the user logs in
    // We use the fused location provider which picks the best available location source
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(ctx)
                fused.lastLocation.addOnSuccessListener { loc -> lastLocation = loc }
            } catch (_: Exception) {}
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    // resetCheckIn clears all the check-in state when the user finishes or cancels a check-in
    // goTo changes the current screen which triggers the animated navigation transition
    fun resetCheckIn() { scannedQr = null; enteredPin = null; photoUri = null; vm.clearCheckInResult() }
    fun goTo(s: Screen) { currentScreen = s }

    CompositionLocalProvider(LocalColors provides appColors) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize(), color = appColors.bgPage) {
                // AnimatedContent handles the slide transition between screens
                // Going forward slides in from the right, going back slides in from the left
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
                            onLogout     = { vm.logout(); goTo(Screen.Login) },
                            themeMode    = themeMode,
                            onThemeChange = ::saveTheme
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
                            onDone        = {
                                // 23/02/2026 Change, edited so that failure screen doesn't show for a brief
                                // moment before it takes the user back to the home screen
                                goTo(Screen.Dashboard)

                                // Success screen doesn't "flip" to Failure during the slide-out
                                vm.viewModelScope.launch {
                                    delay(500) // wait for animation to finish
                                    resetCheckIn()
                                }
                            }
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
                        // Professor screens
                        Screen.ProfDashboard -> ProfDashboardScreen(
                            vm        = vm,
                            sessions  = sessions,
                            onOpenClass = { s -> selectedSession = s; goTo(Screen.ClassAccess) },
                            onViewAttendance = { s -> selectedSession = s; goTo(Screen.AttendanceList) },
                            onLogout  = { vm.logout(); goTo(Screen.Login) },
                            themeMode    = themeMode,
                            onThemeChange = ::saveTheme
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
            } } } // Surface / MaterialTheme / CompositionLocalProvider
}

//  Helpers & common components
// shimmerEffect is a reusable modifier that adds a moving shimmer animation to any element.
// It is used on loading skeleton placeholders to show that content is being fetched.
fun Modifier.shimmerEffect(): Modifier = composed {
    val colors = LocalColors.current
    val t = rememberInfiniteTransition(label = "sh")
    // Animate a horizontal position from 0 to 800 to create the sweeping light effect
    val x by t.animateFloat(0f, 800f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "shx")
    background(Brush.linearGradient(listOf(colors.shimmerBase, colors.shimmerHigh, colors.shimmerBase), Offset(x - 200f, 0f), Offset(x, 0f)))
}

// These are helper functions that format timestamps into readable text.
// fmtTime formats a timestamp as hour and minute (e.g. 8:00 AM).
// fmtDate formats a timestamp as short date with day name (e.g. Sun, 22 Feb).
// fmtDateLong formats a timestamp as full date (e.g. 22 Feb 2026).
// greeting returns the right greeting word based on the current hour.
fun fmtTime(ms: Long): String     = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
fun fmtDate(ms: Long): String     = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(ms))
fun fmtDateLong(ms: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
fun greeting(): String { val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); return when { h < 12 -> "morning"; h < 17 -> "afternoon"; else -> "evening" } }

// sessionStatus checks the current time against a session start and end time
// and returns a string telling us if the session is done, happening now, or coming later.
fun sessionStatus(s: SessionEntity): String {
    val now = System.currentTimeMillis()
    return when { now > s.endTimeMs -> "Done"; now >= s.startTimeMs -> "Now"; else -> "Later" }
}

@Composable
// PulsingDot draws a small circle that fades in and out repeatedly.
// It is used next to live session badges to draw attention to active classes.
fun PulsingDot(color: Color = C.digiRed, size: Int = 6) {
    val a by rememberInfiniteTransition(label = "d").animateFloat(0.35f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "da")
    Box(Modifier.size(size.dp).background(color.copy(alpha = a), CircleShape))
}

@Composable
// DigiTopBar is the shared top bar used on detail screens like Check-In and Leave Request.
// It shows a back button on the left, a title in the middle, and an optional trailing element on the right.
fun DigiTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val C = LocalColors.current
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = C.textPrimary) }
        Text(title, color = C.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
// DigiButton is the standard button used throughout the app.
// It has two styles: a solid red filled button and an outlined red border button.
// Both styles have a small press scale animation when tapped.
fun DigiButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
               enabled: Boolean = true, outlined: Boolean = false) {
    val C = LocalColors.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bs")
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = modifier.scale(scale).height(50.dp),
            enabled = enabled, interactionSource = src,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = C.digiRed),
            border = BorderStroke(1.dp, if (enabled) C.digiRed else C.textMuted),
            shape  = RoundedCornerShape(12.dp)
        ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
    } else {
        Button(onClick = onClick, modifier = modifier.scale(scale).height(50.dp),
            enabled = enabled, interactionSource = src,
            colors = ButtonDefaults.buttonColors(containerColor = C.digiRed, disabledContainerColor = C.bgSurface3),
            shape  = RoundedCornerShape(12.dp)
        ) { Text(text, color = if (enabled) Color.White else C.textMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp) }
    }
}

@Composable
// FieldLabel shows a small uppercase label above a form input field.
fun FieldLabel(text: String) {
    val C = LocalColors.current
    Text(text, color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
// DigiTextField is the standard text input field used across all forms in the app.
// It supports optional password masking with a show and hide toggle,
// and an optional leading icon on the left side of the field.
fun DigiTextField(value: String, onValueChange: (String) -> Unit, placeholder: String,
                  isPassword: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text,
                  singleLine: Boolean = true, minLines: Int = 1, modifier: Modifier = Modifier.fillMaxWidth(),
                  leadingIcon: @Composable (() -> Unit)? = null) {
    var show by remember { mutableStateOf(false) }
    val C = LocalColors.current
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, singleLine = singleLine,
        minLines = minLines,
        placeholder = { Text(placeholder, color = C.textMuted, fontSize = 14.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.digiRed, unfocusedBorderColor = C.dividerColor,
            focusedContainerColor = C.bgSurface2, unfocusedContainerColor = C.bgSurface2,
            cursorColor = C.digiRed, focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !show) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) ({
            TextButton(onClick = { show = !show }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if (show) "Hide" else "Show", color = C.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }) else null
    )
}

//  LOGIN SCREEN
@Composable
// LoginScreen is the first screen the user sees.
// It lets the user choose between Student and Professor roles,
// enter their username and password, and tap Sign In.
// The background uses the navy header color so both light and dark modes look consistent.
fun LoginScreen(onLogin: (String, String, String) -> Unit) {
    var studentId by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf("Student") }
    var errorMsg  by remember { mutableStateOf("") }

    val C = LocalColors.current
    Box(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(C.headerGradientTop, C.headerGradientTop, C.bgPage)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {

            Spacer(Modifier.height(52.dp))

            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(C.digiRed, RoundedCornerShape(1.dp)))
                Spacer(Modifier.width(8.dp))
                Text("DIGI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("PEN", color = C.digiRed,    fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(" SG",  color = Color.White.copy(0.6f), fontSize = 13.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("Welcome\nBack.", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
            Text("Sign in to your account", color = Color.White.copy(0.7f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(36.dp))

            // Role toggle
            Row(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(12.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("Student", "Professor").forEach { r ->
                    Box(Modifier.weight(1f).background(if (role == r) C.digiRed else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { role = r }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(r, color = if (role == r) Color.White else C.textSecondary,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            FieldLabel("USERNAME")
            DigiTextField(studentId, { studentId = it }, if (role == "Student") "alex.t" else "rajan.a",
                leadingIcon = { Icon(Icons.Default.Person, null, tint = C.textMuted) })

            Spacer(Modifier.height(16.dp))

            // PASSWORD label + "Forgot Password?" on the same row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel("PASSWORD")
                Text(
                    "Forgot Password?",
                    color = Color.White.copy(0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            DigiTextField(password, { password = it }, "Enter your password",
                isPassword = true, keyboardType = KeyboardType.Password,
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = C.textMuted) })

            if (errorMsg.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg, color = C.digiRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(28.dp))

            DigiButton("Sign In", modifier = Modifier.fillMaxWidth(), onClick = {
                if (studentId.isBlank() || password.isBlank()) { errorMsg = "Please fill in all fields"; return@DigiButton }
                errorMsg = ""
                onLogin(studentId, password, role)
            })

            Spacer(Modifier.height(16.dp))
            // DEV HINT
            Box(Modifier.fillMaxWidth().background(C.bgSurface2, RoundedCornerShape(10.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Column {
                    Text("🧪  Test accounts", color = C.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Student:   alex.t  /  password123", color = C.textSecondary, fontSize = 11.sp)
                    Text("Professor: rajan.a  /  password123", color = C.textSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

//  DASHBOARD SCREEN
@Composable
// DashboardScreen is the main screen for students after they log in.
// It has four tabs at the bottom: Home, Classes, Leave, and Profile.
// The Home tab shows today's classes and recent leave requests.
// The Classes tab shows the full schedule.
// The Leave tab shows all leave requests.
// The Profile tab shows account info and theme settings.
fun DashboardScreen(
    vm: MainViewModel,
    sessions: List<SessionEntity>,
    leaveList: List<LeaveRequestEntity>,
    onCheckIn: (SessionEntity) -> Unit,
    onNewLeave: () -> Unit,
    onViewLeave: () -> Unit,
    onLogout: () -> Unit,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    val C = LocalColors.current
    val userName  by vm.currentUserName.collectAsState()
    val studentId by vm.currentStudentId.collectAsState()
    val role      by vm.currentRole.collectAsState()
    val initials  = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val now       = System.currentTimeMillis()
    var tab by remember { mutableStateOf(0) }

    Scaffold(containerColor = C.bgPage,
        bottomBar = {
            NavigationBar(containerColor = C.navBarColor, tonalElevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, C.navBarBorder), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))) {
                listOf(Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.Default.DateRange, "Classes"),
                    Triple(2, Icons.Default.List, "Leave"),
                    Triple(3, Icons.Default.Person, "Profile")).forEach { (idx, icon, label) ->
                    NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                        icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = C.digiRed, selectedTextColor = C.digiRed,
                            unselectedIconColor = Color.White.copy(0.60f), unselectedTextColor = Color.White.copy(0.60f),
                            indicatorColor = C.digiRedSoft))
                }
            }
        }
    ) { pad ->
        // Shared header
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.fillMaxWidth()
                .background(C.headerGradientTop)
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Good ${greeting()},", color = Color.White.copy(0.6f), fontSize = 13.sp)
                        Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(C.digiRed, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(studentId, color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Box(Modifier.size(42.dp)
                        .background(Brush.radialGradient(listOf(C.digiRedDark, C.digiRed)), CircleShape)
                        .clickable { onLogout() }, contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Tab content
            when (tab) {
                // HOME: today's classes (compact) + leave summary
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text("📅  Today ${fmtDate(now)}",
                        color = C.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp))
                    sessions.forEach { s ->
                        val status = sessionStatus(s)
                        ClassRow(s, status == "Now", status == "Done") { onCheckIn(s) }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Leave Requests", color = C.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("+ New", color = C.digiRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNewLeave() })
                    }
                    Spacer(Modifier.height(12.dp))
                    if (leaveList.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, C.dividerColor, RoundedCornerShape(14.dp)).padding(20.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No leave requests yet", color = C.textMuted, fontSize = 13.sp)
                        }
                    } else {
                        leaveList.take(2).forEach { LeaveCardCompact(it); Spacer(Modifier.height(8.dp)) }
                        if (leaveList.size > 2)
                            Text("View all ${leaveList.size} →", color = C.digiRed, fontSize = 13.sp,
                                modifier = Modifier.clickable { tab = 2 })
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // CLASSES: full schedule list
                1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text("YOUR SCHEDULE", color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 14.dp))
                    sessions.forEach { s ->
                        val status = sessionStatus(s)
                        ScheduleCard(s, status == "Now", status == "Done") { onCheckIn(s) }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // LEAVE: full leave list + new button
                2 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MY LEAVE REQUESTS", color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Box(Modifier.background(C.digiRed, RoundedCornerShape(8.dp)).clickable { onNewLeave() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("+ New", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (leaveList.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, C.dividerColor, RoundedCornerShape(14.dp)).padding(32.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No leave requests yet", color = C.textMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        leaveList.forEach { LeaveCardFull(it); Spacer(Modifier.height(10.dp)) }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // PROFILE
                else -> ProfileTab(
                    userName    = userName,
                    userId      = studentId,
                    role        = role,
                    initials    = initials,
                    themeMode   = themeMode,
                    onThemeChange = onThemeChange,
                    onLogout    = onLogout
                )
            }
        }
    }
}

@Composable
// ClassRow shows a single class session as a compact card on the Home tab.
// Live sessions get a red background and a pulsing dot with a tap to check in prompt.
// Done sessions are faded out and show a green Done badge.
// Upcoming sessions show a blue Later badge.
fun ClassRow(session: SessionEntity, isLive: Boolean, isDone: Boolean, onCheckIn: () -> Unit) {
    val a by animateFloatAsState(if (isDone) 0.4f else if (!isLive) 0.6f else 1f, tween(300), label = "ra")
    val C = LocalColors.current
    Box(Modifier.fillMaxWidth().alpha(a)
        .background(if (isLive) C.digiRedSoft else C.bgCard, RoundedCornerShape(12.dp))
        .border(1.dp, if (isLive) C.digiRedBorder else C.dividerColor, RoundedCornerShape(12.dp))
        .clickable(enabled = isLive) { onCheckIn() }) {

        // Draw a pulsing red left border strip on live sessions to draw attention
        if (isLive) {
            val sa by rememberInfiniteTransition(label = "s").animateFloat(0.5f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "sv")
            Box(Modifier.width(3.dp).fillMaxHeight()
                .background(C.digiRed.copy(sa), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .align(Alignment.CenterStart))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = if (isLive) 8.dp else 0.dp)) {
                Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                    color = if (isLive) C.digiRed else C.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(session.courseCode, color = if (isLive) C.digiRed else C.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(session.title, color = C.textSecondary, fontSize = 12.sp)
                if (session.room.isNotBlank()) Text(session.room, color = C.textMuted, fontSize = 11.sp)
            }
            when {
                isLive -> Column(horizontalAlignment = Alignment.End) {
                    Row(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(color = C.digiRed); Spacer(Modifier.width(4.dp))
                        Text("Now", color = C.digiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to check in", color = C.digiRed, fontSize = 10.sp)
                }
                isDone -> Box(Modifier.background(C.successSoft, RoundedCornerShape(20.dp))
                    .border(1.dp, C.successBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Done", color = C.successGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                else   -> Box(Modifier.background(C.infoBlueSoft, RoundedCornerShape(20.dp))
                    .border(1.dp, C.infoBlueBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Later", color = C.infoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
// LeaveCardCompact shows a single leave request as a small summary card on the Home tab.
// It shows the affected course codes, leave type, date range, and a colored status badge.
fun LeaveCardCompact(leave: LeaveRequestEntity) {
    val C = LocalColors.current
    val (sc, sb, sbd) = leaveStatusColors(leave.status)
    Row(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(14.dp))
        .border(1.dp, C.dividerColor, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(leave.affectedCourseCodes.replace(",", " · "), color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${leave.leaveType} · ${fmtDateLong(leave.startDateMs)} – ${fmtDateLong(leave.endDateMs)}",
                color = C.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.background(sb, RoundedCornerShape(20.dp)).border(1.dp, sbd, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(leave.status.lowercase().replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
// leaveStatusColors returns the correct text color, background color, and border color
// for a leave request based on its status.
// Approved is green, Rejected is red, and Pending is amber.
fun leaveStatusColors(status: String): Triple<Color, Color, Color> {
    val C = LocalColors.current
    return when (status) {
        "APPROVED" -> Triple(C.successGreen, C.successSoft, C.successBorder)
        "REJECTED" -> Triple(C.digiRed,      C.digiRedSoft, C.digiRedBorder)
        else        -> Triple(C.warningAmber, C.warningBg,   C.warningBorder)
    }
}

//  SCHEDULE CARD (used in Classes tab more detailed than ClassRow)
@Composable
// ScheduleCard shows a single class session as a detailed card on the Classes tab.
// It shows the course code, title, time, room, and a status badge.
// Live sessions show a pulsing red left border and a Check In Now button at the bottom.
// Done sessions are shown at half opacity.
fun ScheduleCard(session: SessionEntity, isLive: Boolean, isDone: Boolean, onCheckIn: () -> Unit) {
    val alpha by animateFloatAsState(if (isDone) 0.5f else 1f, tween(300), label = "sca")
    val C = LocalColors.current
    Box(
        Modifier.fillMaxWidth().alpha(alpha)
            .background(C.bgCard, RoundedCornerShape(14.dp))
            .border(1.dp, if (isLive) C.digiRed.copy(0.4f) else C.dividerColor, RoundedCornerShape(14.dp))
    ) {
        if (isLive) {
            val sa by rememberInfiniteTransition(label = "scl").animateFloat(0.4f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scv")
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(C.digiRed.copy(sa), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .align(Alignment.CenterStart))
        }
        Column(Modifier.fillMaxWidth().padding(start = if (isLive) 18.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    // Course code + status badge row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(session.courseCode, color = if (isLive) C.digiRed else C.textPrimary,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        when {
                            isLive -> Row(
                                Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                                    .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(size = 5); Spacer(Modifier.width(4.dp))
                                Text("LIVE", color = C.digiRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            isDone -> Box(Modifier.background(C.successSoft, RoundedCornerShape(20.dp))
                                .border(1.dp, C.successBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("DONE", color = C.successGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            else -> Box(Modifier.background(C.infoBlueSoft, RoundedCornerShape(20.dp))
                                .border(1.dp, C.infoBlueBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("UPCOMING", color = C.infoBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(session.title, color = C.textSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = C.dividerColor, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Time
                Column {
                    Text("TIME", color = C.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                        color = C.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                // Room
                if (session.room.isNotBlank()) Column {
                    Text("ROOM", color = C.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(session.room, color = C.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (isLive) {
                Spacer(Modifier.height(12.dp))
                DigiButton("Check In Now", onClick = onCheckIn, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

//  LOCATION CHECK SCREEN
@Composable
// LocationCheckScreen is the first step of the check-in flow.
// It shows a map visualization and checks if the student is within the campus boundary.
// If the session has a geofence set, the student must be close enough to campus to proceed.
// There is also a developer toggle at the bottom to spoof the location during testing.
fun LocationCheckScreen(
    session: SessionEntity,
    lastLocation: Location?,
    spoofLocation: Boolean = false,
    onToggleSpoof: () -> Unit = {},
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    // Check if this session has a geofence configured
    // Not all sessions require the student to be on campus to check in
    val hasFence = session.fenceLat != null && session.fenceLng != null && session.fenceRadiusM != null
    // withinFence is true if the student is close enough to campus
    // If spoof is on or no fence is set we always pass this check
    val withinFence = remember(lastLocation, spoofLocation) {
        if (spoofLocation) true
        else if (!hasFence || lastLocation == null) true
        else LocationFence.withinFence(lastLocation, session.fenceLat!!, session.fenceLng!!, session.fenceRadiusM!!)
    }
    val locationKnown = lastLocation != null || spoofLocation

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar(title = "Check-In", onBack = onBack) {
            Box(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp)).border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Map area
        Box(Modifier.fillMaxWidth().height(210.dp)
            .background(Brush.verticalGradient(listOf(C.bgPage, C.bgSurface2))),
            contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val step = 40.dp.toPx(); val lc = Color.White.copy(0.03f)
                var x = 0f; while (x < size.width) { drawLine(lc, Offset(x, 0f), Offset(x, size.height)); x += step }
                var y = 0f; while (y < size.height) { drawLine(lc, Offset(0f, y), Offset(size.width, y)); y += step }
            }
            val pulse by rememberInfiniteTransition(label = "p").animateFloat(0.6f, 1.4f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pv")
            Box(Modifier.size((80 * pulse).dp).background(C.digiRed.copy(0.1f), CircleShape))
            Box(Modifier.size(60.dp).background(C.digiRed.copy(0.15f), CircleShape).border(2.dp, C.digiRed.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).background(C.digiRed, CircleShape))
            }
            Box(Modifier.align(Alignment.TopStart).padding(12.dp)
                .background(if (C.isDark) Color.Black.copy(0.65f) else Color.White.copy(0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, if (C.isDark) Color.White.copy(0.08f) else C.dividerColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("📍  SIT@Punggol", color = if (C.isDark) Color.White.copy(0.9f) else C.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Location status
            val locOk = locationKnown && withinFence
            Row(Modifier.fillMaxWidth()
                .background(when { locOk -> C.successSoft; !locationKnown -> C.bgCard; else -> C.digiRedSoft }, RoundedCornerShape(14.dp))
                .border(1.dp, when { locOk -> C.successBorder; !locationKnown -> C.dividerColor; else -> C.digiRedBorder }, RoundedCornerShape(14.dp))
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(if (locOk) C.successSoft else C.digiRedSoft, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(if (locOk) "✅" else if (!locationKnown) "⏳" else "❌", fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(when { !locationKnown -> "Fetching location…"; locOk -> "Within Campus Boundary"; else -> "Outside Campus" },
                        color = if (locOk) C.successGreen else if (!locationKnown) C.textPrimary else C.digiRed,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (locationKnown) "SIT Punggol Campus · GPS verified" else "Please wait…",
                        color = C.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Session card
            Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(12.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.courseCode, color = C.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(session.title, color = C.textSecondary, fontSize = 12.sp)
                        Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}", color = C.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (session.room.isNotBlank()) Box(Modifier.background(C.bgSurface2, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Text(session.room, color = C.textSecondary, fontSize = 11.sp)
                    }
                }
            }

            if (!withinFence && hasFence && locationKnown) {
                Box(Modifier.fillMaxWidth().background(C.digiRedSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, C.digiRedBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Text("⚠️  You appear to be outside the campus boundary. Check-in may be rejected.",
                        color = C.digiRed, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // DEV: Location spoof toggle
            Box(
                Modifier.fillMaxWidth()
                    .background(if (spoofLocation) Color(0x1A7B1FA2) else C.bgSurface2, RoundedCornerShape(12.dp))
                    .border(1.dp, if (spoofLocation) Color(0x407B1FA2) else C.dividerColor, RoundedCornerShape(12.dp))
                    .clickable { onToggleSpoof() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧪", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DEV: Spoof Campus Location", color = if (spoofLocation) Color(0xFFCE93D8) else C.textSecondary,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (spoofLocation) "Active treating you as inside campus" else "Off using real GPS",
                            color = C.textMuted, fontSize = 11.sp)
                    }
                    Box(Modifier.size(22.dp).background(
                        if (spoofLocation) Color(0xFF7B1FA2) else C.bgSurface3, RoundedCornerShape(6.dp)),
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

//  QR / PIN SCREEN
@Composable
// QrPinScreen is the second step of the check-in flow.
// The student can either scan the QR code shown by the professor
// or type in the 6-digit class password manually.
// There are also developer buttons at the bottom to simulate pass and fail outcomes during testing.
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

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("Verify Class", onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {

            // QR section
            FieldLabel("SCAN QR CODE")
            Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(18.dp))
                .border(2.dp, if (scannedQr != null) C.successGreen else C.digiRed, RoundedCornerShape(18.dp))
                .padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scannedQr != null) {
                        Box(Modifier.size(90.dp).background(C.successSoft, RoundedCornerShape(14.dp)).border(2.dp, C.successBorder, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text("✓", color = C.successGreen, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("QR Code Scanned", color = C.successGreen, fontWeight = FontWeight.Bold)
                    } else {
                        // Show a placeholder QR code graphic when no real QR has been scanned yet
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
                        Text(session.courseCode, color = C.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(); Spacer(Modifier.width(6.dp))
                            Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}", color = C.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            DigiButton(if (scannedQr != null) "✓ Scanned Rescan" else "Open QR Scanner",
                onClick = { showQr = true; scannedQr = null },
                modifier = Modifier.fillMaxWidth(), outlined = scannedQr != null)

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = C.dividerColor)
                Text("  OR  ", color = C.textMuted, fontSize = 12.sp)
                HorizontalDivider(Modifier.weight(1f), color = C.dividerColor)
            }

            // PIN section
            FieldLabel("6-DIGIT CLASS PASSWORD")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..5).forEach { i ->
                    val ch   = pin.getOrNull(i)?.toString() ?: ""
                    val focus = i == pin.length
                    Box(Modifier.weight(1f).aspectRatio(0.88f).background(C.bgSurface2, RoundedCornerShape(10.dp))
                        .border(1.5.dp, if (focus) C.digiRed else C.dividerColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Text(if (ch.isNotBlank()) "•" else if (focus) "|" else "",
                            color = if (ch.isNotBlank()) C.digiRed else C.textMuted,
                            fontSize = if (ch.isNotBlank()) 22.sp else 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // This text field is hidden visually but still receives keyboard input
            // The user types here and the digits show up in the PIN boxes above
            OutlinedTextField(value = pin, onValueChange = { v ->
                if (v.length <= 6 && v.all(Char::isDigit)) { pin = v; pinError = "" }
            },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                label = { Text("Enter PIN here", color = C.textMuted, fontSize = 13.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.digiRed, unfocusedBorderColor = C.dividerColor,
                    focusedContainerColor = C.bgSurface2, unfocusedContainerColor = C.bgSurface2,
                    focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary, cursorColor = C.digiRed
                ), shape = RoundedCornerShape(12.dp)
            )

            if (pinError.isNotBlank()) Text(pinError, color = C.digiRed, fontSize = 13.sp)

            DigiButton("Confirm", modifier = Modifier.fillMaxWidth(),
                enabled = scannedQr != null || pin.length == 6,
                onClick = {
                    when {
                        scannedQr != null -> onVerified(scannedQr, null)
                        pin.length == 6   -> onVerified(null, pin)
                        else              -> pinError = "Enter 6-digit password or scan QR"
                    }
                })

            // DEV buttons to simulate pass and fail check-in outcomes without needing a real QR code
            HorizontalDivider(color = C.dividerColor, modifier = Modifier.padding(vertical = 4.dp))
            Text("🧪  DEV Simulate check-in outcome", color = C.textMuted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Force success: inject correct QR
                Box(Modifier.weight(1f).background(C.successSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, C.successBorder, RoundedCornerShape(10.dp))
                    .clickable { onVerified(session.qrCodePayload, null) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 18.sp)
                        Text("Force Pass", color = C.successGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Correct QR", color = C.textMuted, fontSize = 10.sp)
                    }
                }
                // Force fail: inject wrong QR
                Box(Modifier.weight(1f).background(C.digiRedSoft, RoundedCornerShape(10.dp))
                    .border(1.dp, C.digiRedBorder, RoundedCornerShape(10.dp))
                    .clickable { onVerified("WRONG_QR_FAIL", null) }
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 18.sp)
                        Text("Force Fail", color = C.digiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Wrong QR", color = C.textMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

//  CHECK-IN SUCCESS SCREEN
@Composable
// CheckInSuccessScreen is shown after the student completes the check-in flow.
// It shows a success or failure result with the session details and the attendance status.
// The result card shows class ID, class name, time, attendance status, and whether a photo was taken.
fun CheckInSuccessScreen(
    session: SessionEntity,
    checkInResult: sg.edu.sit.attendance.data.AttendanceEntity?,
    photoUri: String?,
    onDone: () -> Unit
) {
    val isOk    = checkInResult?.status in listOf("PRESENT", "LATE")
    val sColor  = if (isOk) C.successGreen else C.digiRed
    val sBg     = if (isOk) C.successSoft  else C.digiRedSoft
    val sBorder = if (isOk) C.successBorder else C.digiRedBorder

    // Small delay before showing the result so the animation feels intentional
    // The icon pops in with a spring bounce and the rest of the content fades in
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    val scale by animateFloatAsState(if (show) 1f else 0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium), label = "rs")
    val alpha by animateFloatAsState(if (show) 1f else 0f, tween(400), label = "ca")

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        Box(Modifier.size(120.dp).scale(scale), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(sBg, CircleShape).border(2.dp, sBorder, CircleShape))
            Box(Modifier.size(88.dp).background(sBg, CircleShape))
            Text(if (isOk) "✅" else "❌", fontSize = 50.sp)
        }
        Spacer(Modifier.height(24.dp))

        Column(Modifier.alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isOk) "Check-In Successful" else "Check-In Failed", color = C.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (isOk) "Your attendance has been recorded" else (checkInResult?.reason ?: "Please try again"),
                color = C.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)

            Spacer(Modifier.height(28.dp))

            Column(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(18.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(18.dp)).padding(20.dp)) {
                mapOf("Class ID" to session.courseCode, "Class" to session.title,
                    "Time" to SimpleDateFormat("dd MMM · h:mm a", Locale.getDefault()).format(Date()),
                    "Status" to (checkInResult?.status ?: "—")).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = C.textSecondary, fontSize = 13.sp)
                        Text(value, color = if (label == "Status") sColor else C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (label != "Status") HorizontalDivider(color = C.dividerColor, thickness = 0.5.dp)
                }
                HorizontalDivider(color = C.dividerColor, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Photo", color = C.textSecondary, fontSize = 13.sp)
                    Box(Modifier.size(46.dp).background(C.bgSurface2, RoundedCornerShape(10.dp))
                        .border(1.dp, C.digiRedBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
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

//  LEAVE FORM
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// LeaveFormScreen lets students submit a leave request.
// They can choose a leave type, set a date range, select affected classes,
// attach a supporting document, and add optional remarks.
// Tapping Submit Request sends the request through the view model.
fun LeaveFormScreen(vm: MainViewModel, sessions: List<SessionEntity>, onBack: () -> Unit, onSubmitted: () -> Unit) {
    val leaveTypes = listOf("Medical Leave", "Compassionate Leave", "Personal Leave", "Other")
    var leaveType    by remember { mutableStateOf(leaveTypes[0]) }
    var typeExpanded by remember { mutableStateOf(false) }
    val today        = System.currentTimeMillis()
    var startMs      by remember { mutableStateOf(today) }
    var endMs        by remember { mutableStateOf(today + 86_400_000L) }
    var selCodes     by remember { mutableStateOf(setOf<String>()) }
    var remarks      by remember { mutableStateOf("") }

    // Watch the submit state from the view model
    // When the request is submitted successfully we clear the state and go to the leave list
    val submitState by vm.leaveSubmitState.collectAsState()
    LaunchedEffect(submitState) {
        if (submitState is LeaveSubmitState.Success) { vm.clearLeaveSubmitState(); onSubmitted() }
    }

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("Leave Request", onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Dropdown to pick the type of leave being requested
            Column {
                FieldLabel("LEAVE TYPE")
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(value = leaveType, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.digiRed, unfocusedBorderColor = C.dividerColor,
                            focusedContainerColor = C.bgSurface2, unfocusedContainerColor = C.bgSurface2,
                            focusedTextColor = C.textPrimary, unfocusedTextColor = C.textPrimary),
                        shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(C.bgCard)) {
                        leaveTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(t, color = C.textPrimary) }, onClick = { leaveType = t; typeExpanded = false },
                                modifier = Modifier.background(if (t == leaveType) C.digiRedSoft else Color.Transparent))
                        }
                    }
                }
            }

            // Date range picker for the leave duration
            // Currently tapping adds one day each time, this should use a date picker dialog in production
            Column {
                FieldLabel("DURATION")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("From" to startMs, "To" to endMs).forEachIndexed { i, (label, ms) ->
                        Box(Modifier.weight(1f).background(C.bgSurface2, RoundedCornerShape(12.dp))
                            .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp))
                            .clickable { if (i == 0) startMs += 86_400_000L else endMs += 86_400_000L }
                            .padding(14.dp)) {
                            Column {
                                Text(label, color = C.textMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
                                Text(fmtDateLong(ms), color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Classes affected
            Column {
                FieldLabel("CLASSES AFFECTED")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val C = LocalColors.current
                    sessions.forEach { s ->
                        val sel = s.courseCode in selCodes
                        Box(Modifier.background(if (sel) C.digiRedSoft else C.bgSurface2, RoundedCornerShape(20.dp))
                            .border(1.dp, if (sel) C.digiRedBorder else C.dividerColor, RoundedCornerShape(20.dp))
                            .clickable { selCodes = if (sel) selCodes - s.courseCode else selCodes + s.courseCode }
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(s.courseCode, color = if (sel) C.digiRed else C.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Placeholder for document upload such as an MC or approval letter
            // The file picker is not implemented yet, marked as TODO
            Column {
                FieldLabel("SUPPORTING DOCUMENTS")
                Box(Modifier.fillMaxWidth()
                    .background(C.bgSurface2, RoundedCornerShape(12.dp))
                    .drawBehind {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                        val r = 12.dp.toPx()
                        drawRoundRect(C.digiRedBorder, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
                    }
                    .clickable { /* TODO: file picker */ }.padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📎", fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("MC, letter, or other proof", color = C.textSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("Tap to upload", color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Optional remarks field for the student to add any extra notes
            Column {
                FieldLabel("REMARKS (OPTIONAL)")
                DigiTextField(remarks, { remarks = it }, "Add any additional notes…",
                    singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth())
            }

            if (submitState is LeaveSubmitState.Error)
                Text((submitState as LeaveSubmitState.Error).message, color = C.digiRed, fontSize = 13.sp)

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

//  LEAVE LIST
@Composable
// LeaveListScreen shows all leave requests the student has submitted.
// If there are no requests yet it shows an empty state with a prompt to create one.
// There is a plus button in the top bar to go to the leave form.
fun LeaveListScreen(leaveList: List<LeaveRequestEntity>, onBack: () -> Unit, onNewLeave: () -> Unit) {
    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("My Leave", onBack) {
            IconButton(onClick = onNewLeave) { Icon(Icons.Default.Add, null, tint = C.digiRed) }
        }

        if (leaveList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📄", fontSize = 48.sp); Spacer(Modifier.height(12.dp))
                    Text("No Leave Requests", color = C.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Tap + to submit a new request", color = C.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
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
// LeaveCardFull shows the full details of a leave request as a card.
// It shows the affected courses, leave type, date range, document status,
// and the reason if the request was rejected or who approved it if it was approved.
// A colored left border indicates the status at a glance.
fun LeaveCardFull(leave: LeaveRequestEntity) {
    val C = LocalColors.current
    val (sc, sb, sbd) = leaveStatusColors(leave.status)
    Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(16.dp)).border(1.dp, C.dividerColor, RoundedCornerShape(16.dp))) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(sc, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).align(Alignment.CenterStart))
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(leave.affectedCourseCodes.split(",").joinToString(" · "), color = C.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(leave.leaveType, color = C.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Box(Modifier.background(sb, RoundedCornerShape(20.dp)).border(1.dp, sbd, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(leave.status.lowercase().replaceFirstChar { it.uppercase() }, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = C.dividerColor, modifier = Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 13.sp); Spacer(Modifier.width(6.dp))
                Text("${fmtDateLong(leave.startDateMs)} – ${fmtDateLong(leave.endDateMs)}", color = C.textSecondary, fontSize = 12.sp)
            }
            if (leave.documentUri != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("📎", fontSize = 13.sp); Spacer(Modifier.width(6.dp)); Text("Document attached", color = C.textMuted, fontSize = 12.sp) }
            }
            if (leave.status == "REJECTED" && leave.rejectionReason != null) { Spacer(Modifier.height(6.dp)); Text("✕ ${leave.rejectionReason}", color = C.digiRed, fontSize = 12.sp) }
            if (leave.status == "APPROVED" && leave.reviewedBy != null) { Spacer(Modifier.height(6.dp)); Text("✓ Approved by ${leave.reviewedBy}", color = C.successGreen, fontSize = 12.sp) }
        }
    }
}

//  PROFESSOR DASHBOARD  (P1)
// Demo leave requests visible to professor
// profDemoLeave is demo data used to show sample leave requests on the professor dashboard.
// This would be replaced with real data from the database in a production build.
val profDemoLeave = listOf(
    StudentAttendanceRow("Raj Joshi",   "2200305", "LEAVE", "—", "Medical · 20–21 Feb"),
    StudentAttendanceRow("Maya Koh",    "2200147", "LEAVE", "—", "Personal · 22 Feb"),
)

@Composable
// ProfDashboardScreen is the main screen for professors after they log in.
// It has the same four tab layout as the student dashboard.
// The Home tab shows today's classes with attendance stats and pending leave requests.
// The Classes tab shows all sessions with open and view attendance actions.
// The Leave tab shows all student leave requests with approve and reject buttons.
fun ProfDashboardScreen(
    vm: MainViewModel,
    sessions: List<SessionEntity>,
    onOpenClass: (SessionEntity) -> Unit,
    onViewAttendance: (SessionEntity) -> Unit,
    onLogout: () -> Unit,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    val C = LocalColors.current
    val userName  by vm.currentUserName.collectAsState()
    val profId    by vm.currentStudentId.collectAsState()
    val role      by vm.currentRole.collectAsState()
    val initials  = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val now       = System.currentTimeMillis()
    var tab by remember { mutableStateOf(0) }

    // Demo attendance numbers per session (replace with real DB query later)
    val demoStats = mapOf("s1" to Triple(18, 2, 1), "s2" to Triple(22, 3, 2), "s3" to Triple(30, 0, 0))

    Scaffold(containerColor = C.bgPage,
        bottomBar = {
            NavigationBar(containerColor = C.navBarColor, tonalElevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, C.navBarBorder), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))) {
                listOf(Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.Default.DateRange, "Classes"),
                    Triple(2, Icons.Default.List, "Leave"),
                    Triple(3, Icons.Default.Person, "Profile")).forEach { (idx, icon, label) ->
                    NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                        icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = C.digiRed, selectedTextColor = C.digiRed,
                            unselectedIconColor = Color.White.copy(0.60f), unselectedTextColor = Color.White.copy(0.60f),
                            indicatorColor = C.digiRedSoft))
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Shared header
            Box(Modifier.fillMaxWidth()
                .background(C.headerGradientTop)
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Prof. Dashboard", color = Color.White.copy(0.6f), fontSize = 12.sp, letterSpacing = 0.5.sp)
                        Text(userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(C.digiRed, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(profId, color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Box(Modifier.size(42.dp)
                        .background(Brush.radialGradient(listOf(C.digiRedDark, C.digiRed)), CircleShape)
                        .clickable { onLogout() }, contentAlignment = Alignment.Center) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Tab content
            when (tab) {
                // HOME: today's classes with stats + pending leave summary
                0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text("📅  TODAY ${fmtDate(now)}",
                        color = C.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp))
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
                    Text("PENDING LEAVE REQUESTS", color = C.textMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp))
                    profDemoLeave.forEach { leave ->
                        ProfLeaveRow(leave)
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // CLASSES: full class list (all sessions, expandable to open class access)
                1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text("ALL CLASSES", color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 14.dp))
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
                    Spacer(Modifier.height(16.dp))
                    Text("STUDENT LEAVE REQUESTS", color = C.textMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 14.dp))
                    if (profDemoLeave.isEmpty()) {
                        Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(14.dp))
                            .border(1.dp, C.dividerColor, RoundedCornerShape(14.dp)).padding(24.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No pending leave requests", color = C.textMuted, fontSize = 13.sp)
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
                else -> ProfileTab(
                    userName    = userName,
                    userId      = profId,
                    role        = role,
                    initials    = initials,
                    themeMode   = themeMode,
                    onThemeChange = onThemeChange,
                    onLogout    = onLogout
                )
            }
        }
    }
}

@Composable
fun ProfLeaveRow(student: StudentAttendanceRow, showActions: Boolean = false) {
    val initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val C = LocalColors.current
    Row(
        Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(12.dp))
            .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(C.infoBlueSoft, CircleShape).border(1.dp, C.infoBlueBorder, CircleShape),
            contentAlignment = Alignment.Center) {
            Text(initials, color = C.infoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(student.name, color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${student.studentId} · ${student.note}", color = C.textSecondary, fontSize = 12.sp)
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.background(C.successSoft, RoundedCornerShape(8.dp)).border(1.dp, C.successBorder, RoundedCornerShape(8.dp))
                    .clickable { }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Approve", color = C.successGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.background(C.digiRedSoft, RoundedCornerShape(8.dp)).border(1.dp, C.digiRedBorder, RoundedCornerShape(8.dp))
                    .clickable { }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("Reject", color = C.digiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(Modifier.background(C.infoBlueSoft, RoundedCornerShape(8.dp)).border(1.dp, C.infoBlueBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("Pending", color = C.infoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val C = LocalColors.current
    Box(Modifier.fillMaxWidth()
        .background(C.bgCard, RoundedCornerShape(14.dp))
        .border(1.dp, if (isLive) C.digiRedBorder else C.dividerColor, RoundedCornerShape(14.dp))) {

        if (isLive) {
            val sa by rememberInfiniteTransition(label = "ps").animateFloat(0.5f, 1f,
                infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "sv")
            Box(Modifier.width(3.dp).fillMaxHeight()
                .background(C.digiRed.copy(sa), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .align(Alignment.CenterStart))
        }

        Column(Modifier.fillMaxWidth().padding(start = if (isLive) 16.dp else 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
            // Top row: time, course, badge
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${fmtTime(session.startTimeMs)} – ${fmtTime(session.endTimeMs)}",
                        color = C.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(session.courseCode, color = if (isLive) C.digiRed else C.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${session.title} · ${session.room}", color = C.textSecondary, fontSize = 12.sp)
                }
                when {
                    isLive -> Row(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(); Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = C.digiRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    isDone -> Box(Modifier.background(C.bgSurface3, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("DONE", color = C.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    else -> Box(Modifier.background(C.infoBlueSoft, RoundedCornerShape(20.dp))
                        .border(1.dp, C.infoBlueBorder, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("LATER", color = C.infoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            // Stats row (only show if live or done)
            if (isLive || isDone) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceStatBox("$present", "PRESENT", Modifier.weight(1f))
                    AttendanceStatBox("$absent",  "ABSENT",  Modifier.weight(1f))
                    AttendanceStatBox("$leave",   "LEAVE",   Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AttendanceStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val C = LocalColors.current
    // In light mode boost the soft-bg opacity so boxes are clearly visible on white cards
    // Use a stronger background and border opacity in light mode
    // so the stat boxes are clearly visible on white cards
    val bgAlpha  = if (C.isDark) 0x1A else 0x33
    val brdAlpha = if (C.isDark) 0x40 else 0x60
    val (color, bgColor, borderColor) = when (label) {
        "PRESENT" -> Triple(
            C.successGreen,
            C.successGreen.copy(alpha = bgAlpha / 255f),
            C.successGreen.copy(alpha = brdAlpha / 255f)
        )
        "ABSENT"  -> Triple(
            C.digiRed,
            C.digiRed.copy(alpha = bgAlpha / 255f),
            C.digiRed.copy(alpha = brdAlpha / 255f)
        )
        else      -> Triple(
            C.warningAmber,
            C.warningAmber.copy(alpha = bgAlpha / 255f),
            C.warningAmber.copy(alpha = brdAlpha / 255f)
        )
    }
    Box(modifier.background(bgColor, RoundedCornerShape(10.dp)).border(1.dp, borderColor, RoundedCornerShape(10.dp)).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = color.copy(0.75f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}

//  CLASS ACCESS SCREEN  (P2)
@Composable
fun ClassAccessScreen(
    session: SessionEntity,
    onViewAttendance: () -> Unit,
    onBack: () -> Unit
) {
    var showPassword    by remember { mutableStateOf(false) }
    var extensionMins   by remember { mutableStateOf(10) }
    var extended        by remember { mutableStateOf(false) }

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("CLASS ACCESS", onBack) {
            Box(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // QR Code card
            Column {
                Text("QR CODE", color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp))
                Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(16.dp))
                    .border(1.dp, C.dividerColor, RoundedCornerShape(16.dp)).padding(20.dp),
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
                        Row(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                            .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(); Spacer(Modifier.width(6.dp))
                            Text("Valid for entire class · ${session.courseCode} ${fmtTime(session.startTimeMs)}–${fmtTime(session.endTimeMs)}",
                                color = C.digiRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Session password row
            Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(12.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Session Password · fixed for class", color = C.textSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (showPassword) session.classPassword
                            else session.classPassword.take(2) + " " + "●".repeat(session.classPassword.length - 2),
                            color = C.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp
                        )
                    }
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide" else "Reveal", color = C.digiRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Extend check-in window
            Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(12.dp))
                .border(1.dp, C.dividerColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Extend Check-In Window", color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Currently: ${extensionMins} min window", color = C.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Minus button
                        Box(Modifier.size(36.dp).background(C.digiRed, RoundedCornerShape(8.dp))
                            .clickable { if (extensionMins > 5) extensionMins -= 5 },
                            contentAlignment = Alignment.Center) {
                            Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${extensionMins}m", color = C.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp), textAlign = TextAlign.Center)
                        Box(Modifier.size(36.dp).background(C.digiRed, RoundedCornerShape(8.dp))
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

//  ATTENDANCE LIST SCREEN  (P3)

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
    // Demo roster replace with real attendanceDao.observeSessionAttendance(session.sessionId)
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

    val C = LocalColors.current
    Column(Modifier.fillMaxSize().background(C.bgPage).statusBarsPadding().navigationBarsPadding()) {
        DigiTopBar("ATTENDANCE", onBack) {
            Box(Modifier.background(C.digiRedSoft, RoundedCornerShape(20.dp))
                .border(1.dp, C.digiRedBorder, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(session.courseCode, color = C.digiRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Stats strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttendanceStatBox("$present", "PRESENT", Modifier.weight(1f))
            AttendanceStatBox("$absent",  "ABSENT",  Modifier.weight(1f))
            AttendanceStatBox("$leave",   "LEAVE",   Modifier.weight(1f))
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
        "PRESENT" -> Quadruple(C.successGreen, C.successSoft,  C.successBorder, "✓")
        "LATE"    -> Quadruple(C.warningAmber, C.warningBg,   C.warningBorder, "△")
        "ABSENT"  -> Quadruple(C.digiRed,      C.digiRedSoft,  C.digiRedBorder, "✕")
        "LEAVE"   -> Quadruple(C.infoBlue,     C.infoBlueSoft, C.infoBlueBorder,"📄")
        else      -> Quadruple(C.textMuted,    C.bgSurface2,   C.dividerColor,  "?")
    }

    val initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    val C = LocalColors.current
    Box(Modifier.fillMaxWidth().background(C.bgCard, RoundedCornerShape(14.dp))
        .border(1.dp, C.dividerColor, RoundedCornerShape(14.dp))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(Modifier.size(40.dp).background(
                Brush.radialGradient(listOf(
                    when (student.status) {
                        "PRESENT" -> C.successGreen.copy(if (C.isDark) 0.35f else 0.25f)
                        "LATE"    -> C.warningAmber.copy(if (C.isDark) 0.35f else 0.25f)
                        "ABSENT"  -> C.digiRed.copy(if (C.isDark) 0.35f else 0.25f)
                        else      -> C.infoBlue.copy(if (C.isDark) 0.35f else 0.25f)
                    },
                    C.bgSurface3
                )), CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            // Name & ID
            Column(Modifier.weight(1f)) {
                Text(student.name, color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(student.studentId, color = C.textSecondary, fontSize = 12.sp)
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
                    if (student.note.isNotBlank()) Text(student.note, color = C.textSecondary, fontSize = 10.sp)
                    if (student.status == "LATE" && student.checkInTime != "—") Text(student.checkInTime, color = C.textSecondary, fontSize = 10.sp)
                }

                // Action button
                when (student.status) {
                    "ABSENT" -> Box(Modifier.background(C.digiRed, RoundedCornerShape(8.dp))
                        .clickable { /* TODO: edit attendance */ }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("EDIT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    "LEAVE" -> Box(Modifier.background(C.infoBlueSoft, RoundedCornerShape(8.dp))
                        .border(1.dp, C.infoBlueBorder, RoundedCornerShape(8.dp))
                        .clickable { /* TODO: view leave doc */ }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("VIEW", color = C.infoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    else -> Box(Modifier.background(C.bgSurface3, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("📞", fontSize = 14.sp) // placeholder contact action
                    }
                }
            }
        }
    }
}


//  PROFILE TAB  (shared by Student + Professor dashboards)
@Composable
// ProfileTab shows the logged in user's account information and appearance settings.
// It displays the user's name, ID, and role.
// The appearance section lets the user switch between Dark, Light, and Follow System themes.
// There is also a Sign Out button at the bottom.
fun ProfileTab(
    userName: String,
    userId: String,
    role: String,
    initials: String,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    val C = LocalColors.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        // Avatar + name
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(80.dp)
                    .background(Brush.radialGradient(listOf(C.digiRedDark, C.digiRed)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(userName, color = C.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(userId, color = C.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }

        Spacer(Modifier.height(32.dp))

        // Account section
        Text("ACCOUNT", color = C.textMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 10.dp))

        val cardBorderWidth = if (C.isDark) 1.dp else 1.5.dp
        val dividerThickness = if (C.isDark) 0.5.dp else 1.dp
        Box(
            Modifier.fillMaxWidth()
                .background(C.bgCard, RoundedCornerShape(14.dp))
                .border(cardBorderWidth, C.dividerColor, RoundedCornerShape(14.dp))
        ) {
            Column {
                ProfileInfoRow("Name",   userName,  C, Icons.Default.Person)
                HorizontalDivider(color = C.dividerColor, thickness = dividerThickness, modifier = Modifier.padding(horizontal = 0.dp))
                ProfileInfoRow("ID",     userId,    C, Icons.Default.AccountBox)
                HorizontalDivider(color = C.dividerColor, thickness = dividerThickness, modifier = Modifier.padding(horizontal = 0.dp))
                ProfileInfoRow("Role",   role.replaceFirstChar { it.uppercase() }, C, Icons.Default.AccountCircle)
            }
        }

        Spacer(Modifier.height(32.dp))

        // Appearance section
        Text("APPEARANCE", color = C.textMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 10.dp))

        Box(
            Modifier.fillMaxWidth()
                .background(C.bgCard, RoundedCornerShape(14.dp))
                .border(cardBorderWidth, C.dividerColor, RoundedCornerShape(14.dp))
        ) {
            Column {
                ThemeOption(
                    label    = "Dark",
                    emoji    = "🌙",
                    selected = themeMode == ThemeMode.DARK,
                    C        = C,
                    onClick  = { onThemeChange(ThemeMode.DARK) }
                )
                HorizontalDivider(color = C.dividerColor, thickness = dividerThickness, modifier = Modifier.padding(horizontal = 0.dp))
                ThemeOption(
                    label    = "Light",
                    emoji    = "☀️",
                    selected = themeMode == ThemeMode.LIGHT,
                    C        = C,
                    onClick  = { onThemeChange(ThemeMode.LIGHT) }
                )
                HorizontalDivider(color = C.dividerColor, thickness = dividerThickness, modifier = Modifier.padding(horizontal = 0.dp))
                ThemeOption(
                    label    = "System Colour Theme",
                    emoji    = "📱",
                    selected = themeMode == ThemeMode.SYSTEM,
                    C        = C,
                    onClick  = { onThemeChange(ThemeMode.SYSTEM) }
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        // Sign out
        DigiButton("Sign Out", onClick = onLogout, modifier = Modifier.fillMaxWidth(), outlined = true)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, C: AppColors, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = C.textMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = C.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Text(value, color = C.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ThemeOption(label: String, emoji: String, selected: Boolean, C: AppColors, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) C.digiRedSoft else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (selected) C.digiRed else C.textPrimary,
            fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f))
        if (selected) {
            Box(
                Modifier.size(20.dp)
                    .background(C.digiRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper data class for 4 values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)