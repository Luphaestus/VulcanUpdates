package com.vulcanizer.updates.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vulcanizer.updates.databinding.ActivityBootloaderBootAnimationBinding

class BootloaderBootAnimationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBootloaderBootAnimationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBootloaderBootAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }

    }

}
