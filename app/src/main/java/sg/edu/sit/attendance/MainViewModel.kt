package sg.edu.sit.attendance

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sg.edu.sit.attendance.data.AttendanceEntity
import sg.edu.sit.attendance.data.LeaveRequestEntity
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.repo.AttendanceRepository

// ── Demo credentials ─────────────────────────────────────────────────────────
// Student:   username = alex.t      password = password123
// Professor: username = rajan.a     password = password123
// (These are checked locally for testing; replace with FirebaseAuth later)
private val DEMO_USERS = mapOf(
    "alex.t"    to Triple("Alex Tan",   "password123", "STUDENT"),
    "rajan.a"   to Triple("Dr. Rajan",  "password123", "PROFESSOR")
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AttendanceRepository(app.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    // ── Auth state ────────────────────────────────────────────────────────
    private val _isLoggedIn       = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserName  = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentUserId    = MutableStateFlow("")
    val currentStudentId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentRole      = MutableStateFlow("STUDENT")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    fun login(username: String, password: String, role: String, onResult: (Boolean, String) -> Unit) {
        // TODO: replace with FirebaseAuth.signInWithEmailAndPassword
        if (username.isBlank() || password.isBlank()) {
            onResult(false, "Please fill in all fields")
            return
        }

        val demo = DEMO_USERS[username.trim().lowercase()]
        when {
            demo == null -> onResult(false, "Username not found")
            demo.second != password -> onResult(false, "Incorrect password")
            demo.third.uppercase() != role.uppercase() -> onResult(false, "Wrong role selected for this account")
            else -> {
                _currentUserId.value   = username.trim().lowercase()
                _currentUserName.value = demo.first
                _currentRole.value     = demo.third
                _isLoggedIn.value      = true
                onResult(true, "")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _isLoggedIn.value      = false
        _currentRole.value     = "STUDENT"
        _currentUserName.value = ""
        _currentUserId.value   = ""
    }

    // ── Sessions ──────────────────────────────────────────────────────────
    val sessions = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Leave requests ────────────────────────────────────────────────────
    val leaveRequests = repo.observeMyLeaveRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Check-In ──────────────────────────────────────────────────────────
    private val _lastCheckInResult = MutableStateFlow<AttendanceEntity?>(null)
    val lastCheckInResult: StateFlow<AttendanceEntity?> = _lastCheckInResult.asStateFlow()

    fun submitCheckIn(
        session: SessionEntity,
        scannedQr: String?,
        enteredPin: String?,
        location: Location?,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val att = repo.checkIn(session, scannedQr, enteredPin, location, photoUri)
            _lastCheckInResult.value = att
        }
    }

    fun clearCheckInResult() { _lastCheckInResult.value = null }

    // ── Leave submission ──────────────────────────────────────────────────
    private val _leaveSubmitState = MutableStateFlow<LeaveSubmitState>(LeaveSubmitState.Idle)
    val leaveSubmitState: StateFlow<LeaveSubmitState> = _leaveSubmitState.asStateFlow()

    fun submitLeaveRequest(
        leaveType: String,
        startDateMs: Long,
        endDateMs: Long,
        affectedCourseCodes: List<String>,
        remarks: String,
        documentUri: String?
    ) {
        viewModelScope.launch {
            _leaveSubmitState.value = LeaveSubmitState.Loading
            try {
                repo.submitLeaveRequest(leaveType, startDateMs, endDateMs, affectedCourseCodes, remarks, documentUri)
                _leaveSubmitState.value = LeaveSubmitState.Success
            } catch (e: Exception) {
                _leaveSubmitState.value = LeaveSubmitState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearLeaveSubmitState() { _leaveSubmitState.value = LeaveSubmitState.Idle }
}

sealed class LeaveSubmitState {
    object Idle    : LeaveSubmitState()
    object Loading : LeaveSubmitState()
    object Success : LeaveSubmitState()
    data class Error(val message: String) : LeaveSubmitState()
}