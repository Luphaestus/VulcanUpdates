package com.vulcanizer.updates.fragments.info

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.ConfigHandler
import dev.oneuiproject.oneui.widget.Separator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceInfoFragment : Fragment() {

    private var handler = Handler(Looper.getMainLooper())

    private lateinit var upTimeA: Array<Pair<String, Any>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        get_software_info()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove any pending callbacks to avoid memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    private val deviceNameMap = mapOf(
        "SM-N986B" to "Galaxy Note 20 Ultra 5G",
        "SM-N985F" to "Galaxy Note 20 4G",
        "SM-N980F" to "Galaxy Note 20 4G",
        "SM-N981B" to "Galaxy Note 20 5G",
        "SM-G981B" to "Galaxy S20 5G",
        "SM-G980F" to "Galaxy S20 4G",
        "SM-G985F" to "Galaxy S20+ 4G",
        "SM-G986B" to "Galaxy S20+ 5G",
        "SM-G988B" to "Galaxy S20 Ultra 5G"
    )

    // Function to get the user-friendly device name
    fun getDeviceName(): String {
        val model =    Build.MODEL.uppercase()
        return deviceNameMap[model] ?: "${Build.MANUFACTURER} $model"
    }

    fun get_software_info() {
        lifecycleScope.launch {
            val androidDeviceInfoA = withContext(Dispatchers.IO) {
                arrayOf(
                    "Device Name" to getDeviceName(), // User-visible name for the device
                    "Model Code" to android.os.Build.MODEL, // Model code for the device
                    "Manufacturer" to android.os.Build.MANUFACTURER, // Manufacturer of the product/hardware
                    "Device Code" to android.os.Build.DEVICE, // Technical device code
                    "Board" to android.os.Build.BOARD, // Name of the underlying board
                    "Hardware" to android.os.Build.HARDWARE,
                    "Brand" to android.os.Build.BRAND // Brand (e.g., carrier or consumer-visible name)
                )
            }

            val batteryUtil = BatteryInfoUtil(requireContext())
            val batteryA = withContext(Dispatchers.IO) {
                batteryUtil.getBatteryInfo(requireContext())
            }

            val displayInfoUtil = DisplayInfoUtil(requireContext())
            val displayInfoA = withContext(Dispatchers.IO) {
                displayInfoUtil.getDisplayInfo()
            }

            val cameraInfoUtil = BatteryInfoUtil.CameraInfoUtil(requireContext())
            val cameraInfoA = withContext(Dispatchers.IO) {
                cameraInfoUtil.getCameraInfo()
            }

            val androidDeviceInfoT = view?.findViewById<TableLayout>(R.id.androidDeviceInfo)
            val javaEnvironmentInfoT = view?.findViewById<TableLayout>(R.id.fingerPrint)
            val kernelInfoT = view?.findViewById<TableLayout>(R.id.display)
            val cameraInfoT = arrayOf(
                view?.findViewById<TableLayout>(R.id.camera0),
                view?.findViewById<TableLayout>(R.id.camera1),
                view?.findViewById<TableLayout>(R.id.camera2),
                view?.findViewById<TableLayout>(R.id.camera3),
                view?.findViewById<TableLayout>(R.id.camera4)
            )

            val cameraInfoS = arrayOf(
                view?.findViewById<Separator>(R.id.camera0s),
                view?.findViewById<Separator>(R.id.camera1s),
                view?.findViewById<Separator>(R.id.camera2s),
                view?.findViewById<Separator>(R.id.camera3s),
                view?.findViewById<Separator>(R.id.camera4s)
            )

            // Iterate over the software information pairs and create TextViews for each pair
            set_table(androidDeviceInfoA.toList(), androidDeviceInfoT, this@DeviceInfoFragment)
            set_table(batteryA.toList(), javaEnvironmentInfoT, this@DeviceInfoFragment)
            set_table(displayInfoA.toList(), kernelInfoT, this@DeviceInfoFragment)
            try {
                for (i in 0..4) {
                    cameraInfoS[i]?.text = if (i > 0) cameraInfoA[i][1].second.toString().replaceFirstChar { it.uppercase() } + " Camera Information" else "General Camera Information"
                    set_table(cameraInfoA[i], cameraInfoT[i], this@DeviceInfoFragment)
                }
            } catch (e: Exception) {
            }
        }
    }

    suspend fun set_table(softwareInfo: List<Pair<String, Any>>, table: TableLayout?, fragment: Fragment) {
        var appsettings = ConfigHandler(requireContext(), "appsettings")

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
}

class DisplayInfoUtil(private val context: Context) {

    fun getDisplayInfo(): Array<Pair<String, Any>> {
        val displayManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = displayManager.defaultDisplay

        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        val displayInfoA: Array<Pair<String, Any>> = arrayOf(
            "Display ID" to display.displayId,
            "Display Name" to getDisplayName(display),
            "Display Width (px)" to metrics.widthPixels,
            "Display Height (px)" to metrics.heightPixels,
            "Density (dpi)" to metrics.densityDpi,
            "Refresh Rate (Hz)" to getRefreshRate(display),
            "Orientation" to getOrientation(display),
            "Physical Size (inch)" to getPhysicalSize(display),
            "Brightness Mode" to getBrightnessMode(),
            "Screen Timeout (ms)" to getScreenTimeout(),
            "Xdpi" to metrics.xdpi,
            "Ydpi" to metrics.ydpi,
            "Logical Density" to metrics.density,
            "Scaled Density" to metrics.scaledDensity,
            "Font Scale" to getFontScale()
        )

        return displayInfoA
    }

    private fun getDisplayName(display: Display): String {
        return display.name
        return "Unknown"
    }

    private fun getRefreshRate(display: Display): Float {
        val mode = display.mode
        return mode.refreshRate
        return -1f
    }

    private fun getOrientation(display: Display): String {
        return when (display.rotation) {
            Surface.ROTATION_0 -> "Portrait"
            Surface.ROTATION_90 -> "Landscape (right side up)"
            Surface.ROTATION_180 -> "Portrait (upside down)"
            Surface.ROTATION_270 -> "Landscape (left side up)"
            else -> "Unknown"
        }
    }

    private fun getPhysicalSize(display: Display): Float {
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val width = metrics.widthPixels / metrics.xdpi
        val height = metrics.heightPixels / metrics.ydpi
        return Math.sqrt((width * width + height * height).toDouble()).toFloat()
    }

    private fun getBrightnessMode(): String {
        return when (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "Automatic"
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL -> "Manual"
            else -> "Unknown"
        }
    }

    private fun getScreenTimeout(): Int {
        return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1)
    }

    private fun getFontScale(): Float {
        return Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1.0f)
    }
}


class BatteryInfoUtil(private val context: Context) {

    fun getBatteryInfo(context: Context): Array<Pair<String, Any>> {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val health = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val powerSource = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val capacity = getBatteryCapacity(context)


        val batteryA: Array<Pair<String, Any>> = arrayOf(

            "Temperature (°C)" to temperature / 10f,
            "Health" to getBatteryHealthString(health),
            "Power Source" to getPowerSourceString(powerSource),
            "Technology" to technology,
            "Voltage (mV)" to voltage,
            "Capacity (mAh)" to capacity
        )

        return batteryA
    }


    private fun getBatteryHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    private fun getPowerSourceString(powerSource: Int): String {
        return when (powerSource) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "N/A"
        }
    }

    private fun getBatteryCapacity(context: Context): String {
        val mBatteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val chargeCounter =
            mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val capacity = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return ((if (chargeCounter == Int.MIN_VALUE || capacity == Int.MIN_VALUE) 0 else (chargeCounter / capacity * 100).toLong())/1000).toString()
    }



    class CameraInfoUtil(private val context: Context) {

        fun getCameraInfo(): List<List<Pair<String, Any>>> {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            val commonInfoList = mutableListOf<Pair<String, Any>>()
            val cameraInfoList = mutableListOf<List<Pair<String, Any>>>()

            // Common characteristics (using the first camera's characteristics as reference)
            if (cameraIdList.isNotEmpty()) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraIdList[0])

                val aberrationModes =
                    characteristics.get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
                if (aberrationModes != null) {
                    commonInfoList.add("Aberration Modes" to aberrationModes.joinToString(", ") {
                        getAberrationModeString(
                            it
                        )
                    })
                }

                val antibandingModes =
                    characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)
                if (antibandingModes != null) {
                    commonInfoList.add("Antibanding Modes" to antibandingModes.joinToString(", ") {
                        getAntibandingModeString(
                            it
                        )
                    })
                }

                val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                if (aeModes != null) {
                    commonInfoList.add("Auto Exposure Modes" to aeModes.joinToString(", ") {
                        getAeModeString(
                            it
                        )
                    })
                }

                val fpsRanges =
                    characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                if (fpsRanges != null) {
                    commonInfoList.add("Target FPS Ranges" to fpsRanges.joinToString(", ") { "${it.lower}-${it.upper}" })
                }

                val compensationRange =
                    characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                if (compensationRange != null) {
                    commonInfoList.add("Compensation Range" to "${compensationRange.lower} to ${compensationRange.upper}")
                }

                val compensationStep =
                    characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                if (compensationStep != null) {
                    commonInfoList.add("Compensation Step" to compensationStep.toString())
                }

                val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                if (afModes != null) {
                    commonInfoList.add("Auto Focus Modes" to afModes.joinToString(", ") {
                        getAfModeString(
                            it
                        )
                    })
                }

                val effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                if (effects != null) {
                    commonInfoList.add("Effects" to effects.joinToString(", ") { getEffectString(it) })
                }

                val sceneModes =
                    characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                if (sceneModes != null) {
                    commonInfoList.add("Scene Modes" to sceneModes.joinToString(", ") {
                        getSceneModeString(
                            it
                        )
                    })
                }

                val videoStabilizationModes =
                    characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                if (videoStabilizationModes != null) {
                    commonInfoList.add(
                        "Video Stabilization Modes" to videoStabilizationModes.joinToString(
                            ", "
                        ) { getVideoStabilizationModeString(it) })
                }

                val awbModes =
                    characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
                if (awbModes != null) {
                    commonInfoList.add("Auto White Balance Modes" to awbModes.joinToString(", ") {
                        getAwbModeString(
                            it
                        )
                    })
                }

                val maxAeRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)
                commonInfoList.add("Maximum Auto Exposure Regions" to (maxAeRegions ?: 0))

                val maxAwbRegions =
                    characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)
                commonInfoList.add("Maximum Auto White Balance Regions" to (maxAwbRegions ?: 0))

                val hotPixelModes =
                    characteristics.get(CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES)
                if (hotPixelModes != null) {
                    commonInfoList.add("Hot Pixel Modes" to hotPixelModes.joinToString(", ") {
                        getHotPixelModeString(
                            it
                        )
                    })
                }

                val thumbnailSizes =
                    characteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES)
                if (thumbnailSizes != null) {
                    commonInfoList.add("Thumbnail Sizes" to thumbnailSizes.joinToString(", ") { "${it.width}x${it.height}" })
                }

                val focusDistanceCalibration =
                    characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
                commonInfoList.add(
                    "Focus Distance Calibration" to getFocusDistanceCalibrationString(
                        focusDistanceCalibration
                    )
                )

                val capabilitiesArr =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                if (capabilitiesArr != null) {
                    commonInfoList.add("Camera Capabilities" to capabilitiesArr.joinToString(", ") {
                        getCapabilityString(
                            it
                        )
                    })
                }
            }

            // Add commonInfoList to the main list
            cameraInfoList.add(commonInfoList)

            // Individual camera characteristics
            for (cameraId in cameraIdList) {
                val individualCameraInfoList = mutableListOf<Pair<String, Any>>()
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                individualCameraInfoList.add("Camera ID" to cameraId)

                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                individualCameraInfoList.add("Lens Facing" to getLensFacingString(lensFacing))

                val sensorInfo =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                individualCameraInfoList.add("Resolution" to "${sensorInfo?.width}x${sensorInfo?.height}")

                val focalLengths =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focalLengths != null && focalLengths.isNotEmpty()) {
                    individualCameraInfoList.add("Focal Lengths" to focalLengths.joinToString(", "))
                }

                val apertures =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                if (apertures != null && apertures.isNotEmpty()) {
                    individualCameraInfoList.add("Apertures" to apertures.joinToString(", "))
                }

                individualCameraInfoList.add(
                    "Hardware Level" to getHardwareLevelString(
                        characteristics.get(
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                        )
                    )
                )

                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                if (capabilities != null) {
                    individualCameraInfoList.add("Capabilities" to capabilities.joinToString(", ") {
                        getCapabilityString(
                            it
                        )
                    })
                }

                val edgeModes = characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
                if (edgeModes != null) {
                    individualCameraInfoList.add("Edge Modes" to edgeModes.joinToString(", ") {
                        getEdgeModeString(
                            it
                        )
                    })
                }

                val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                individualCameraInfoList.add("Flash Available" to (flashAvailable ?: false))

                // Add individual camera list to the main list
                cameraInfoList.add(individualCameraInfoList)
            }

            return cameraInfoList
        }


        private fun getLensFacingString(facing: Int?): String {
            return when (facing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
        }

        private fun getHardwareLevelString(level: Int?): String {
            return when (level) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
                else -> "Unknown"
            }
        }

        private fun getCapabilityString(capability: Int): String {
            return when (capability) {
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "Manual Post Processing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Raw"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "Private Reprocessing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "Read Sensor Settings"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "Constrained High Speed Video"
                else -> "Unknown"
            }
        }

        private fun getAberrationModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_OFF -> "Off"
                CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_FAST -> "Fast"
                CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY -> "High Quality"
                else -> "Unknown"
            }
        }

        private fun getAntibandingModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_50HZ -> "50Hz"
                CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_60HZ -> "60Hz"
                CameraCharacteristics.CONTROL_AE_ANTIBANDING_MODE_AUTO -> "Auto"
                else -> "Unknown"
            }
        }

        private fun getAeModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_AE_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_AE_MODE_ON -> "On"
                CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH -> "Auto Flash"
                CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> "Always Flash"
                CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> "Auto Flash Redeye"
                else -> "Unknown"
            }
        }

        private fun getAfModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_AF_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "Auto"
                CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "Macro"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "Continuous Video"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "Continuous Picture"
                CameraCharacteristics.CONTROL_AF_MODE_EDOF -> "EDOF"
                else -> "Unknown"
            }
        }

        private fun getEffectString(effect: Int): String {
            return when (effect) {
                CameraCharacteristics.CONTROL_EFFECT_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_EFFECT_MODE_MONO -> "Mono"
                CameraCharacteristics.CONTROL_EFFECT_MODE_NEGATIVE -> "Negative"
                CameraCharacteristics.CONTROL_EFFECT_MODE_SOLARIZE -> "Solarize"
                CameraCharacteristics.CONTROL_EFFECT_MODE_SEPIA -> "Sepia"
                CameraCharacteristics.CONTROL_EFFECT_MODE_POSTERIZE -> "Posterize"
                CameraCharacteristics.CONTROL_EFFECT_MODE_WHITEBOARD -> "Whiteboard"
                CameraCharacteristics.CONTROL_EFFECT_MODE_BLACKBOARD -> "Blackboard"
                CameraCharacteristics.CONTROL_EFFECT_MODE_AQUA -> "Aqua"
                else -> "Unknown"
            }
        }

        private fun getSceneModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_SCENE_MODE_DISABLED -> "Disabled"
                CameraCharacteristics.CONTROL_SCENE_MODE_FACE_PRIORITY -> "Face Priority"
                CameraCharacteristics.CONTROL_SCENE_MODE_ACTION -> "Action"
                CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT -> "Portrait"
                CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE -> "Landscape"
                CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT -> "Night"
                CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> "Night Portrait"
                CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE -> "Theatre"
                CameraCharacteristics.CONTROL_SCENE_MODE_BEACH -> "Beach"
                CameraCharacteristics.CONTROL_SCENE_MODE_SNOW -> "Snow"
                CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET -> "Sunset"
                CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO -> "Steady Photo"
                CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS -> "Fireworks"
                CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS -> "Sports"
                CameraCharacteristics.CONTROL_SCENE_MODE_PARTY -> "Party"
                CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT -> "Candlelight"
                CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE -> "Barcode"
                CameraCharacteristics.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> "High Speed Video"
                CameraCharacteristics.CONTROL_SCENE_MODE_HDR -> "HDR"
                else -> "Unknown"
            }
        }

        private fun getVideoStabilizationModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "On"
                else -> "Unknown"
            }
        }

        private fun getAwbModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_AWB_MODE_OFF -> "Off"
                CameraCharacteristics.CONTROL_AWB_MODE_AUTO -> "Auto"
                CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT -> "Incandescent"
                CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT -> "Fluorescent"
                CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT -> "Warm Fluorescent"
                CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT -> "Daylight"
                CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "Cloudy Daylight"
                CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT -> "Twilight"
                CameraCharacteristics.CONTROL_AWB_MODE_SHADE -> "Shade"
                else -> "Unknown"
            }
        }

        private fun getEdgeModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.EDGE_MODE_OFF -> "Off"
                CameraCharacteristics.EDGE_MODE_FAST -> "Fast"
                CameraCharacteristics.EDGE_MODE_HIGH_QUALITY -> "High Quality"
                CameraCharacteristics.EDGE_MODE_ZERO_SHUTTER_LAG -> "Zero Shutter Lag"
                else -> "Unknown"
            }
        }

        private fun getHotPixelModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.HOT_PIXEL_MODE_OFF -> "Off"
                CameraCharacteristics.HOT_PIXEL_MODE_FAST -> "Fast"
                CameraCharacteristics.HOT_PIXEL_MODE_HIGH_QUALITY -> "High Quality"
                else -> "Unknown"
            }
        }

        private fun getOpticalStabilizationModeString(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_OFF -> "Off"
                CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON -> "On"
                else -> "Unknown"
            }
        }

        private fun getFocusDistanceCalibrationString(calibration: Int?): String {
            return when (calibration) {
                CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED -> "Uncalibrated"
                CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE -> "Approximate"
                CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED -> "Calibrated"
                else -> "Unknown"
            }
        }

        private fun getColorFilterArrangementString(arrangement: Int?): String {
            return when (arrangement) {
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB"
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG"
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG"
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR"
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> "RGB"
                else -> "Unknown"
            }
        }
    }
}

