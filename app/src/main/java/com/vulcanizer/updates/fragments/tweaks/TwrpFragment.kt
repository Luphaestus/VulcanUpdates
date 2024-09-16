package com.vulcanizer.updates.fragments.tweaks
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.ConfigHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class TwrpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tweak, container, false)
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
        val adjustededpath = tempPath.replace("user/0/","")
        runShellCommand("echo  install  /data$adjustededpath/$fileName >> /cache/recovery/openrecoveryscript",
            onSuccess = {
                GlobalScope.launch(Dispatchers.Main) {
                    runShellCommand("echo cmd rm -rf /data$adjustededpath/$fileName >> /cache/recovery/openrecoveryscript\n",
                        onSuccess = {
                            GlobalScope.launch(Dispatchers.Main) {
                                showRebootDialog(true, "recovery", this@TwrpFragment.requireContext())
                                setStatus(tweakContainer, packageName)
                                callback(true) // Call the callback with success
                            }
                        },
                        onFailure = { errorCode, errorMessage ->
                            callback(false) // Call the callback with failure
                        }
                    )
                }
            },
            onFailure = { errorCode, errorMessage ->
                callback(false) // Call the callback with failure
            }
        )
    }

    private fun setStatus(container: LinearLayout, name: String) {
        //TODO
        Log.e("Twrp", "TODO")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tweakContainer = view.requireViewById<LinearLayout>(R.id.tweakcontianer)

        val url : String =   getString(if (ConfigHandler(requireContext(), "appsettings").getBoolean("dev", false)) R.string.twrp_manifest_dev else R.string.twrp_manifest)

        setupTweaks(url, tweakContainer, this@TwrpFragment.requireContext(), this@TwrpFragment.requireActivity(), ::handleDownloadCompletion, this,::setStatus)

    }
}