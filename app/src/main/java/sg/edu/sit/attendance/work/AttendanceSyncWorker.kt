package sg.edu.sit.attendance.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import sg.edu.sit.attendance.data.DbProvider
import sg.edu.sit.attendance.sync.FirebaseSync

class AttendanceSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = DbProvider.get(applicationContext).dao()
        val sync = FirebaseSync()

        val pending = dao.getPendingAttendance(limit = 50)
        if (pending.isEmpty()) return Result.success()

        return try {
            for (att in pending) {
                sync.uploadAttendance(att)
                dao.updateSyncStatus(att.attendanceId, "SYNCED")
            }
            Result.success()
        } catch (e: Exception) {
            // Mark failed, but keep it retryable
            pending.forEach { dao.updateSyncStatus(it.attendanceId, "FAILED") }
            Result.retry()
        }
    }
}
