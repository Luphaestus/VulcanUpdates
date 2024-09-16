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
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.ConfigHandler
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SUFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tweak, container, false)
    }


    private fun setStatus(container: LinearLayout, name: String) {
        if (!isAdded) {
            return
        }
        getModuleList(
            onSuccess = { moduleMap ->
                if (moduleMap.containsKey(name)) {
                    val deleteButton = container.findViewById<AppCompatImageButton>(R.id.delete_button)
                    deleteButton.visibility = View.VISIBLE
                    container.findViewById<AppCompatImageButton>(R.id.download).visibility = View.GONE
                    container.findViewById<AppCompatImageButton>(R.id.play).visibility = View.GONE


                    deleteButton.setOnClickListener {
                        val id = moduleMap[name]!!.get("id")
                        val command = "ksud module uninstall $id"
                        runShellCommand(command,
                            onSuccess = {
                                GlobalScope.launch(Dispatchers.Main) {
                                    showRebootDialog(false, "", this@SUFragment.requireContext())
                                    deleteButton.visibility = View.GONE
                                    container.findViewById<AppCompatImageButton>(R.id.download).visibility = View.VISIBLE
                                }
                            },
                            onFailure = { errorMessage, errorCode ->
                                GlobalScope.launch(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), getString(R.string.module_uninstallation_failed, errorMessage, errorCode), Toast.LENGTH_SHORT).show()                                }
                            }
                        )
                    }
                }
            },
            onFailure = {
                Toast.makeText(requireContext(), getString(R.string.failed_to_retrieve_module_list), Toast.LENGTH_SHORT).show()
            }
        )
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
        val command = "ksud module install $tempPath/$fileName"
        textView.text = getString(R.string.installing)
        runShellCommand(command,
            onSuccess = {
                GlobalScope.launch(Dispatchers.Main) {
                    showRebootDialog(true, "", context)
                    callback(true)
                }
            },
            onFailure = { errorMessage, errorCode ->
                callback(false)
            }
        )
        callback(true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tweakContainer = view.requireViewById<LinearLayout>(R.id.tweakcontianer)

        val url : String = getString(if (ConfigHandler(this@SUFragment.requireActivity(), "appsettings").getBoolean("dev", false)) R.string.module_manifest_dev else R.string.module_manifest)
        setupTweaks(url, tweakContainer, this@SUFragment.requireContext(), this@SUFragment.requireActivity(), ::handleDownloadCompletion, this, ::setStatus)

        }

     fun getModuleList(onSuccess: (Map<String, Map<String, Any>>) -> Unit, onFailure: () -> Unit) {
        runShellCommandForResult("ksud module list", onSuccess, onFailure)
    }
}
