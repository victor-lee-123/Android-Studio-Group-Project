package sg.edu.sit.attendance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sg.edu.sit.attendance.data.AttendanceEntity
import sg.edu.sit.attendance.data.GroupEntity
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.data.UserEntity

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(att: AttendanceEntity)

    @Query("SELECT * FROM sessions ORDER BY createdAtMs DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM attendance WHERE syncStatus = 'PENDING' ORDER BY createdAtMs ASC LIMIT :limit")
    suspend fun getPendingAttendance(limit: Int = 50): List<AttendanceEntity>

    @Query("UPDATE attendance SET syncStatus = :newStatus WHERE attendanceId = :id")
    suspend fun updateSyncStatus(id: String, newStatus: String)
}
