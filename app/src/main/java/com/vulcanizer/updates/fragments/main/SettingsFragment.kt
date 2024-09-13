package com.vulcanizer.updates.fragments.main

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.R
import com.vulcanizer.updates.activities.oobe.OOBEActivity
import com.vulcanizer.updates.utils.ConfigHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference
import dev.oneuiproject.oneui.preference.internal.PreferenceRelatedCard
import dev.oneuiproject.oneui.utils.PreferenceUtils.createRelatedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    // Preferences
    private lateinit var darkModePref: HorizontalRadioPreference
    private lateinit var autoDarkModePref: SwitchPreferenceCompat
    private lateinit var updateChannelPref: DropDownPreference

    // Related card
    private var relatedCard: PreferenceRelatedCard? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    // Initialize preferences
    private fun initPreferences() {
        lifecycleScope.launch(Dispatchers.Default) {
            // Find preferences
//            darkModePref = findPreference("dark_mode_pref")!!
//            autoDarkModePref = findPreference("dark_mode_auto_pref")!!
            updateChannelPref = findPreference("update_channel")!!

            // Set listeners
//            setPreferenceChangeListener(darkModePref)
//            setPreferenceChangeListener(autoDarkModePref)
            setPreferenceChangeListener(updateChannelPref)

            // Other preferences
            val updateNotification = findPreference<SwitchPreferenceCompat>("notifications")!!
            val updateFrequency = findPreference<ListPreference>("update_frequency")!!
            val updateNetwork = findPreference<DropDownPreference>("update_over_network")!!

            // Set listeners for other preferences
            setPreferenceChangeListener(updateNotification)
            setPreferenceChangeListener(updateFrequency)
            setPreferenceChangeListener(updateNetwork)

            // Set Entries and Entry Values for DropDownPreference
            updateChannelPref.entries = arrayOf("Stable", "Beta", "Alpha")
            updateChannelPref.entryValues = updateChannelPref.entries

            // Set click listeners for special preferences
            setSpecialPreferenceClickListeners()
        }
    }

    // Set preference change listener for a preference
    private fun setPreferenceChangeListener(preference: Preference) {
        preference.onPreferenceChangeListener = this
    }

    // Set preference click listener for a preference
    private fun setPreferenceClickListener(preference: Preference, listener: () -> Unit) {
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            listener()
            true
        }
    }

    // Set click listeners for special preferences
    private fun setSpecialPreferenceClickListeners() {
        // Terms of Service preference
        findPreference<PreferenceScreen>("tos_pref")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                showTermsOfServiceDialog()
                true
            }

        // Delete app data preference
        findPreference<PreferenceScreen>("delete_app_data_pref")?.setOnPreferenceClickListener {
            showDeleteAppDataDialog()
            true
        }
    }

    // Show Terms of Service dialog
    private fun showTermsOfServiceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.tos))
            .setMessage(getString(R.string.tos_content))
            .setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setNegativeButton(R.string.reject) { dialog: DialogInterface, _: Int ->
                rejectTermsOfService()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Reject Terms of Service
    private fun rejectTermsOfService() {
        val appinfo = ConfigHandler(requireActivity(), "appinfo")
        appinfo.saveBoolean("tos", false)
        startActivity(Intent(requireActivity(), OOBEActivity::class.java))
        Toast.makeText(requireContext(), "TOS Rejected", Toast.LENGTH_SHORT).show()
    }

    // Show dialog to delete app data
    private fun showDeleteAppDataDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.delete_appdata_and_exit)
            .setMessage(R.string.delete_appdata_and_exit_warning)
            .setNegativeButton(R.string.sesl_cancel, null)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                (requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            }
            .create()
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPreferences()
        setFragmentBackgroundColor()
        val binding = (requireActivity() as MainActivity).binding
        binding.drawerLayoutMain.setTitle("Settings", "Vulcan Settings")
    }

    // Set background color of fragment
    private fun setFragmentBackgroundColor() {
        requireView().setBackgroundColor(
            resources.getColor(
                dev.oneuiproject.oneui.design.R.color.oui_background_color,
                requireActivity().theme
            )
        )
    }

    override fun onStart() {
        super.onStart()
        setRelatedCardView()
    }

    // Set related card view
    private fun setRelatedCardView() {
        if (relatedCard == null) {
            relatedCard = createRelatedCard(requireActivity())
            relatedCard?.setTitleText("Looking for help?")
            relatedCard?.addButton("Ask a question (Telegram) (Preferred).") {
                val xdaThreadUrl = "https://t.me/VulcanROM"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(xdaThreadUrl))
                startActivity(intent)
            }?.show(this)
            relatedCard?.addButton("Telegram announcement channel.") {
                val xdaThreadUrl = "https://t.me/note20updates"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(xdaThreadUrl))
                startActivity(intent)
            }?.show(this)
            relatedCard?.addButton("View our XDA thread.") {
                val xdaThreadUrl = "https://xdaforums.com/t/vulcan-rom-one-ui-6-port.4658874/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(xdaThreadUrl))
                startActivity(intent)
            }?.show(this)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateUI()
        }
    }

    // Update UI based on preferences
    private fun updateUI() {
        val binding = (requireActivity() as MainActivity).binding

        binding.drawerLayoutMain.setTitle("Settings", "Vulcan Settings")

        lifecycleScope.launch {
            val appsettings = ConfigHandler(requireActivity(), "appsettings")
            findPreference<PreferenceCategory>("dev_options")?.isVisible =
                appsettings.getBoolean("dev", false)
            autoDarkModePref.isChecked = appsettings.getBoolean("autoDarkMode", false)
            darkModePref.isEnabled = !autoDarkModePref.isChecked
            darkModePref.value = if (appsettings.getBoolean("darkMode", true)) "1" else "0"
        }
    }



    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            "dark_mode_pref" -> {
                handleDarkModePreference(newValue as String)
                return true
            }
            "dark_mode_auto_pref" -> {
                handleAutoDarkModePreference(newValue as Boolean)
                return true
            }
            "notifications" -> {
                handleNotificationsPreference(newValue as Boolean)
                return true
            }
            "update_frequency" -> {
                handleUpdateFrequencyPreference(newValue as String)
                return true
            }
            "update_over_network" -> {
                handleUpdateOverNetworkPreference(newValue as String)
                return true
            }
        }
        return false
    }

    // Handle dark mode preference change
    private fun handleDarkModePreference(value: String) {
        val darkMode = value == "1"
//        AppCompatDelegate.setDefaultNightMode(
//            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//        )

        lifecycleScope.launch {
            ConfigHandler(requireActivity(), "appsettings").saveBoolean("darkMode", darkMode)
        }
    }

    // Handle auto dark mode preference change
    private fun handleAutoDarkModePreference(value: Boolean) {
        darkModePref.isEnabled = !value
        ConfigHandler(requireActivity(), "appsettings").saveBoolean("autoDarkMode", value)
///*
//        lifecycleScope.launch {
//            if (value) {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//            } else {
//                val darkMode =
//                    ConfigHandler(requireActivity(), "appsettings").getBoolean("darkMode", true)
//                AppCompatDelegate.setDefaultNightMode(if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
//            }
//        }*/
    }

    // Handle notifications preference change
    private fun handleNotificationsPreference(enabled: Boolean) {
        ConfigHandler(requireActivity(), "appsettings").saveBoolean("notifications", enabled)
        findPreference<ListPreference>("update_frequency")?.isEnabled = enabled
    }

    // Handle update frequency preference change
    private fun handleUpdateFrequencyPreference(value: String) {
        ConfigHandler(requireActivity(), "appsettings").saveFloat("update_frequency", value.toFloat())
        findPreference<ListPreference>("update_frequency")?.value = value
    }

    // Handle update over network preference change
    private fun handleUpdateOverNetworkPreference(value: String) {
        ConfigHandler(requireActivity(), "appsettings").saveBoolean("update_over_network", value.toBoolean())
        findPreference<DropDownPreference>("update_over_network")?.value = value
    }

    // Save the current fragment state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            outState.putString("currentFragment", currentFragment::class.java.name)
        }
    }

    // Restore the current fragment state
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            val fragmentClassName = savedInstanceState.getString("currentFragment")
            if (fragmentClassName != null) {
                val fragment = requireActivity().supportFragmentManager.fragmentFactory?.instantiate(requireActivity().classLoader, fragmentClassName)
                if (fragment != null) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
            }
        }
    }
}
