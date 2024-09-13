package com.vulcanizer.updates.activities.oobe

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vulcanizer.updates.MainActivity
import com.vulcanizer.updates.R
import com.vulcanizer.updates.databinding.ActivityOobeBinding
import com.vulcanizer.updates.utils.CheckAppStartUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class OOBEActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOobeBinding
    private lateinit var toSDialog: AlertDialog

    private var time: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding = ActivityOobeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initTipsItems()
        initToSView()
        initFooterButton()
//        if (!checkSuPermissions()) {
//            val intent = Intent(this, SuActivity::class.java)
//            startActivity(intent)
//        }

        }
    override fun onBackPressed() {
        if (System.currentTimeMillis() - time < 3000) {
            finishAffinity()
        } else {
            Toast.makeText(this@OOBEActivity, "Press again to exit", Toast.LENGTH_SHORT).show()
            time = System.currentTimeMillis()
        }
    }
    private fun initTipsItems() {
        val defaultLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val titles = arrayOf(R.string.oobe_onboard_msg1_title, R.string.oobe_onboard_msg2_title, R.string.oobe_onboard_msg3_title)
        val summaries = arrayOf(R.string.oobe_onboard_msg1_summary, R.string.oobe_onboard_msg2_summary, R.string.oobe_onboard_msg3_summary)
        val icons = arrayOf(
            dev.oneuiproject.oneui.R.drawable.ic_oui_palette,
            R.drawable.vulcan_robot,
            dev.oneuiproject.oneui.R.drawable.ic_oui_credit_card_outline,
        )
        for (i in titles.indices) {
            val item = TipsItemView(this)
            item.setIcon(icons[i])
            item.setTitleText(getString(titles[i]))
            item.setSummaryText(getString(summaries[i]))
            binding.oobeIntroTipsContainer.addView(item, defaultLp)
        }
    }

    private fun initToSView() {
        val tos = getString(R.string.tos)
        val tosText = getString(R.string.oobe_tos_text, tos)
        val tosLink = SpannableString(tosText)
        tosLink.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    toSDialog.show()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
            },
            tosText.indexOf(tos), tosText.length - if (Locale.getDefault().language == "de") 4 else 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.oobeIntroFooterTosText.text = tosLink
        binding.oobeIntroFooterTosText.movementMethod = LinkMovementMethod.getInstance()
        binding.oobeIntroFooterTosText.highlightColor = Color.TRANSPARENT
        initToSDialog()
    }

    private fun initToSDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.tos))
        builder.setMessage(getString(R.string.tos_content))
        builder.setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        toSDialog = builder.create()
    }

    private fun initFooterButton() {
        if (resources.configuration.screenWidthDp < 360) {
            binding.oobeIntroFooterButton.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.oobeIntroFooterButton.setOnClickListener {
            binding.oobeIntroFooterTosText.isEnabled = false
            binding.oobeIntroFooterButton.visibility = View.GONE
            binding.oobeIntroFooterButtonProgress.visibility = View.VISIBLE
            val context = this
            lifecycleScope.launch {
                CheckAppStartUseCase.setTOS(context)
                startActivity(Intent(this@OOBEActivity, MainActivity::class.java))
                if (Build.VERSION.SDK_INT < 34) {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                finish()



            }
        }
    }

     fun checkSuPermissions(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo"))
            val exitValue = process.waitFor()
            exitValue == 0
        } catch (e: Exception) {
            false
        }
    }
}