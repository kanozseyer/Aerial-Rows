package com.kanozseyer.aerialrows.kodibridge

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executors

object RefreshScheduler {
    private const val JOB_ID = 0x4b0d1
    private const val PERIOD_MS = 30L * 60L * 1000L

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, RefreshJobService::class.java))
            .setPeriodic(PERIOD_MS)
            .setPersisted(true)
            .build()
        runCatching { scheduler.schedule(job) }
    }
}

class RefreshJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartJob(params: JobParameters): Boolean {
        executor.execute {
            RedLightSync.discoverAndSync(applicationContext)
            TvPublisher.refreshAll(applicationContext)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) RefreshScheduler.schedule(context)
    }
}
