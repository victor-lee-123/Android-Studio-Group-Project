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
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
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
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val fenceLat: Double?,
    val fenceLng: Double?,
    val fenceRadiusM: Float?,   // e.g. 50m
    val qrCodePayload: String,  // what QR encodes
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
    val photoUri: String?,      // saved image location
    val status: String,         // "PRESENT", "REJECTED"
    val reason: String?,        // why rejected
    val syncStatus: String,     // "PENDING", "SYNCED", "FAILED"
    val createdAtMs: Long = System.currentTimeMillis()
)
