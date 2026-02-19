package sg.edu.sit.attendance.repo

import android.content.Context
import android.location.Location
import sg.edu.sit.attendance.data.AttendanceEntity
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.work.WorkEnqueue
import com.google.firebase.auth.FirebaseAuth
import sg.edu.sit.attendance.location.LocationFence
import java.util.UUID

class AttendanceRepository(
    private val context: Context
) {
    private val dao = DbProvider.get(context).dao()
    private val auth = FirebaseAuth.getInstance()

    fun observeSessions() = dao.observeSessions()

    suspend fun seedDemoSessionIfEmpty() {
        // Creates 1 demo session so you can test immediately.
        // Replace with real "Create Session" screen later.
        val existing = dao.observeSessions() // Flow, can't read directly here without collecting
        // Keep base simple: no seed to avoid needing flow collection.
    }

    suspend fun checkIn(
        session: SessionEntity,
        scannedQr: String,
        currentLocation: Location?,
        photoUri: String?
    ): AttendanceEntity {
        val uid = auth.currentUser?.uid ?: "ANON"

        val now = System.currentTimeMillis()

        var status = "PRESENT"
        var reason: String? = null

        // QR validation
        if (scannedQr != session.qrCodePayload) {
            status = "REJECTED"
            reason = "Invalid QR"
        }

        // Time window validation
        if (status == "PRESENT" && (now < session.startTimeMs || now > session.endTimeMs)) {
            status = "REJECTED"
            reason = "Outside time window"
        }

        // Location fence validation (only if session has fence)
        if (status == "PRESENT" && session.fenceLat != null && session.fenceLng != null && session.fenceRadiusM != null) {
            if (currentLocation == null) {
                status = "REJECTED"
                reason = "No location available"
            } else {
                val within = LocationFence.withinFence(
                    current = currentLocation,
                    fenceLat = session.fenceLat,
                    fenceLng = session.fenceLng,
                    radiusM = session.fenceRadiusM
                )
                if (!within) {
                    status = "REJECTED"
                    reason = "Outside location fence"
                }
            }
        }

        val att = AttendanceEntity(
            attendanceId = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            userUid = uid,
            checkInTimeMs = now,
            lat = currentLocation?.latitude,
            lng = currentLocation?.longitude,
            accuracyM = currentLocation?.accuracy,
            photoUri = photoUri,
            status = status,
            reason = reason,
            syncStatus = "PENDING"
        )

        dao.upsertAttendance(att)
        WorkEnqueue.enqueueSync(context)

        return att
    }
}