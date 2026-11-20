package be.tvgids.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Ververst de gids 's nachts op de achtergrond, zodat ze klaarstaat als je de app
 * opent en de geschiedenis van een week ook aangevuld wordt als je de app een paar
 * dagen niet gebruikt.
 */
class VernieuwWerker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val r = EpgRepository(applicationContext).haalOp()
        // Geen enkele bron bereikbaar: later opnieuw proberen, maar niet eindeloos.
        return if (r.bronnen.none { it.ok } && runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        private const val NAAM = "gids-vernieuwen"

        /** open-epg ververst rond half drie; even later zijn de nieuwe gegevens er zeker. */
        private val TIJDSTIP: LocalTime = LocalTime.of(3, 30)

        /** Plant de dagelijkse verversing in; een bestaande planning blijft staan. */
        fun plan(context: Context) {
            val verzoek = PeriodicWorkRequestBuilder<VernieuwWerker>(24, TimeUnit.HOURS)
                .setInitialDelay(totVolgende(ZonedDateTime.now(BRUSSEL)).toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAAM, ExistingPeriodicWorkPolicy.KEEP, verzoek)
        }

        internal fun totVolgende(nu: ZonedDateTime): Duration {
            var volgende = nu.with(TIJDSTIP)
            if (!volgende.isAfter(nu)) volgende = volgende.plusDays(1)
            return Duration.between(nu, volgende)
        }
    }
}
