package com.vulcanizer.updates.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vulcanizer.updates.R
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class ApkInstaller {

    companion object {
        private var isRunning = false
        private var isPaused = false
        private var downloadManager: DownloadManager? = null
        private var downloadId: Long? = null
        private var handler: Handler? = null

        fun installUpdate(
            url: String,
            versionName: String,
            context: Context,
            title: String = "Vulcan Updates",
            progressCallback: ((Int, Long, Long) -> Unit)? = null,
            failureCallback: (() -> Unit)? = null, // Callback for download failure
            completionCallback: (() -> Unit)? = null // Callback for download completion
        ) {
            if (isRunning) return
            isRunning = true

            var destination =
                context.externalCacheDir?.absolutePath + "/" + "file" + ".apk"

            val file = File(destination)
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType("application/vnd.android.package-archive")
            request.setTitle(title)
            request.setDescription(versionName)
            request.setDestinationUri(Uri.parse("file://$destination"))

            downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager?.enqueue(request)

            handler = Handler(Looper.getMainLooper())

            val runnable = object : Runnable {
                override fun run() {
                    if (!isPaused) {
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId!!)
                        val cursor = downloadManager?.query(query)
                        cursor?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val totalBytesIdx =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val downloadedBytesIdx =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                                if (totalBytesIdx != -1 && downloadedBytesIdx != -1 && statusIdx != -1) {
                                    val totalBytes = cursor.getLong(totalBytesIdx)
                                    val downloadedBytes = cursor.getLong(downloadedBytesIdx)

                                    if (totalBytes > 0) {
                                        val progress = (downloadedBytes * 100L / totalBytes).toInt()
                                        val downloadedMB = downloadedBytes.toDouble() / (1024 * 1024)
                                        val totalMB = totalBytes.toDouble() / (1024 * 1024)
                                        progressCallback?.invoke(progress, downloadedMB.toLong(), totalMB.toLong())
                                    }
                                    val status = cursor.getInt(statusIdx)
                                    if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL) {
                                        cursor.close()
                                        if (status == DownloadManager.STATUS_FAILED) {
                                            failureCallback?.invoke()
                                            stop()
                                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                            completionCallback?.invoke()
                                            stop()
                                        }
                                        return
                                    }
                                }
                            }
                        }
                        handler?.postDelayed(this, 1000) // Check progress every second if not paused
                    }
                }
            }

            handler?.post(runnable)

            val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId!!)
                        val cursor = downloadManager?.query(query)
                        cursor?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                if (statusIdx != -1) {
                                    val status = cursor.getInt(statusIdx)
                                    if (DownloadManager.STATUS_SUCCESSFUL == status) {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            try {
                                                moveFile(
                                                    context.externalCacheDir?.absolutePath + "/",
                                                    "file" + ".apk",
                                                    "/data/local/tmp/"
                                                )
                                                destination = "/data/local/tmp/" + "file" + ".apk"

                                                val command = "pm install $destination"

                                                // Execute the command
                                                val process = Runtime.getRuntime()
                                                    .exec(arrayOf("su", "-c", command))

                                                // Capture the standard output and error streams
                                                val reader = process.inputStream.bufferedReader()
                                                val errorReader =
                                                    process.errorStream.bufferedReader()

                                                // Read and print the output from the command
                                                val output = reader.use { it.readText() }
                                                val errorOutput = errorReader.use { it.readText() }

                                                // Wait for the process to complete
                                                val exitValue = process.waitFor()

                                                Log.e(
                                                    "InstallationError",
                                                    "Error Output: $errorOutput"
                                                )


                                            } catch (e: Exception) {
                                                // Handle exceptions
                                                Log.e(
                                                    "taisetn",
                                                    "Exception during APK installation",
                                                    e
                                                )
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Download failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                        cursor?.close()
                        context.unregisterReceiver(this)
                    }
                }
            }

            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED
            )
        }

        fun pause() {
            isPaused = true
        }

        fun stop() {
            isRunning = false
            isPaused = false
            handler?.removeCallbacksAndMessages(null)
            downloadManager?.remove(downloadId ?: -1)
        }

        fun isDownloading(): Boolean {
            return isRunning
        }
    }
}



fun isAppInstalled(context: Context, packageName: String): Boolean {
    val packageManager = context.packageManager
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun openApp(packageName: String, context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(it)
    } ?: run {
        // Handle case when intent is null (e.g., app not found)
        Toast.makeText(
            context,
            "Unable to open app",
            Toast.LENGTH_SHORT
        ).show()
    }
}
private fun moveFile(inputPath: String, inputFile: String, outputPath: String) {
    try {
        // Create output directory if it doesn't exist
        val dir = File(outputPath)
        if (!dir.exists()) {
            // Execute mkdir command as root
            val mkdirCommand = "su -c mkdir -p $outputPath"
            val mkdirProcess = Runtime.getRuntime().exec(mkdirCommand)
            mkdirProcess.waitFor()
        }

        // Move file using mv command as root
        val mvCommand = "su -c mv $inputPath$inputFile $outputPath$inputFile"
        val mvProcess = Runtime.getRuntime().exec(mvCommand)
        mvProcess.waitFor()

        // Set executable permissions for the moved file
        val chmodCommand = "su -c chmod +x $outputPath$inputFile"
        val chmodProcess = Runtime.getRuntime().exec(chmodCommand)
        chmodProcess.waitFor()
        val chmodCommand2 = "su -c chmod 777 $outputPath$inputFile"
        val chmodProcess2 = Runtime.getRuntime().exec(chmodCommand2)
        chmodProcess2.waitFor()
        // Delete the original file
        val deleteCommand = "su -c rm $inputPath$inputFile"
        val deleteProcess = Runtime.getRuntime().exec(deleteCommand)
        deleteProcess.waitFor()

    } catch (e: Exception) {
        Log.e("tag", e.message!!)
    }
}

