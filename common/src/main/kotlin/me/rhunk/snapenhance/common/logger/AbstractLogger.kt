package me.rhunk.snapenhance.common.logger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

abstract class AbstractLogger(
    logChannel: LogChannel,
) {
    private val TAG = logChannel.shortName

    companion object {

        private const val TAG = "SnapEnhanceCommon"

        fun directDebug(message: Any?, tag: String = TAG) {
            Log.println(Log.DEBUG, tag, message.toString())
        }

        fun directError(message: Any?, throwable: Throwable, tag: String = TAG) {
            Log.println(Log.ERROR, tag, message.toString())
            Log.println(Log.ERROR, tag, throwable.stackTraceToString())
        }

    }

    open fun debug(message: Any?, tag: String = TAG) {}

    open fun error(message: Any?, tag: String = TAG) {}

    open fun error(message: Any?, throwable: Throwable, tag: String = TAG) {}

    open fun info(message: Any?, tag: String = TAG) {}

    open fun verbose(message: Any?, tag: String = TAG) {}

    open fun warn(message: Any?, tag: String = TAG) {}

    open fun assert(message: Any?, tag: String = TAG) {}
}

fun Context.fatalCrash(throwable: Throwable) {
    getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_HIGH)
        )
        notify(
            0,
            Notification.Builder(this@fatalCrash, "default")
                .setContentTitle("Failed to load SnapEnhance")
                .setStyle(Notification.BigTextStyle().bigText(throwable.message + "\n" + throwable.stackTraceToString()))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .build()
        )
    }
    exitProcess(1)
}