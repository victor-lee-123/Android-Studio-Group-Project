package sg.edu.sit.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import sg.edu.sit.attendance.auth.LocalAccountEntity
import sg.edu.sit.attendance.auth.LocalAuthDao

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        SessionEntity::class,
        AttendanceEntity::class,
        LeaveRequestEntity::class,
        LocalAccountEntity::class,
        AccountEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AttendanceDao

    // Local auth DAO
    abstract fun localAuthDao(): LocalAuthDao
}