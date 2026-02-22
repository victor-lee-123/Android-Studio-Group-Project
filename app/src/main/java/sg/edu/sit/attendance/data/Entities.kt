package sg.edu.sit.attendance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String? = null,
    val studentId: String? = null,   // e.g. "2200123"
    val role: String = "STUDENT",    // "STUDENT" or "PROFESSOR"
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
    val courseCode: String = "",      // e.g. "CSD3156"
    val ownerUid: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sessions",
    indices = [Index("groupId")]
)
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val groupId: String,
    val courseCode: String = "",      // e.g. "CSD3156"
    val title: String,
    val room: String = "",            // e.g. "Room 4B-02"
    val startTimeMs: Long,
    val endTimeMs: Long,
    val fenceLat: Double?,
    val fenceLng: Double?,
    val fenceRadiusM: Float?,
    val qrCodePayload: String,
    val classPassword: String = "",   // 6-digit PIN fallback
    val createdByUid: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance",
    indices = [Index("sessionId"), Index("userUid")]
)
data class AttendanceEntity(
    @PrimaryKey val attendanceId: String,
    val sessionId: String,
    val userUid: String,
    val checkInTimeMs: Long,
    val lat: Double?,
    val lng: Double?,
    val accuracyM: Float?,
    val photoUri: String?,
    val status: String,         // "PRESENT", "REJECTED", "LATE"
    val reason: String?,
    val syncStatus: String,     // "PENDING", "SYNCED", "FAILED"
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "leave_requests",
    indices = [Index("userUid")]
)
data class LeaveRequestEntity(
    @PrimaryKey val leaveId: String,
    val userUid: String,
    val leaveType: String,           // "Medical", "Compassionate", "Personal"
    val startDateMs: Long,
    val endDateMs: Long,
    val affectedSessionIds: String,  // JSON array of session IDs
    val affectedCourseCodes: String, // comma-separated, e.g. "CSD3156,MAT2010"
    val remarks: String = "",
    val documentUri: String? = null,
    val status: String = "PENDING",  // "PENDING", "APPROVED", "REJECTED"
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val syncStatus: String = "PENDING",
    val createdAtMs: Long = System.currentTimeMillis()
)