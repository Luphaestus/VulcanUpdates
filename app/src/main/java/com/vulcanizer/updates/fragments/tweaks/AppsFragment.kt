package com.vulcanizer.updates.fragments.tweaks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import com.ketch.Ketch
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.ConfigHandler
import com.vulcanizer.updates.utils.isAppInstalled
import com.vulcanizer.updates.utils.openApp

class AppsFragment : Fragment() {
    private lateinit var ketch: Ketch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tweak, container, false)
    }

    private fun setStatus(container: LinearLayout, name: String) {
        if (!isAdded) return
        if (isAppInstalled( this@AppsFragment.requireContext(), name)) {
            container.findViewById<AppCompatImageButton>(R.id.download).visibility = View.GONE
            container.findViewById<AppCompatImageButton>(R.id.play).visibility = View.GONE
            val openButton = container.findViewById<AppCompatImageButton>(R.id.open)
            openButton.visibility = View.VISIBLE
            openButton.setOnClickListener {
                openApp(name, requireContext())
            }
        }
    }

    private fun handleDownloadCompletion(
        tempPath: String,
        fileName: String,
        textView: TextView,
        linearLayout: LinearLayout,
        packageName: String,
        tweakContainer: LinearLayout,
        context: Context,
        previous: String,
        callback: (Boolean) -> Unit // Add a callback parameter
    ) {
        runShellCommandForResult("pm install $tempPath/$fileName", onSuccess =
        {


        }, onFailure = {

        })
        linearLayout.findViewById<AppCompatImageButton>(R.id.download).visibility = View.GONE
        val openButton = linearLayout.findViewById<AppCompatImageButton>(R.id.open)
        openButton.visibility = View.VISIBLE
        openButton.setOnClickListener {
            openApp(packageName, context)
        }
        callback(true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tweakContainer = view.requireViewById<LinearLayout>(R.id.tweakcontianer)
        val url : String = getString(if (ConfigHandler(this@AppsFragment.requireActivity(), "appsettings").getBoolean("dev", false)) R.string.app_manifest_dev else R.string.app_manifest)
        setupTweaks(url, tweakContainer, this@AppsFragment.requireContext(), this@AppsFragment.requireActivity(), ::handleDownloadCompletion, this,::setStatus)


    }
}
