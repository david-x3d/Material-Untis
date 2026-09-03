package dev.x3d.dayline.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.x3d.dayline.domain.PeriodException
import dev.x3d.dayline.domain.PeriodRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimetableSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {
    private val repository: PeriodRepository by inject()

    override suspend fun doWork(): Result {
        val session = repository.session
        return try {
            repository.refresh(force = false)
            Result.success()
        } catch (_: PeriodException.Auth) {
            Result.failure()
        } catch (_: PeriodException.SessionExpired) {
            Result.failure()
        } catch (_: PeriodException.Network) {
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
