package com.vulcanizer.updates.fragments.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.R
import com.vulcanizer.updates.activities.BootloaderBootAnimationActivity
import com.vulcanizer.updates.activities.ForceRefreshRateActivity
import com.vulcanizer.updates.activities.SettingsImageActivity
import com.vulcanizer.updates.activities.SystemBootAnimationActivity
import dev.oneuiproject.oneui.layout.DrawerLayout

class DisplayFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_display, container, false)

        val forceRefreshLayout = layout.findViewById<LinearLayout>(R.id.force_refresh_option)
        val bootloaderBootLayout = layout.findViewById<LinearLayout>(R.id.bootloader_animation_option)
        val systemBootLayout = layout.findViewById<LinearLayout>(R.id.system_animation_option)
        val settingsImageLayout = layout.findViewById<LinearLayout>(R.id.settings_image_option)

        forceRefreshLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ForceRefreshRateActivity::class.java))
        }
        bootloaderBootLayout.setOnClickListener {
            startActivity(Intent(requireContext(), BootloaderBootAnimationActivity::class.java))
        }
        systemBootLayout.setOnClickListener {
            startActivity(Intent(requireContext(), SystemBootAnimationActivity::class.java))
        }
        settingsImageLayout.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsImageActivity::class.java))
        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = (requireActivity() as MainActivity).binding
        binding.drawerLayoutMain.setTitle("Display", "Display")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val binding = (requireActivity() as MainActivity).binding
            binding.drawerLayoutMain.setTitle("Display", "Display")
            scrollToTop()
        }
    }

    private fun scrollToTop() {
        val scrollView = (requireActivity() as MainActivity).findViewById<DrawerLayout>(R.id.drawer_layout_main)
        scrollView?.scrollTo(0, 0)
    }
}


