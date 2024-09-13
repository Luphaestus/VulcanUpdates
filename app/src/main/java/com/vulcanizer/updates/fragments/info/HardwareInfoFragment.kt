package com.vulcanizer.updates.fragments.info

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.RandomAccessFile


class HardwareInfoFragment : Fragment() {

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var androidDeviceInfoT : TableLayout
    private lateinit var internalStorageT : TableLayout
    private lateinit var cpuT : TableLayout


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hardware_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        get_software_info()
        system_resources()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove any pending callbacks to avoid memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    fun system_resources() {
        handler.post(object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    val androidDeviceInfoA = getRamInfoA(requireContext())
                    set_table(androidDeviceInfoA, androidDeviceInfoT, this@HardwareInfoFragment)

                    val internalStorageA = getStorageInfoA()
                    set_table(internalStorageA, internalStorageT, this@HardwareInfoFragment)

                    val cpuA = getCpuInfo()
                    set_table(cpuA, cpuT, this@HardwareInfoFragment)
                }
                handler.postDelayed(this, 1000) // Update every second
            }
        })
    }
    fun get_software_info() {
        androidDeviceInfoT = view?.findViewById<TableLayout>(R.id.androidDeviceInfo)!!
        internalStorageT = view?.findViewById<TableLayout>(R.id.javaEnvironmentInfo)!!
        cpuT = view?.findViewById<TableLayout>(R.id.kernelInfo) !!

        val cpuInfoT = view?.findViewById<TableLayout>(R.id.cpuInfo)
        lifecycleScope.launch {
            set_table(getProcessorInfo(), cpuInfoT, this@HardwareInfoFragment)
        }
    }

    suspend fun set_table(softwareInfo: Array<Pair<String, Any>>, table: TableLayout?, fragment: Fragment) {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            table?.removeAllViews() // Clear the table before adding new rows
            softwareInfo.forEach { (name, value) ->
                val textViewName = TextView(fragment.requireContext()).apply {
                    text = name
                    textSize = 16f
                    layoutParams = TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1f // Weight 1 to take up left side
                    ).apply {
                        setMargins(0, 20, 0, 0)
                    }
                }

                val textViewValue = TextView(fragment.requireContext()).apply {
                    text = value.toString()
                    setTextColor(ContextCompat.getColor(context, R.color.primary_text_icon_color_themed)) // Set text color to pure white
                    textSize = 14f
                    layoutParams = TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1f // Weight 2 to take up right side, starting from center
                    ).apply {
                        setMargins(0, 20, 0, 0)
                    }
                }

                val row = TableRow(fragment.requireContext()).apply {
                    addView(textViewName)
                    addView(textViewValue)
                }

                table?.addView(row)
            }
        }
    }




    // Function to get CPU usage per core
    fun getCpuUsagePerCore(): List<Double> {
        val cpuUsages = mutableListOf<Double>()
        for (i in 0..7) {
            try {
                val currentFreq: Double
                val readerCurFreq: RandomAccessFile =
                    RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq", "r")
                val curfreg = readerCurFreq.readLine()
                currentFreq = curfreg.toDouble() / 1000
                readerCurFreq.close()
                cpuUsages.add(currentFreq)
            } catch (ex: IOException) {
                cpuUsages.add(0.toDouble())
                ex.printStackTrace()
            }
        }
        return cpuUsages
    }

    fun getRamInfoA(context: Context): Array<Pair<String, Any>> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem


        val availableRam = memoryInfo.availMem
        val usedRam = totalRam - availableRam
        val ramUsedPercentage = 100 * usedRam / totalRam

        return arrayOf(
            "Total RAM" to "${totalRam / (1024 * 1024)} MB",
            "RAM Used" to "${usedRam / (1024 * 1024)} MB",
            "RAM Percentage Used" to "$ramUsedPercentage%"
        )
    }

    fun getStorageInfoA(): Array<Pair<String, Any>> {
        val stat = StatFs(Environment.getDataDirectory().path)

        val blockSize: Long = stat.blockSizeLong
        val totalBlocks: Long = stat.blockCountLong
        val availableBlocks: Long = stat.availableBlocksLong


        val totalStorage = totalBlocks * blockSize
        val availableStorage = availableBlocks * blockSize
        val usedStorage = totalStorage - availableStorage
        val storageUsedPercentage = 100.0 * usedStorage / totalStorage

        return listOf(
            "Total Internal Storage" to "${totalStorage / (1024 * 1024)} MB",
            "Used Internal Storage" to "${usedStorage / (1024 * 1024)} MB",
            "Internal Storage Percentage Used" to "%.2f%%".format(storageUsedPercentage)
        ).toTypedArray()
    }

    // Function to get the system resource information
    fun getCpuInfo(): Array<Pair<String, Any>> {
        val cpuUsages = getCpuUsagePerCore()
        return cpuUsages.mapIndexed { index, usage -> "Core ${index + 1} Usage" to "$usage Mhz" }.toTypedArray()

    }

    fun getCpuAbis(): String {
        val sb = StringBuilder()

        // Number of cores per CPU architecture
        val cores = mutableMapOf<String, Int>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (abi in Build.SUPPORTED_ABIS) {
                cores[abi] = Runtime.getRuntime().availableProcessors()
            }
        } else {
            cores[Build.CPU_ABI] = Runtime.getRuntime().availableProcessors()
        }

        // Counting the occurrences of each CPU architecture
        val archCounts = mutableMapOf<String, Int>()
        for ((arch, _) in cores) {
            archCounts[arch] = archCounts.getOrDefault(arch, 0) + 1
        }

        // Appending the structure information
        var isFirst = true
        archCounts.forEach { (arch, count) ->
            if (isFirst) {
                sb.append(arch)
                isFirst = false
            } else {
                sb.append(", $arch")
            }
        }

        return sb.toString()
    }


    fun getCpuFeatures(): String {
        val processBuilder = ProcessBuilder("/system/bin/cat", "/proc/cpuinfo")
        return try {
            val process = processBuilder.start()
            val inputStream = process.inputStream
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            val cpuInfo = reader.use { it.readText() }

            // Use regex to find the first occurrence of "Features" and extract the entire line
            val regex = Regex("Features\\s+:\\s+(.*)")
            val matchResult = regex.find(cpuInfo)
            if (matchResult != null) {
                matchResult.groupValues[1]
            } else {
                "Features not found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "Exception Occurred"
        }
    }


    fun getGovernor(): String {
        val processBuilder = ProcessBuilder("/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
        return try {
            val process = processBuilder.start()
            val inputStream = process.inputStream
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            val result = reader.use { it.readText() }.trim()
            inputStream.close()
            result
        } catch (e: IOException) {
            e.printStackTrace()
            "Exception Occurred"
        }
    }

    fun is64Bit(): Boolean {
        val arch = System.getProperty("os.arch")
        return arch != null && (arch.contains("64") || arch.contains("aarch64"))
    }

    fun getProcessorInfo(): Array<Pair<String, Any>> {
        val processor = if (is64Bit()) "64 bit" else "32 bit"
     //   val structure =  Build.BOARD
        val frequency = getMaxCpuFrequency()
        val fabrication = Build.MANUFACTURER
        val supportedAbis = getCpuAbis()
        val cpuGovernor = getGovernor()
        val features = getCpuFeatures()

        return arrayOf(
            "Processor" to processor,
  //          "Structure" to structure,
            "Frequency" to frequency,
            "Fabrication" to fabrication,
            "Supported ABIs" to supportedAbis,
            "CPU Governor" to cpuGovernor,
            "Features" to features
        )
    }

    fun getMaxCpuFrequency(): String {
        val cpuFreqPath = "/sys/devices/system/cpu/"
        val cpuDirs = File(cpuFreqPath).listFiles { file -> file.isDirectory && file.name.startsWith("cpu") && file.name.length > 3 }
        val output = mutableMapOf<String, Int>()

        if (cpuDirs != null) {
            for (cpuDir in cpuDirs) {
                try {
                    val minFreqFile = File(cpuDir, "cpufreq/cpuinfo_min_freq")
                    val maxFreqFile = File(cpuDir, "cpufreq/cpuinfo_max_freq")

                    if (minFreqFile.exists() && maxFreqFile.exists()) {
                        val minFreqKHz = minFreqFile.readText().trim().toInt()
                        val maxFreqKHz = maxFreqFile.readText().trim().toInt()
                        val minFreqMHz = minFreqKHz / 1000
                        val maxFreqMHz = maxFreqKHz / 1000
                        val data = "${minFreqMHz} MHz - ${maxFreqMHz} MHz"
                        if (output.containsKey(data))
                        {
                            output[data] = output[data]!! + 1
                        }
                        else
                        {
                            output[data] = 1
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return "Exception Occurred"
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    return "Invalid Frequency Format"
                }
            }
        }

        if (output.isEmpty())
        {
            return "No CPU frequency information found"
        }
        else
        {
            var outputString : String = ""
            for ((key, value) in output) {
                outputString += "$value x $key\n"
            }
            return outputString
        }
    }


}
