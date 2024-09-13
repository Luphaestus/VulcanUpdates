package com.vulcanizer.updates

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vulcanizer.updates.activities.AboutActivity
import com.vulcanizer.updates.activities.NoInternetActivity
import com.vulcanizer.updates.activities.SuActivity
import com.vulcanizer.updates.activities.getAppVersion
import com.vulcanizer.updates.activities.oobe.OOBEActivity
import com.vulcanizer.updates.databinding.ActivityMainBinding
import com.vulcanizer.updates.fragments.main.TweakFragment
import com.vulcanizer.updates.utils.AppStart
import com.vulcanizer.updates.utils.CheckAppStartUseCase
import com.vulcanizer.updates.utils.ConfigHandler
import com.vulcanizer.updates.utils.DataDownloader
import com.vulcanizer.updates.utils.TelegramBot
import com.vulcanizer.updates.utils.XMLParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    var Mstate : Boolean = true


    private val menuItems = arrayOf(
        mutableMapOf(
            "drawerID" to R.id.draweritem_tweaks,
            "fragmentID" to "com.vulcanizer.updates.fragments.main.TweakFragment"
        ),
        mutableMapOf(
            "drawerID" to R.id.draweritem_display,
            "fragmentID" to "com.vulcanizer.updates.fragments.main.DisplayFragment"
        ),
        mutableMapOf(
            "drawerID" to R.id.draweritem_info,
            "fragmentID" to "com.vulcanizer.updates.fragments.main.InfoFragment"
        ),
        mutableMapOf(
            "drawerID" to R.id.draweritem_begreport,
            "fragmentID" to "com.vulcanizer.updates.fragments.main.BugReportFragment"
        ),
        mutableMapOf(
            "drawerID" to R.id.draweritem_settings,
            "fragmentID" to "com.vulcanizer.updates.fragments.main.SettingsFragment"
        )
    )

    lateinit var binding: ActivityMainBinding
    private val fragments = mutableMapOf<String, Fragment>()

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()

        val appInfo = ConfigHandler(this, "appinfo")
        if (appInfo.getBoolean("first-start${getAppVersion(this)}", true)) {
            TelegramBot().newperson("${getAppVersion(this)} - ${Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)}")
            appInfo.saveBoolean("first-start${getAppVersion(this)}", false)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        lifecycleScope.launch {
            when (CheckAppStartUseCase.get(this@MainActivity)) {
                AppStart.TOS -> openOOBE()
                AppStart.NORMAL, AppStart.FIRST_TIME_VERSION -> openMain()
            }
        }

       requestPermissionsIfNeeded()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        for (i in 0 until menu!!.size()) menu.getItem(i).setVisible(Mstate)

        return true
    }

    public fun hide_menu(state:Boolean)
    {
        Mstate = state
        invalidateOptionsMenu()

    }

    fun restartMainActivity() {
        // Create an intent to restart the main activity
        val intent = Intent(this, MainActivity::class.java) // Replace with your main activity class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Optional: Call finish() if you want to close the current activity

        // Override the default transition animations
        overridePendingTransition(0, 0) // No animation
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.main_menu)
        {
            restartMainActivity()
            return true
        }
        return false
    }

    private fun checkSuPermissions(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo"))
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Hide the menu items or the entire menu
        menu?.setGroupVisible(R.id.main_menu, false) // Hide a specific group of menu items
        return true
    }

    private fun requestPermissionsIfNeeded() {


        val permissions = mutableListOf<String>()
        if (!getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 101)
            Toast.makeText(this, "Notification Required", Toast.LENGTH_SHORT).show()
        }

        DataDownloader.download(
            getString(if (ConfigHandler(this, "appsettings").getBoolean("dev", false)) R.string.vulcan_updates_manifest_dev else R.string.vulcan_updates_manifest),
            null, -1, object : DataDownloader.DownloadListener {
                override fun onProgressUpdate(progress: String) {
                }

                override fun onComplete(path: String) {
                    val currentVersion = getAppVersion(this@MainActivity)
                    val document = XMLParser.parseXMLFile(path)
                    document?.let {
                        val newestVersion = XMLParser.getElementValueByTagName(it, "version")
                        val currentVersionNumeric = currentVersion.replace(".", "").toIntOrNull() ?: 0
                        val newestVersionNumeric = newestVersion?.replace(".", "")?.toIntOrNull() ?: 0

                        if (newestVersionNumeric > currentVersionNumeric) {
                            Toast.makeText(applicationContext, "Please update the application.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                        }
                    } ?: Log.e("XML GET TAG", "Failed to parse the XML file.")

                    DataDownloader.deleteFile(path)
                }

                override fun onError(errorMessage: String) {
                    Log.e("FAILED", errorMessage)
                    startActivity(Intent(this@MainActivity, NoInternetActivity::class.java))
                }
            }
        )

        if (BuildConfig.BUILD_TYPE == "release") {
            if (!checkSuPermissions()) {
                startActivity(Intent(this, SuActivity::class.java))
            }
        }

    }

    private fun init() {
        val appSettings = ConfigHandler(this, "appsettings")
        val autoDarkMode = appSettings.getBoolean("autoDarkMode", false)
        val darkMode = appSettings.getBoolean("darkMode", true)

        AppCompatDelegate.setDefaultNightMode(
            when {
                autoDarkMode -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                darkMode -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun openOOBE() {
        startActivity(Intent(this, OOBEActivity::class.java))
        if (Build.VERSION.SDK_INT < 34) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        finish()
    }

    private fun openMain() {
        initDrawer()
        val tweakFragment = TweakFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, tweakFragment)
            .commit()
        fragments["com.vulcanizer.updates.fragments.main.TweakFragment"] = tweakFragment // Store the fragment
    }

    @Suppress("DEPRECATION")
    private fun initDrawer() {
        binding.drawerLayoutMain.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline))
        binding.drawerLayoutMain.setDrawerButtonOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        menuItems.forEach { item ->
            findViewById<LinearLayout>(item["drawerID"] as Int).setOnClickListener {
                lifecycleScope.launch {
                    val currentFragment = ConfigHandler(this@MainActivity, "currentFragment")
                    currentFragment.saveString("fragment", item["fragmentID"].toString())

                    binding.drawerLayoutMain.setDrawerOpen(false, true)
                    val fragmentClassName = item["fragmentID"] as? String

                    // Check if the fragment is already created
                    if (fragmentClassName != null) {
                        val fragment = fragments[fragmentClassName] ?: Class.forName(fragmentClassName).newInstance() as Fragment
                        fragments[fragmentClassName] = fragment // Store the fragment

                        // Show the fragment if it's already added, otherwise add it
                        if (!fragment.isAdded) {
                            supportFragmentManager.beginTransaction()
                                .add(R.id.fragment_container, fragment)
                                .commit()
                        }

                        // Hide all other fragments
                        fragments.values.forEach { f ->
                            if (f != fragment) {
                                supportFragmentManager.beginTransaction().hide(f).commit()
                            }
                        }

                        // Show the selected fragment
                        supportFragmentManager.beginTransaction().show(fragment).commit()
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Confirmation")
            .setMessage("Are you sure you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                if ((0..6).random() == 0) {
                    showSecondaryExitConfirmationDialog()
                } else {
                    exitProcess(0)
                }
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showSecondaryExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Confirmation")
            .setMessage("Are you sure really you want to exit the app?")
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Just joking", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 3000)
            }
            .create()
            .show()
    }
}
