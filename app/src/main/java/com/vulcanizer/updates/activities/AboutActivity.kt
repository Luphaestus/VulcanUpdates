package com.vulcanizer.updates.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.layout.AppInfoLayout
import android.widget.Toast
import kotlinx.coroutines.launch
import com.vulcanizer.updates.utils.ConfigHandler
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.vulcanizer.updates.utils.ApkInstaller
import com.vulcanizer.updates.utils.DataDownloader
import com.vulcanizer.updates.utils.XMLParser
import org.w3c.dom.Document
import com.vulcanizer.updates.R
import com.vulcanizer.updates.databinding.ActivityAboutBinding

public fun getAppVersion(context: Context): String {
    return try {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        packageInfo.longVersionCode.toString()
        "$versionName"
    } catch (e: PackageManager.NameNotFoundException) {
        "information not available"
    }
}



@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
    private var clicks = 0
    private lateinit var binding: ActivityAboutBinding
    private lateinit var AppManifest : Document
    private var updateStatus = 0

    override fun onBackPressed() {
        when (updateStatus) {
            0 -> {
                Toast.makeText(this@AboutActivity, "Checking for updates, please wait!", Toast.LENGTH_SHORT).show()
            }
            1 -> {
                super.onBackPressed()
            }
            2 -> {
                Toast.makeText(this@AboutActivity, "Please update the application!", Toast.LENGTH_SHORT).show()
            }
            3 -> {
                super.onBackPressed()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        var appsettings = ConfigHandler(this, "appsettings")

        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val model =  Build.MODEL
        binding.appInfoLayout.addOptionalText("Model $model")
        binding.appInfoLayout.setMainButtonClickListener(object : AppInfoLayout.OnClickListener {
            override fun onUpdateClicked(v: View) {
                binding.appInfoLayout.status = AppInfoLayout.LOADING
                binding.appInfoLayout.addOptionalText("Installing application...")
                val appLink = XMLParser.getElementValueByTagName(AppManifest, "downloadURL").toString()
                ApkInstaller.installUpdate(appLink, "2.0.1", this@AboutActivity)
            }


            override fun onRetryClicked(v: View) {
                checkUpdate()
            }
        })
        val version: TextView = binding.appInfoLayout.findViewById(dev.oneuiproject.oneui.design.R.id.app_info_version)
        val premium: TextView = binding.appInfoLayout.findViewById(R.id.premium)
        lifecycleScope. launch { setVersionTextView(version, appsettings.getBoolean("dev", false), premium) }
        version.setOnClickListener {
            clicks++
            if (clicks > 5) {
                clicks = 0
                lifecycleScope.launch {
                    val newDevModeEnabled = !appsettings.getBoolean("dev", false)
                    appsettings.saveBoolean("dev", newDevModeEnabled)
                    setVersionTextView(version, newDevModeEnabled, premium)
                }
            }
        }
        checkUpdate()
    }

    private fun setVersionTextView(textView: TextView, devModeEnabled: Boolean, premium: TextView) {
        lifecycleScope.launch {
            textView.text = getString(
                dev.oneuiproject.oneui.design.R.string.version_info, getAppVersion(this@AboutActivity) +  if (devModeEnabled) " (dev)" else ""
            )
            premium.text = if (devModeEnabled) getString(R.string.premium) else getString(R.string.Xda)

        }
    }


    fun checkUpdate()
    {
        binding.appInfoLayout.status = AppInfoLayout.LOADING
        binding.appInfoLayout.setNavigationButtonOnClickListener(View.OnClickListener {
            Toast.makeText(this@AboutActivity, "Checking for updates, please wait!", Toast.LENGTH_SHORT).show()
        })
//        if (ApkInstaller.isInstallationRunning())
//        {
//            binding.appInfoLayout.addOptionalText("Installing application...")
//            return
//        }
        DataDownloader.download(getString(if (ConfigHandler(this@AboutActivity, "appsettings").getBoolean("dev", false)) R.string.vulcan_updates_manifest_dev else R.string.vulcan_updates_manifest)
            , null, -1, object : DataDownloader.DownloadListener {
            override fun onProgressUpdate(progress: String) {
                binding.appInfoLayout.status = AppInfoLayout.LOADING
            }

            override fun onComplete(path: String) {
                val currentVersion = getAppVersion(this@AboutActivity)
                DataDownloader.logFileContent(path)
                    val document = XMLParser.parseXMLFile(path)


                document?.let {
                    val newestVersion = XMLParser.getElementValueByTagName(it, "version")

                    val lastDotIndex = currentVersion.lastIndexOf('.')
                    val partBeforeLastDot = currentVersion.substring(0, lastDotIndex).replace(".", "")
                    val partAfterLastDot = currentVersion.substring(lastDotIndex)
                    val intCurrentVersion = partBeforeLastDot + partAfterLastDot

                    val newestNoDots =  newestVersion.toString().replace(".", "")

                    val dotIndex = intCurrentVersion .indexOf('.')

                    val intNewestVersion = if (dotIndex >= 0 && dotIndex < newestNoDots.length) {
                        newestNoDots.substring(0, dotIndex) + "." + newestNoDots.substring(dotIndex)
                    } else {
                        newestNoDots
                    }

                    val needUpdate = intNewestVersion.toFloat() > intCurrentVersion.toFloat()

                    if (    needUpdate)
                    {
                        binding.appInfoLayout.status = AppInfoLayout.UPDATE_AVAILABLE
                        AppManifest = document
                        updateStatus = 2
                        binding.appInfoLayout.setNavigationButtonOnClickListener(View.OnClickListener {
                            Toast.makeText(this@AboutActivity, "Please update the application!", Toast.LENGTH_SHORT).show()
                        })
                    }
                    else
                    {
                        updateStatus = 3
                        binding.appInfoLayout.status = AppInfoLayout.NO_UPDATE
                        binding.appInfoLayout.setNavigationButtonOnClickListener(View.OnClickListener {
                            onBackPressed()
                        })                    }
                } ?: run {
                    Log.e("XML GET TAG","Failed to parse the XML file.")
                    binding.appInfoLayout.status = AppInfoLayout.NO_CONNECTION
                }
                DataDownloader.deleteFile(path)
            }

            override fun onError(errorMessage: String) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.e("FAILED", errorMessage)
                    updateStatus = 1
                    binding.appInfoLayout.setNavigationButtonOnClickListener(View.OnClickListener {
                        onBackPressed()
                    })
                    binding.appInfoLayout.status = AppInfoLayout.NO_CONNECTION
                }, 1000)
            }
        })
    }


    @Suppress("unused_parameter")
    fun openTelegram(v: View?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(getString(R.string.link_telegram))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No suitable activity found", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("unused_parameter")
    fun openPremium(v: View?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            var appsettings = ConfigHandler(this, "appsettings")
            if (appsettings.getBoolean("dev", false))
            {
                intent.data = Uri.parse(getString(R.string.link_telegram_premium))
            }
            else
            {
                intent.data = Uri.parse(getString(R.string.link_xda))
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No suitable activity found", Toast.LENGTH_SHORT).show()
        }
    }
}