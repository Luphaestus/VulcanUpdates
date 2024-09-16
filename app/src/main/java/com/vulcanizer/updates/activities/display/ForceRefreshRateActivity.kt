package com.vulcanizer.updates.activities

import android.content.ContentResolver
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vulcanizer.updates.databinding.ActivityForceRefreshRateBinding
import com.vulcanizer.updates.fragments.tweaks.runShellCommand

class ForceRefreshRateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForceRefreshRateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceRefreshRateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }
        getRefreshRates(contentResolver)


        val refreshRates = listOf(-1, 48, 60, 96, 120)
        val radioButtons = listOf(
            binding.radioRefreshRateAdaptive,
            binding.radioRefreshRate48,
            binding.radioRefreshRate60,
            binding.radioRefreshRate96,
            binding.radioRefreshRate120
        )
        val layouts = listOf(
            binding.refreshRateAdaptive,
            binding.refreshRate48,
            binding.refreshRate60,
            binding.refreshRate96,
            binding.refreshRate120
        )

        for (i in radioButtons.indices) {
            radioButtons[i].setOnClickListener {
                setRefreshRate(refreshRates[i])
            }
            layouts[i].setOnClickListener {
                setRefreshRate(refreshRates[i])
            }
        }
    }
    fun getRefreshRates(contentResolver: ContentResolver) {
        try {
            val minRefreshRate = Settings.System.getFloat(contentResolver, "min_refresh_rate", -1f) // Default to 60.0f if not set
            setRefreshRate(minRefreshRate.toInt(), false)
        } catch (e: Settings.SettingNotFoundException) {
            Log.e("RefreshRates", "Setting not found", e)
        }
    }

    private fun setRefreshRate(refreshRate: Int, apply:Boolean=true) {
        val refreshRateButtons = listOf(
            Pair(binding.radioRefreshRateAdaptive, -1),
            Pair(binding.radioRefreshRate48, 48),
            Pair(binding.radioRefreshRate60, 60),
            Pair(binding.radioRefreshRate96, 96),
            Pair(binding.radioRefreshRate120, 120)
        )
        if (apply) {
            if (refreshRate == -1) {
                runShellCommand("settings delete system min_refresh_rate")
                runShellCommand("settings delete system peak_refresh_rate")
                runShellCommand("settings put secure refresh_rate_mode 1")
            } else {
                runShellCommand("settings put system min_refresh_rate $refreshRate.0")
                runShellCommand("settings put system peak_refresh_rate $refreshRate.0")
                runShellCommand("settings put secure refresh_rate_mode 2")
            }
        }
        for ((radioButton, rate) in refreshRateButtons) {
            radioButton.isChecked = (rate == refreshRate)
        }

    }
}
