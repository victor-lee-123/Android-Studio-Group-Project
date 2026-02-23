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
import sg.edu.sit.attendance.data.AccountEntity
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.auth.LocalAuthRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.edu.sit.attendance.auth.LocalSession

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // Inside MainViewModel.kt
    fun deleteLeaveRequest(leave: LeaveRequestEntity) {
        viewModelScope.launch {
            try {
                repo.deleteLeaveRequest(leave)
                // Optionally: clear state or show a toast if needed
            } catch (e: Exception) {
                // Handle error (e.g., log it or show a message)
            }
        }
    }


    private val repo = AttendanceRepository(app.applicationContext)

    private val db = DbProvider.get(app.applicationContext)
    private val authRepo = LocalAuthRepository(db.localAuthDao())

    // ── Auth state ────────────────────────────────────────────────────────
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentStudentId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentRole = MutableStateFlow("STUDENT")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    fun login(username: String, password: String, role: String, onResult: (Boolean, String) -> Unit) {
        val u = username.trim()
        val p = password

        if (u.isBlank() || p.isBlank()) {
            onResult(false, "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            try {
                // ✅ do DB work off main thread
                val acct = withContext(Dispatchers.IO) {
                    authRepo.login(u, p)
                }

                _currentRole.value = role.uppercase()

                _currentUserId.value = acct.username
                _currentUserName.value = acct.studentName
                _isLoggedIn.value = true

                LocalSession.save(getApplication(), acct.accountId)

                onResult(true, "")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Login failed")
            }
        }
    }

    fun signUp(username: String, password: String, studentName: String, role: String, onResult: (Boolean, String) -> Unit) {
        val u = username.trim()
        val p = password
        val n = studentName.trim()

        if (u.isBlank() || p.isBlank() || n.isBlank()) {
            onResult(false, "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            try {
                val acct = withContext(Dispatchers.IO) {
                    authRepo.signUp(u, p, n)
                }

                // log them in immediately after sign up (optional)
                _currentUserId.value = acct.username
                _currentUserName.value = acct.studentName
                _currentRole.value = role.uppercase()
                _isLoggedIn.value = true

                LocalSession.save(getApplication(), acct.accountId)

                onResult(true, "")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Sign up failed")
            }
        }
    }

    // --- hashing helpers ---
    private fun hashPassword(raw: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(raw: String, storedHash: String): Boolean {
        return hashPassword(raw) == storedHash
    }

    fun logout() {
        LocalSession.logout(getApplication())
        _isLoggedIn.value = false
        _currentRole.value = "STUDENT"
        _currentUserName.value = ""
        _currentUserId.value = ""
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


    fun submitCheckIn(session: SessionEntity, scannedQr: String?, enteredPin: String?, location: Location?, photoUri: String?) {
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