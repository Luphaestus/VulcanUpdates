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
import com.vulcanizer.updates.activities.SystemBootAnimationActivity
import com.vulcanizer.updates.activities.SystemShutdownAnimationActivity
import dev.oneuiproject.oneui.layout.DrawerLayout

class DisplayFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_display, container, false)
        val forceRefreshLayout = layout.findViewById<LinearLayout>(R.id.force_refresh_option)
        forceRefreshLayout.setOnClickListener {
            startActivity(Intent(requireContext(), ForceRefreshRateActivity::class.java))
        }

        val bootloaderBootLayout = layout.findViewById<LinearLayout>(R.id.bootloader_animation_option)
        val systemBootLayout = layout.findViewById<LinearLayout>(R.id.system_animation_option)
        val systemShutdownLayout = layout.findViewById<LinearLayout>(R.id.system_shutdown_animation_option)

        bootloaderBootLayout.setOnClickListener {
            startActivity(Intent(requireContext(), BootloaderBootAnimationActivity::class.java))
        }
        systemBootLayout.setOnClickListener {
            startActivity(Intent(requireContext(), SystemBootAnimationActivity::class.java))
        }
        systemShutdownLayout.setOnClickListener {
            startActivity(Intent(requireContext(), SystemShutdownAnimationActivity::class.java))
        }

        return layout
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val binding = (requireActivity() as MainActivity).binding
            binding.drawerLayoutMain.setTitle("Vulcan Updates", "Vulcan Updates")
            scrollToTop()
        }
    }

    private fun scrollToTop() {
        val scrollView = (requireActivity() as MainActivity).findViewById<DrawerLayout>(R.id.drawer_layout_main)
        scrollView?.scrollTo(0, 0)
    }
}


