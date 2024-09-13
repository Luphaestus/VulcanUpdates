package com.vulcanizer.updates.activities

import CreateFlashAbleZip
import FilePicker
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vulcanizer.updates.R
import com.vulcanizer.updates.databinding.ActivitySystemBootAnimationBinding
import java.io.File
import java.io.FileInputStream

class SystemBootAnimationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySystemBootAnimationBinding
    private lateinit var  destinationFolderPath : String


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Write the zipped file to the selected location
                val inputStream = FileInputStream(File("${filesDir.absolutePath}/zipped_folder.zip"))
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemBootAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarLayout.setNavigationButtonOnClickListener { finish() }

        destinationFolderPath = "${cacheDir.absolutePath}/systembootanim"
        CreateFlashAbleZip.createTree(destinationFolderPath, "qmg", assets)

        val bootSamsungPicker = FilePicker(this) { uri, fileName, filePath ->
            Log.e("FilePicker", "Selected file: $fileName, PATH: $filePath, URI: $uri")

            val selectedFile = File(filePath)
            val destinationFile = File(destinationFolderPath, "bootsamsung.qmg")
            selectedFile.copyTo(destinationFile, overwrite = true)

            binding.filenameBootsamsung.text="Selected File: $fileName"
            binding.filenameBootsamsung.visibility = View.VISIBLE
            binding.buttonUploadBootsamsung.text = "Remove"

            binding.buttonApply.isEnabled = true
            val color = ContextCompat.getColor(this, R.color.primary_color)
            binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
        }

        binding.buttonUploadBootsamsung.setOnClickListener {
            if (binding.buttonUploadBootsamsung.text == "Remove")
            {
                val file = File(destinationFolderPath, "bootsamsung.qmg")
                file.delete()
                binding.filenameBootsamsung.visibility = View.GONE
                binding.buttonUploadBootsamsung.text = "Chose File"

                if (binding.buttonUploadBootsamsungloop.text != "Remove" && binding.buttonUploadShutdownsamsung.text != "Remove"){
                    binding.buttonApply.isEnabled = false
                    val color = ContextCompat.getColor(this, R.color.secondary_text_icon_color)
                    binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
                }
            }
            else
                bootSamsungPicker.pickFile("*/*", "qmg") // Call pickFile without a lambda
        }

        val bootSamsungLoopPicker = FilePicker(this) { uri, fileName, filePath ->
            Log.e("FilePicker", "Selected file: $fileName, PATH: $filePath, URI: $uri")

            val selectedFile = File(filePath)
            val destinationFile = File(destinationFolderPath, "bootsamsungloop.qmg")
            selectedFile.copyTo(destinationFile, overwrite = true)

            binding.filenameBootsamsungloop.text="Selected File: $fileName"
            binding.filenameBootsamsungloop.visibility = View.VISIBLE
            binding.buttonUploadBootsamsungloop.text = "Remove"

            binding.buttonApply.isEnabled = true
            val color = ContextCompat.getColor(this, R.color.primary_color)
            binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
        }

        binding.buttonUploadBootsamsungloop.setOnClickListener {
            if (binding.buttonUploadBootsamsungloop.text == "Remove")
            {
                val file = File(destinationFolderPath, "bootsamsungloop.qmg")
                file.delete()
                binding.filenameBootsamsungloop.visibility = View.GONE
                binding.buttonUploadBootsamsungloop.text = "Chose File"

                if (binding.buttonUploadBootsamsung.text != "Remove" && binding.buttonUploadShutdownsamsung.text != "Remove") {
                    binding.buttonApply.isEnabled = false
                    val color = ContextCompat.getColor(this, R.color.secondary_text_icon_color)
                    binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
                }
            }
            else
                bootSamsungLoopPicker.pickFile("*/*", "qmg") // Call pickFile without a lambda
        }

        val shutdownSamsungPicker = FilePicker(this) { uri, fileName, filePath ->
            Log.e("FilePicker", "Selected file: $fileName, PATH: $filePath, URI: $uri")

            val selectedFile = File(filePath)
            val destinationFile = File(destinationFolderPath, "shutdownsamsung.qmg")
            selectedFile.copyTo(destinationFile, overwrite = true)

            binding.filenameShutdownsamsung.text="Selected File: $fileName"
            binding.filenameShutdownsamsung.visibility = View.VISIBLE
            binding.buttonUploadShutdownsamsung.text = "Remove"

            binding.buttonApply.isEnabled = true
            val color = ContextCompat.getColor(this, R.color.primary_color)
            binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
        }

        binding.buttonUploadShutdownsamsung.setOnClickListener {
            if (binding.buttonUploadShutdownsamsung.text == "Remove")
            {
                val file = File(destinationFolderPath, "shutdownsamsung.qmg")
                file.delete()
                binding.filenameShutdownsamsung.visibility = View.GONE
                binding.buttonUploadShutdownsamsung.text = "Chose File"

                if (binding.buttonUploadBootsamsungloop.text != "Remove" && binding.buttonUploadBootsamsung.text != "Remove"){
                    binding.buttonApply.isEnabled = false
                    val color = ContextCompat.getColor(this, R.color.secondary_text_icon_color)
                    binding.buttonApply.backgroundTintList = ColorStateList.valueOf(color)
                }
            }
            else
                shutdownSamsungPicker.pickFile("*/*", "qmg") // Call pickFile without a lambda
        }

        binding.buttonApply.setOnClickListener {
            Log.e("Button Apply", "Clicked")
//            CreateFlashAbleZip.zipFolder(destinationFolderPath, "${filesDir.absolutePath}/zipped_folder.zip")
        }


    }

}
