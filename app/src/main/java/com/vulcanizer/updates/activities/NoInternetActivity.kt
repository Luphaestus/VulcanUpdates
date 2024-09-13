package com.vulcanizer.updates.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.vulcanizer.updates.R
import com.vulcanizer.updates.utils.ConfigHandler
import com.vulcanizer.updates.utils.DataDownloader
import com.vulcanizer.updates.utils.XMLParser


@AndroidEntryPoint
class NoInternetActivity : AppCompatActivity() {
    var exit : Boolean = false
    override fun onBackPressed() {
        if (!exit)
            Toast.makeText(this, "No networks available.", Toast.LENGTH_SHORT).show()
        else
        {
            exit = true
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)

        // Find the button by its ID
        val tryAgainButton: Button = findViewById(R.id.button_try_again)

        // Set an OnClickListener for the button
        tryAgainButton.setOnClickListener {
            // Show a toast message when the button is clicked
            DataDownloader.download(getString(if (ConfigHandler(this, "appsettings").getBoolean("dev", false)) R.string.vulcan_updates_manifest_dev else R.string.vulcan_updates_manifest)
                , null, -1, object : DataDownloader.DownloadListener {
                override fun onProgressUpdate(progress: String) {}

                override fun onComplete(path: String) {
                    exit = true
                    onBackPressed()
                }

                override fun onError(errorMessage: String) {
                    Log.e("process error", errorMessage)
                    Toast.makeText(this@NoInternetActivity, "No networks available.", Toast.LENGTH_SHORT).show()
                }
            })


        }
    }
}
