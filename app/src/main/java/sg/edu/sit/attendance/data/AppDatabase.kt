package sg.edu.sit.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import sg.edu.sit.attendance.data.AttendanceEntity
import sg.edu.sit.attendance.data.GroupEntity
import sg.edu.sit.attendance.data.SessionEntity
import sg.edu.sit.attendance.data.UserEntity

@Database(
    entities = [UserEntity::class, GroupEntity::class, SessionEntity::class, AttendanceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AttendanceDao
}
