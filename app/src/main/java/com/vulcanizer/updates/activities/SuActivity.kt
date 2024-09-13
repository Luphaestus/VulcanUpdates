package com.vulcanizer.updates.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.DataDownloader
import com.vulcanizer.updates.utils.XMLParser


@AndroidEntryPoint
class SuActivity : AppCompatActivity() {
    var exit: Boolean = false
    override fun onBackPressed() {
        if (!exit)
            Toast.makeText(this, "Root not available.", Toast.LENGTH_SHORT).show()
        else {
            exit = true
            super.onBackPressed()
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_su)



        // Find the button by its ID
        val tryAgainButton: Button = findViewById(R.id.button_try_again)

        // Set an OnClickListener for the button
        tryAgainButton.setOnClickListener {
            if (checkSuPermissions()) {
                exit = true
                onBackPressed()
            } else {
                Toast.makeText(this@SuActivity, "Root not available.", Toast.LENGTH_SHORT).show()
            }
        }


    }
}

