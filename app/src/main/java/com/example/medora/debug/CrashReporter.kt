package com.example.medora.debug

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object CrashReporter {
    const val FILE_NAME = "last_crash.txt"
    private const val APP_PACKAGE = "com.example.medora"
    private const val MAX_CHARS = 60000

    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildReport(app, thread, throwable)
                File(app.filesDir, FILE_NAME).writeText(report)
                val intent = Intent(app, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                app.startActivity(intent)
                Thread.sleep(300)
            } catch (_: Throwable) {
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun rootCause(t: Throwable): Throwable {
        var cur = t
        var depth = 0
        while (cur.cause != null && cur.cause !== cur && depth < 20) {
            cur = cur.cause!!
            depth++
        }
        return cur
    }

    private fun findLocation(t: Throwable): String {
        val root = rootCause(t)
        val frame = root.stackTrace.firstOrNull { it.className.startsWith(APP_PACKAGE) }
            ?: t.stackTrace.firstOrNull { it.className.startsWith(APP_PACKAGE) }
        return if (frame != null) {
            "${frame.fileName}:${frame.lineNumber}  (${frame.className.substringAfterLast('.')}.${frame.methodName})"
        } else {
            "غير واضح (الخطأ خارج كود Medora)"
        }
    }

    private fun buildReport(app: Application, thread: Thread, t: Throwable): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val version = try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        val root = rootCause(t)
        val sb = StringBuilder()
        sb.append("=== MEDORA CRASH REPORT ===\n")
        sb.append("Time: ").append(time).append('\n')
        sb.append("App: ").append(version).append('\n')
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(" / Android ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("Thread: ").append(thread.name).append('\n')
        sb.append("Location: ").append(findLocation(t)).append('\n')
        sb.append("Root cause: ").append(root.javaClass.name).append(": ").append(root.message).append("\n\n")
        sb.append(Log.getStackTraceString(t))
        return if (sb.length > MAX_CHARS) sb.substring(0, MAX_CHARS) else sb.toString()
    }
}
