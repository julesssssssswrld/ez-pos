package com.iandevs.ezpos

import android.app.Application
import android.util.Log
import com.iandevs.ezpos.data.SoapDatabase
import com.iandevs.ezpos.data.SoapRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoapApp : Application() {
    val database by lazy { SoapDatabase.getDatabase(this) }
    val repository by lazy {
        SoapRepository(database.productDao(), database.saleDao(), database.inventoryLogDao())
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val crashLog = buildString {
                    appendLine("=== EZ-POS Crash Report ===")
                    appendLine("Time: $timestamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine("Stack trace:")
                    appendLine(sw.toString())
                    appendLine("===========================")
                }
                Log.e("EZ-POS", crashLog)

                // Write to internal storage file
                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.appendText(crashLog + "\n")
            } catch (_: Exception) {
                // Don't let crash handler itself crash
            }

            // Delegate to default handler (shows Android crash dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

