package com.conreo.couchytv.integration.android

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.conreo.couchytv.Actions
import com.conreo.couchytv.R
import com.conreo.couchytv.core.launch.AllowAllLaunchPolicy
import com.conreo.couchytv.core.launch.LaunchDispatcher
import com.conreo.couchytv.core.launch.LaunchExecutor
import com.conreo.couchytv.core.launch.LaunchResult
import com.conreo.couchytv.core.launch.LaunchTarget
import com.conreo.couchytv.core.model.AppId

class AndroidLaunchExecutor(
    private val context: Context,
) : LaunchExecutor {
    override fun execute(target: LaunchTarget): LaunchResult = when (target) {
        is LaunchTarget.App -> launchApp(target.appId)
        is LaunchTarget.TvProgram -> launchTvProgram(target)
    }

    private fun launchApp(appId: AppId): LaunchResult {
        val pm = context.packageManager
        val component = appId.componentName?.let(ComponentName::unflattenFromString)
        val intent = component?.let(Intent::makeMainActivity)
            ?: pm.getLeanbackLaunchIntentForPackage(appId.packageName)
            ?: pm.getLaunchIntentForPackage(appId.packageName)

        if (intent == null) {
            Actions.toast(context, context.getString(R.string.toast_no_launchable))
            return LaunchResult.NoHandler
        }

        return start(intent)
    }

    private fun launchTvProgram(target: LaunchTarget.TvProgram): LaunchResult {
        if (target.intentUri.isNotBlank()) {
            val parsed = runCatching {
                Intent.parseUri(target.intentUri, Intent.URI_INTENT_SCHEME)
            }.getOrNull()
            if (parsed != null) {
                val direct = start(parsed, showFailureToast = false)
                if (direct is LaunchResult.Success) return direct
            }
        }

        if (target.ownerPackage.isNotBlank()) {
            return launchApp(AppId(packageName = target.ownerPackage))
        }

        Actions.toast(context, context.getString(R.string.toast_cannot_open))
        return LaunchResult.NoHandler
    }

    private fun start(
        intent: Intent,
        showFailureToast: Boolean = true,
    ): LaunchResult {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            LaunchResult.Success
        }.getOrElse { error ->
            if (showFailureToast) {
                Actions.toast(context, context.getString(R.string.toast_cannot_open))
            }
            LaunchResult.Failure(error)
        }
    }
}

fun createDefaultLaunchDispatcher(context: Context): LaunchDispatcher = LaunchDispatcher(
    policy = AllowAllLaunchPolicy,
    executor = AndroidLaunchExecutor(context),
)
