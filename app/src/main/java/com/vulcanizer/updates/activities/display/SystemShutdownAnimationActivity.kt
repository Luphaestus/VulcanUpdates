package com.vulcanizer.updates.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vulcanizer.updates.databinding.ActivitySystemShutdownAnimationBinding

class SystemShutdownAnimationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySystemShutdownAnimationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemShutdownAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }

    }

}
