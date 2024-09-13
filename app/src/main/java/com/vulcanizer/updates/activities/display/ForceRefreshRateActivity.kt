package com.vulcanizer.updates.activities

import android.content.ContentResolver
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vulcanizer.updates.databinding.ActivityForceRefreshRateBinding

class ForceRefreshRateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForceRefreshRateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceRefreshRateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }
        getRefreshRates(contentResolver)


        val refreshRates = listOf(48, 60, 90, 120)
        val radioButtons = listOf(
            binding.radioRefreshRate48,
            binding.radioRefreshRate60,
            binding.radioRefreshRate90,
            binding.radioRefreshRate120
        )
        val layouts = listOf(
            binding.refreshRate48,
            binding.refreshRate60,
            binding.refreshRate90,
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
            val minRefreshRate = Settings.System.getFloat(contentResolver, "min_refresh_rate", 50.0f) // Default to 60.0f if not set
            val peakRefreshRate = Settings.System.getFloat(contentResolver, "peak_refresh_rate", 50.0f) // Default to 60.0f if not set

            Log.d("RefreshRates", "Min Refresh Rate: $minRefreshRate")
            Log.d("RefreshRates", "Peak Refresh Rate: $peakRefreshRate")
        } catch (e: Settings.SettingNotFoundException) {
            Log.e("RefreshRates", "Setting not found", e)
        }
    }

    private fun setRefreshRate(refreshRate: Int) {
        // List of pairs containing RadioButton and their corresponding refresh rates
        val refreshRateButtons = listOf(
            Pair(binding.radioRefreshRate48, 48),
            Pair(binding.radioRefreshRate60, 60),
            Pair(binding.radioRefreshRate90, 90),
            Pair(binding.radioRefreshRate120, 120)
        )

        // Loop through the list and set the checked state
        for ((radioButton, rate) in refreshRateButtons) {
            radioButton.isChecked = (rate == refreshRate)
        }
    }
}
