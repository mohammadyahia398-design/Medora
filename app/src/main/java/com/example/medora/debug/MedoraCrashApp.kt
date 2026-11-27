package com.example.medora.debug

import android.app.Application
import java.io.File

class MedoraCrashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!isCrashProcess()) CrashReporter.install(this)
    }

    private fun isCrashProcess(): Boolean = try {
        File("/proc/self/cmdline").readText().trim { it <= ' ' }.endsWith(":crash")
    } catch (e: Exception) {
        false
    }
}
