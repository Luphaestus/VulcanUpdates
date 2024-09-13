package com.vulcanizer.updates.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.databinding.FragmentInformationBinding
import com.vulcanizer.updates.fragments.info.DeviceInfoFragment
import com.vulcanizer.updates.fragments.info.HardwareInfoFragment
import com.vulcanizer.updates.fragments.info.SoftwareInfoFragment


class InfoFragment : Fragment() {
    private lateinit var binding: FragmentInformationBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragmentDesignSubTabs.seslSetSubTabStyle()
        binding.fragmentDesignSubTabs.tabMode = TabLayout.SESL_MODE_WEIGHT_AUTO
        binding.viewPager2Design.adapter = ViewPager2AdapterTabDesignSubtabs(this)
        binding.viewPager2Design.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
        TabLayoutMediator(binding.fragmentDesignSubTabs, binding.viewPager2Design) { tab, position ->
            tab.text = arrayOf("System", "Device", "Hardware")[position]
        }.attach()

        val binding = (requireActivity() as MainActivity).binding
        binding.drawerLayoutMain.setTitle("Device Information", "Device Information")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val binding = (requireActivity() as MainActivity).binding
            binding.drawerLayoutMain.setTitle("Device Information", "Device Information")
        }
    }

}


class ViewPager2AdapterTabDesignSubtabs(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SoftwareInfoFragment()
        1 -> DeviceInfoFragment()
        2 -> HardwareInfoFragment()
        else -> DeviceInfoFragment()
    }
}