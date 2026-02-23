package sg.edu.sit.attendance.repo

import android.content.Context
import android.location.Location
import sg.edu.sit.attendance.data.AttendanceEntity
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.LeaveRequestEntity
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.work.WorkEnqueue
import com.google.firebase.auth.FirebaseAuth
import sg.edu.sit.attendance.location.LocationFence
import java.util.UUID

class AttendanceRepository(private val context: Context) {

    private val dao  = DbProvider.get(context).dao()
    private val auth = FirebaseAuth.getInstance()

    // ── Observe ──────────────────────────────────────────────────────────
    fun observeSessions() = dao.observeSessions()

    fun observeMyLeaveRequests(): kotlinx.coroutines.flow.Flow<List<LeaveRequestEntity>> {
        val uid = auth.currentUser?.uid ?: "ANON"
        return dao.observeMyLeaveRequests(uid)
    }

    // ── Check-In ─────────────────────────────────────────────────────────
    suspend fun checkIn(
        session: SessionEntity,
        scannedQr: String?,
        enteredPin: String?,
        currentLocation: Location?,
        photoUri: String?
    ): AttendanceEntity {
        val uid = auth.currentUser?.uid ?: "ANON"
        val now = System.currentTimeMillis()

        var status = "PRESENT"
        var reason: String? = null

        // QR or PIN validation (either one can unlock)
        val qrValid  = !scannedQr.isNullOrBlank() && scannedQr == session.qrCodePayload
        val pinValid = !enteredPin.isNullOrBlank() && enteredPin == session.classPassword
        if (!qrValid && !pinValid) {
            status = "REJECTED"
            reason = "Invalid QR / PIN"
        }

        // Time window validation
        if (status == "PRESENT" && (now < session.startTimeMs || now > session.endTimeMs)) {
            status = "REJECTED"
            reason = "Outside check-in window"
        }

        // Location fence (only when session has a fence configured)
        if (status == "PRESENT"
            && session.fenceLat != null
            && session.fenceLng != null
            && session.fenceRadiusM != null
        ) {
            if (currentLocation == null) {
                status = "REJECTED"
                reason = "Location unavailable"
            } else if (!LocationFence.withinFence(currentLocation, session.fenceLat, session.fenceLng, session.fenceRadiusM)) {
                status = "REJECTED"
                reason = "Outside campus boundary"
            }
        }

        // Late check — warn but still PRESENT if within window
        if (status == "PRESENT" && now > session.startTimeMs + 15 * 60_000L) {
            status = "LATE"
        }

        val att = AttendanceEntity(
            attendanceId  = UUID.randomUUID().toString(),
            sessionId     = session.sessionId,
            userUid       = uid,
            checkInTimeMs = now,
            lat           = currentLocation?.latitude,
            lng           = currentLocation?.longitude,
            accuracyM     = currentLocation?.accuracy,
            photoUri      = photoUri,
            status        = status,
            reason        = reason,
            syncStatus    = "PENDING"
        )

        dao.upsertAttendance(att)
        WorkEnqueue.enqueueSync(context)
        return att
    }

    // ── Leave Request ─────────────────────────────────────────────────────
    suspend fun submitLeaveRequest(
        leaveType: String,
        startDateMs: Long,
        endDateMs: Long,
        affectedCourseCodes: List<String>,
        remarks: String,
        documentUri: String?
    ): LeaveRequestEntity {
        val uid = auth.currentUser?.uid ?: "ANON"
        val req = LeaveRequestEntity(
            leaveId             = UUID.randomUUID().toString(),
            userUid             = uid,
            leaveType           = leaveType,
            startDateMs         = startDateMs,
            endDateMs           = endDateMs,
            affectedSessionIds  = "[]",
            affectedCourseCodes = affectedCourseCodes.joinToString(","),
            remarks             = remarks,
            documentUri         = documentUri,
            status              = "PENDING",
            syncStatus          = "PENDING"
        )
        dao.upsertLeaveRequest(req)
        WorkEnqueue.enqueueSync(context)
        return req
    }

    // ── Delete Leave Request ──────────────────────────────────────────────
    suspend fun deleteLeaveRequest(leave: LeaveRequestEntity) {
        dao.deleteLeaveRequest(leave) // Calls the DAO to remove from local DB
        WorkEnqueue.enqueueSync(context) // Optional: triggers sync to remove from remote DB
    }
}