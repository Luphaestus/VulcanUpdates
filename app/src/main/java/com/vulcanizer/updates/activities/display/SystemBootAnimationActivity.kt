package com.vulcanizer.updates.activities

import CreateFlashAbleZip
import FilePicker
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vulcanizer.updates.R
import com.vulcanizer.updates.databinding.ActivitySystemBootAnimationBinding
import java.io.File

class SystemBootAnimationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySystemBootAnimationBinding
    private lateinit var destinationFolderPath: String
    private val fileName: String = "systembootanim"
    private val sourceDir : String = "qmg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemBootAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }

        destinationFolderPath = "${cacheDir.absolutePath}/$fileName"
        CreateFlashAbleZip.createTree(destinationFolderPath, sourceDir, assets)
        File(destinationFolderPath, sourceDir).apply { mkdir() }

        initFilePicker(binding.buttonUploadBootsamsung, "bootsamsung.qmg", binding.filenameBootsamsung)
        initFilePicker(binding.buttonUploadBootsamsungloop, "bootsamsungloop.qmg", binding.filenameBootsamsungloop)
        initFilePicker(binding.buttonUploadShutdownsamsung, "shutdown.qmg", binding.filenameShutdownsamsung)

        binding.buttonApply.setOnClickListener {
            Log.e("Button Apply", "Clicked")
            CreateFlashAbleZip.zipFolder(destinationFolderPath, "${filesDir.absolutePath}/$fileName.zip")
            CreateFlashAbleZip.flashZip("${filesDir.absolutePath}/$fileName.zip", this)
        }
    }

    private fun initFilePicker(button: Button, fileName: String, filenameTextView: TextView) {
        val filePicker = FilePicker(this) { uri, selectedFileName, filePath ->
            Log.e("FilePicker", "Selected file: $selectedFileName, PATH: $filePath, URI: $uri")
            handleFileSelection(filePath, fileName, selectedFileName!!, filenameTextView, button)
        }

        button.setOnClickListener {
            if (button.text == "Remove") {
                handleFileRemoval(fileName, filenameTextView, button)
            } else {
                filePicker.pickFile("*/*", sourceDir)
            }
        }
    }

    private fun handleFileSelection(filePath: String, fileName: String, selectedFileName: String, filenameTextView: TextView, button: Button) {
        val selectedFile = File(filePath)
        val destinationFile = File("${destinationFolderPath}/$sourceDir", fileName)
        selectedFile.copyTo(destinationFile, overwrite = true)

        filenameTextView.text = getString(R.string.selected_file, selectedFileName)
        filenameTextView.visibility = View.VISIBLE
        button.text = getString(R.string.remove_file)

        updateApplyButtonState(true)
    }

    private fun handleFileRemoval(fileName: String, filenameTextView: View, button: Button) {
        val file = File("${destinationFolderPath}/$sourceDir", fileName)
        file.delete()
        filenameTextView.visibility = View.GONE
        button.text = getString(R.string.choose_file)

        if (areAllFilesRemoved()) {
            updateApplyButtonState(false)
        }
    }

    private fun updateApplyButtonState(isEnabled: Boolean) {
        binding.buttonApply.isEnabled = isEnabled
        val color = if (isEnabled) {
            ContextCompat.getColor(this, R.color.primary_color)
        } else {
            ContextCompat.getColor(this, R.color.secondary_text_icon_color)
        }
        binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun areAllFilesRemoved(): Boolean {
        return binding.buttonUploadBootsamsung.text != "Remove" &&
                binding.buttonUploadBootsamsungloop.text != "Remove" &&
                binding.buttonUploadShutdownsamsung.text != "Remove"
    }
}
