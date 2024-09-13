package com.vulcanizer.updates.fragments.tweaks

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.kdownloader.DownloaderConfig
import com.kdownloader.KDownloader
import com.vulcanizer.updates.R
import com.vulcanizer.updates.activities.NoInternetActivity
import com.vulcanizer.updates.fragments.main.createTweak
import com.vulcanizer.updates.utils.ConfigHandler
import com.vulcanizer.updates.utils.DataDownloader
import com.vulcanizer.updates.utils.XMLParser
import com.vulcanizer.updates.utils.getFileNameFromUrl
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

typealias DownloadCompletionHandler = (
    tempPath: String,
    fileName: String,
    textView: TextView,
    linearLayout: LinearLayout,
    packageName: String,
    tweakContainer: LinearLayout,
    context: Context,
    previous: String,
    callback: (Boolean) -> Unit
) -> Unit


fun setStatus(view: TextView, roundLinearLayout: LinearLayout, isSuccess:Boolean, previous: String="", completeStatus: ((container: LinearLayout, name: String) -> Unit)? = null, packageName: String="") {

    roundLinearLayout.findViewById<SeslProgressBar>(R.id.progress).visibility = View.GONE
    roundLinearLayout.findViewById<AppCompatImageButton>(R.id.stop).visibility = View.GONE
    roundLinearLayout.findViewById<AppCompatImageButton>(R.id.pause).visibility = View.GONE
    roundLinearLayout.findViewById<AppCompatImageButton>(R.id.download).visibility = View.VISIBLE
    if (isSuccess)
    {
        view.text = previous
        completeStatus!!(roundLinearLayout, packageName)
    }
    else {
        view.text = "FAILED"
    }
}


fun setupTweaks(
    url: String,
    container: LinearLayout,
    context: Context,
    activity: Activity,
    handleDownloadCompletion : DownloadCompletionHandler,
    fragment: Fragment,
    setStatus: (container: LinearLayout, name: String) -> Unit
) {

    val progressBar = SeslProgressBar(context, null, android.R.attr.progressBarStyleLarge).apply {
        id = R.id.tweak_load // Set the ID
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER // Center the progress bar in the container
        }
        isIndeterminate = true // Set to indeterminate if needed
        visibility = View.VISIBLE // Make it visible
    }

    container.addView(progressBar)

    DataDownloader.run {
        download(
            url,
            null,
            -1,
            object : DataDownloader.DownloadListener {
                override fun onProgressUpdate(progress: String) {
                }

                override fun onComplete(path: String) {
                    handleDownloadComplete(path, context, activity, container, ConfigHandler(context, "download")
                        , handleDownloadCompletion, fragment, setStatus)
                }

                override fun onError(errorMessage: String) {
                    Log.e("FAILED", errorMessage)
                    context.startActivity(Intent(activity, NoInternetActivity::class.java))
                }
            }
        )
    }
}

fun tweakDownloader(
    linearLayout: LinearLayout,
    textView: TextView,
    previous: String,
    packageName: String,
    data: Any?,
    kDownloader: KDownloader,
    tweakContainer: LinearLayout,
    context: Context,
    url: String,
    fileName: String,
    onCompletion: DownloadCompletionHandler,
    setSuccessStatus: (container: LinearLayout, name: String) -> Unit
) {
    val downloadState = ConfigHandler(context, "download")
    val tempPath = context.filesDir.absolutePath

    // Create a NotificationManager
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "download_channel"
    val channelName = "Download Notifications"

    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
    notificationManager.createNotificationChannel(channel)

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(dev.oneuiproject.oneui.R.drawable.ic_oui_download) // Replace with your download icon
        .setBadgeIconType(dev.oneuiproject.oneui.R.drawable.ic_oui_download)
        .setContentTitle("Download in Progress")
        .setContentText("Starting download...")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)

    var lastUpdateTime = System.currentTimeMillis()

    val request = kDownloader.newRequestBuilder(url, tempPath, getFileNameFromUrl(url)!!).build()
    var downloadId = 0
    downloadId = kDownloader.enqueue(request,
        onStart = {
            downloadState.saveBoolean(fileName, true)
            notificationManager.notify(downloadId, notificationBuilder.build()) // Show notification
            linearLayout.findViewById<AppCompatImageButton>(R.id.pause).visibility = View.VISIBLE
            linearLayout.findViewById<AppCompatImageButton>(R.id.pause).setOnClickListener {
                kDownloader.pause(downloadId)
            }
            linearLayout.findViewById<AppCompatImageButton>(R.id.stop).setOnClickListener {
                kDownloader.cancel(downloadId)
            }
        },

        onProgress = {
            val currentTime = System.currentTimeMillis()
            val progress = (request.downloadedBytes * 100 / request.totalBytes).toInt()

            // Update the text view with the current download status
            textView.text = "${request.downloadedBytes / 1024 / 1024}MB / ${request.totalBytes / 1024 / 1024}MB"

            // Check if at least 1 second has passed since the last update
            if (currentTime - lastUpdateTime >= 1000) {
                // Update notification with progress
                notificationBuilder.setContentText("Downloaded ${request.downloadedBytes / 1024 / 1024}MB / ${request.totalBytes / 1024 / 1024}MB")
                    .setProgress(100, progress, false)
                notificationManager.notify(downloadId, notificationBuilder.build())

                // Update the last update time
                lastUpdateTime = currentTime
            }
        },
        onCompleted = {
            downloadState.saveBoolean(fileName, false)
            textView.text = "Installing..."
            // Update notification to show download completed
            notificationBuilder.setContentText("Download completed")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setSmallIcon(dev.oneuiproject.oneui.R.drawable.ic_oui_checkbox_checked) // Replace with your download icon
                .setBadgeIconType(dev.oneuiproject.oneui.R.drawable.ic_oui_checkbox_checked)

            notificationManager.notify(downloadId, notificationBuilder.build())

            onCompletion(tempPath, fileName, textView, linearLayout, packageName, tweakContainer, context, previous) { success ->
                setStatus(textView, linearLayout, success, previous, setSuccessStatus, packageName)
            }
        },
        onError = {
            downloadState.saveBoolean(fileName, false)

            // Update notification to show error
            notificationBuilder.setContentText("Download failed: $it")
                .setProgress(0, 0, false)
                .setOngoing(false)
            notificationManager.notify(downloadId, notificationBuilder.build())

            if (it == "Cancelled")
                setStatus(textView, linearLayout, true, previous, setSuccessStatus, packageName)
            else
                setStatus(textView, linearLayout, false)
        },
        onPause = {
            linearLayout.findViewById<SeslProgressBar>(R.id.progress).visibility = View.GONE
            linearLayout.findViewById<AppCompatImageButton>(R.id.stop).visibility = View.GONE
            linearLayout.findViewById<AppCompatImageButton>(R.id.pause).visibility = View.GONE
            linearLayout.findViewById<AppCompatImageButton>(R.id.play).visibility = View.VISIBLE
            textView.text = "Paused ".plus(textView.text)

            // Update notification to show paused
            notificationBuilder.setContentText("Download paused")
                .setOngoing(false)
            notificationManager.notify(downloadId, notificationBuilder.build())
        }
    )
}


fun handleDownloadComplete(
    path: String,
    context: Context,
    activity: Activity,
    tweakContainer: LinearLayout,
    downloadState: ConfigHandler,
    onCompletion: DownloadCompletionHandler,
    fragment: Fragment,
    setStatus: (container: LinearLayout, name: String) -> Unit
) {    DataDownloader.logFileContent(path)
    val document = XMLParser.parseXMLFile(path)

    document?.let {
        val map = XMLParser.documentToMap(document)
        for (key in map.keys) {
            val data = map[key]!! as MutableMap<String, String>
            val name = key.replace('_', ' ').split(" ").joinToString(" ") { it.capitalize() }
            data["name"] = name

            val packageName = data["package"].toString()
            val kDownloader = KDownloader.create(context, DownloaderConfig(databaseEnabled = true, readTimeOut = 80000, connectTimeOut = 80000))
            val url = data["downloadurl"].toString()
            val fileName = getFileNameFromUrl(url)!!

            val ss = if (data["ss"] == null) listOf() else data["ss"]!!.split("\\s*,\\s*".toRegex())
            val layout = createTweak(
                title = name,
                brief = data["brief"]!!,
                heading = data["heading"]!!,
                description = data["description"]!!,
                icon = data["icon"]!!,
                root = tweakContainer,
                context = context,
                data = data,
                fragment = fragment,
                images = ss,
                download = { linearLayout, textView, previous, data, context ->
                    tweakDownloader(linearLayout, textView, previous, packageName, data, kDownloader, tweakContainer, context, url, fileName, onCompletion, setStatus)
                }
            )

            if (downloadState.getBoolean(fileName, false)) {
                layout.findViewById<SeslProgressBar>(R.id.progress).visibility = View.VISIBLE
                layout.findViewById<AppCompatImageButton>(R.id.stop).visibility = View.VISIBLE
                layout.findViewById<AppCompatImageButton>(R.id.pause).visibility = View.GONE
                layout.findViewById<AppCompatImageButton>(R.id.download).visibility = View.GONE
                layout.findViewById<AppCompatImageButton>(R.id.play).visibility = View.GONE
                val brief = layout.findViewById<TextView>(R.id.brief)
                brief.text = "Waiting..."
                tweakDownloader(layout, brief, data["brief"]!!, packageName, data, kDownloader, tweakContainer, context, url, fileName, onCompletion, setStatus)
            }

            fun traverseViews(view: View) {
                if (view.id == R.id.tweak_load) {
                    view.visibility = View.GONE
                }
                if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        traverseViews(view.getChildAt(i))
                    }
                }
            }

            traverseViews(activity.findViewById(android.R.id.content))

            setStatus(layout, data["package"].toString())
        }
    } ?: run {
        context.startActivity(Intent(activity, NoInternetActivity::class.java))
    }
    DataDownloader.deleteFile(path)
}


fun runShellCommand(command: String, onSuccess: () -> Unit = {}, onFailure: (String, String) -> Unit = { _, _ -> }, onFailuerCallback: ((Boolean) -> Unit)? = null ) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val output = StringBuilder()
            reader.lineSequence().forEach { output.append(it).append("\n") }

            val exitCode = process.waitFor()
            reader.close()
            outputStream.close()

            if (exitCode == 0) {
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error executing command: $command"
                    Log.e("Shell Command", "$errorMessage, Exit Code: $exitCode, Output: ${output.toString()}")
                    onFailure(errorMessage, output.toString())
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error occurred"
            Log.e("Shell Command", "Exception occurred while executing command: $command, Message: $errorMessage")
            withContext(Dispatchers.Main) { onFailure("Exception occurred: $errorMessage", "") }
            if (onFailuerCallback != null) onFailuerCallback(false)
        }
    }
}

fun showRebootDialog(isInstall: Boolean, location:String, context: Context) {
    val title = if (isInstall) "Tweak Installed" else "Tweak Uninstalled"
    val message = if (isInstall) {
        "Tweak installed successfully. Please reboot your device for the changes to take effect."
    } else {
        "Tweak uninstalled successfully. Please reboot your device for the changes to take effect."
    }

    AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .setNegativeButton("Reboot now") { dialog, _ ->
            runShellCommand("echo wipe cache >> /cache/recovery/openrecoveryscript")
            runShellCommand("reboot $location",
                onSuccess = {},
                onFailure = { errorMessage, _ ->
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Module installation failed! Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        .show()


}

fun runShellCommandForResult(command: String, onSuccess: (Map<String, Map<String, Any>>) -> Unit, onFailure: () -> Unit) {
    GlobalScope.launch(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val output = StringBuilder()
            reader.lineSequence().forEach { output.append(it) }

            val exitCode = process.waitFor()
            reader.close()
            outputStream.close()

            if (exitCode == 0) {
                val result = parseJsonToMapOfMaps(output.toString())
                withContext(Dispatchers.Main) { onSuccess(result) }
            } else {
                withContext(Dispatchers.Main) { onFailure() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { onFailure() }
        }
    }
}

fun parseJsonToMapOfMaps(jsonString: String): Map<String, Map<String, Any>> {
    val jsonArray = JSONArray(jsonString)
    return (0 until jsonArray.length()).associate { i ->
        val jsonObject = jsonArray.getJSONObject(i)
        val name = jsonObject.getString("name")
        name to jsonObject.toMap()
    }
}

fun JSONObject.toMap(): Map<String, Any> {
    return keys().asSequence().associateWith { get(it) }
}

