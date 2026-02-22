package sg.edu.sit.attendance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // ── Users ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserEntity?

    // ── Groups ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    // ── Sessions ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startTimeMs ASC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getSession(id: String): SessionEntity?

    // ── Attendance ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(att: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE syncStatus = 'PENDING' ORDER BY createdAtMs ASC LIMIT :limit")
    suspend fun getPendingAttendance(limit: Int = 50): List<AttendanceEntity>

    @Query("UPDATE attendance SET syncStatus = :newStatus WHERE attendanceId = :id")
    suspend fun updateSyncStatus(id: String, newStatus: String)

    @Query("SELECT * FROM attendance WHERE userUid = :uid ORDER BY checkInTimeMs DESC")
    fun observeMyAttendance(uid: String): Flow<List<AttendanceEntity>>

    // ── Leave Requests ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaveRequest(req: LeaveRequestEntity)

    @Query("SELECT * FROM leave_requests WHERE userUid = :uid ORDER BY createdAtMs DESC")
    fun observeMyLeaveRequests(uid: String): Flow<List<LeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE syncStatus = 'PENDING' ORDER BY createdAtMs ASC")
    suspend fun getPendingLeaveRequests(): List<LeaveRequestEntity>

    @Query("UPDATE leave_requests SET syncStatus = :newStatus WHERE leaveId = :id")
    suspend fun updateLeaveRequestSyncStatus(id: String, newStatus: String)
}