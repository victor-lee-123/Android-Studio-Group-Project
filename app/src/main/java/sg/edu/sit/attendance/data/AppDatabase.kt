package sg.edu.sit.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        SessionEntity::class,
        AttendanceEntity::class,
        LeaveRequestEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AttendanceDao
}