package dev.x3d.dayline.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SyncScheduler(private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedulePeriodic(minutes: Int) {
        val interval = minutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<TimetableSyncWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<TimetableSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(UNIQUE)
        workManager.cancelUniqueWork(UNIQUE_ONCE)
    }

    companion object {
        private const val UNIQUE = "dayline-sync"
        private const val UNIQUE_ONCE = "dayline-sync-once"
    }
}
