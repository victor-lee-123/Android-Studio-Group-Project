package sg.edu.sit.attendance

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.repo.AttendanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AttendanceRepository(app.applicationContext)

    val sessions = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    // Simple UI state
    var lastResult: String = ""
        private set

    fun submitCheckIn(
        session: SessionEntity,
        scannedQr: String,
        location: Location?,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val att = repo.checkIn(session, scannedQr, location, photoUri)
            lastResult = "${att.status}${att.reason?.let { " ($it)" } ?: ""}"
        }
    }
}