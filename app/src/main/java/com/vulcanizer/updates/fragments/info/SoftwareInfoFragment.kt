package com.vulcanizer.updates.fragments.info

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vulcanizer.updates.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SoftwareInfoFragment : Fragment() {

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var upTimeTable : TableLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_software_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate software information
        getSoftwareInfo()

        // Update system uptime periodically
        updateUptimePeriodically()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove any pending callbacks to avoid memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateUptimePeriodically() {
        handler.post(object : Runnable {
            override fun run() {
                // Update uptime TextView
                setTable(upTimeTable, arrayOf("System Uptime" to getFormattedUptime()))
                // Schedule the next update
                handler.postDelayed(this, 1000) // Update every second
            }
        })
    }

    private fun getFormattedUptime(): String {
        // Get the elapsed real time since device boot in milliseconds
        val uptimeMillis = android.os.SystemClock.elapsedRealtime()
        // Convert uptime to hours, minutes, seconds
        val hours = uptimeMillis / 3600000
        val minutes = (uptimeMillis % 3600000) / 60000
        val seconds = (uptimeMillis % 60000) / 1000
        // Format the uptime string
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getSoftwareInfo() {
        // Fetch software information in a coroutine
        lifecycleScope.launch {
            // Software information arrays
            val androidDeviceInfoA : Array<Pair<String, Any>> = arrayOf(
                "Version Name" to "Android " + android.os.Build.VERSION.RELEASE + " " +  getAndroidVersionName(android.os.Build.VERSION.RELEASE),
                ("OS Name" to System.getProperty("os.name")) as Pair<String, Any>,
                "API Level" to android.os.Build.VERSION.SDK_INT,
                "Build Number" to android.os.Build.ID,
                "Build Time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(Build.TIME)),
                "Build ID" to android.os.Build.DISPLAY,
                "Security Patch Level" to android.os.Build.VERSION.SECURITY_PATCH,
                "Bootloader" to android.os.Build.BOOTLOADER,
                "Baseband" to android.os.Build.getRadioVersion(),
                "Language" to Locale.getDefault().language,
                "Time Zone" to java.util.TimeZone.getDefault().displayName,
            )

            val javaEnvironmentInfoA : Array<Pair<String, Any>> = arrayOf(
                ("Java Runtime Version" to System.getProperty("java.runtime.version")) as Pair<String, Any>,
                ("Java VM Version" to System.getProperty("java.vm.version")) as Pair<String, Any>,
                "Java VM Stack Size" to Thread.currentThread().stackTrace.size,
                ("Java Home" to System.getProperty("java.home")) as Pair<String, Any>,
                ("Java Vendor" to System.getProperty("java.vendor")) as Pair<String, Any>,
                ("Java Vendor URL" to System.getProperty("java.vendor.url")) as Pair<String, Any>
            )

            val kernelInfoA : Array<Pair<String, Any>> = arrayOf(
                ("Kernel Architecture" to System.getProperty("os.arch")) as Pair<String, Any>,
                ("Kernel Version" to System.getProperty("os.version")) as Pair<String, Any>
            )

            // TableLayouts to display software information
            upTimeTable = view?.findViewById<TableLayout>(R.id.uptime)!!
            val androidInfoTable = view?.findViewById<TableLayout>(R.id.androidDeviceInfo)
            val javaEnvironmentTable = view?.findViewById<TableLayout>(R.id.javaEnvironmentInfo)
            val kernelInfoTable = view?.findViewById<TableLayout>(R.id.kernelInfo)

            // Populate TableLayouts with software information
            setTable(upTimeTable, arrayOf("System Uptime" to "")) // Initial value, will be updated continuously
            setTable(androidInfoTable, androidDeviceInfoA)
            setTable(javaEnvironmentTable, javaEnvironmentInfoA)
            setTable(kernelInfoTable, kernelInfoA)
        }
    }

    private fun setTable(table: TableLayout?, data: Array<Pair<String, Any>>) {
        // Clear the table before adding new rows
        table?.removeAllViews()
        // Populate the table with data
        data.forEach { (name, value) ->
            val textViewName = TextView(requireContext()).apply {
                text = name
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.primary_text_icon_color_themed))
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }

            val textViewValue = TextView(requireContext()).apply {
                text = value.toString()
                setTextColor(ContextCompat.getColor(context, R.color.primary_text_icon_color_themed))
                textSize = 14f
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }

            val row = TableRow(requireContext()).apply {
                addView(textViewName)
                addView(textViewValue)
            }

            table?.addView(row)
        }
    }

    private fun getAndroidVersionName(version: String): String {
        // Mapping of Android versions to dessert names
        return when (version) {
            "10" -> "Quince Tart"
            "11" -> "Red Velvet Cake"
            "12" -> "Snow Cone"
            "13" -> "Tiramisu"
            "14" -> "Upside Down Cake"
            else -> ""
        }
    }
}
