package sg.edu.sit.attendance.sync

import sg.edu.sit.attendance.data.AttendanceEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseSync(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun currentUidOrNull(): String? = auth.currentUser?.uid

    suspend fun uploadAttendance(att: AttendanceEntity) {
        // Firestore path: groups/{groupId}/sessions/{sessionId}/attendance/{attendanceId}
        // But we don't store groupId in AttendanceEntity (session has it).
        // For base scaffold, store at: attendance/{attendanceId}
        // Your team can later restructure collections by group/session.

        val uid = currentUidOrNull() ?: throw IllegalStateException("Not logged in")

        val doc = hashMapOf(
            "attendanceId" to att.attendanceId,
            "sessionId" to att.sessionId,
            "userUid" to att.userUid,
            "checkInTimeMs" to att.checkInTimeMs,
            "lat" to att.lat,
            "lng" to att.lng,
            "accuracyM" to att.accuracyM,
            "photoUri" to att.photoUri,
            "status" to att.status,
            "reason" to att.reason,
            "createdAtMs" to att.createdAtMs,
            "uploadedBy" to uid
        )

        db.collection("attendance")
            .document(att.attendanceId)
            .set(doc)
            .await()
    }
}

// Minimal Task.await() helper
private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) {} }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
}
