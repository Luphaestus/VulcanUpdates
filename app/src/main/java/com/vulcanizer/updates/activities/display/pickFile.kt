import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FilePicker(
    private val activity: AppCompatActivity,
    private val onFileSelected: (uri: Uri, fileName: String?, filePath : String) -> Unit // Updated lambda to handle file selection
) {

    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var allowedExtension : String? = null


    init {
        // Initialize the file picker launcher
        filePickerLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    val fileName = getFileName(it)
                    Log.e("file picker", allowedExtension!!)
                    if (allowedExtension != null && !FileTypeChecker.isFileTypeMatching(fileName!!, allowedExtension!!)) {
                        val actualFileType = File(fileName).extension
                        showFileTypeErrorDialog(activity, allowedExtension!!)
                    }
                    else
                    {
                        copyFileToTempDirectory(uri) { filePath ->
                            onFileSelected(it, fileName, filePath!!)
                        }
                    }
                }
            }
        }
    }

    fun pickFile(allowedFileTypes: String? = null, allowedExtension : String? = null) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = allowedFileTypes ?: "*/*" // Default to all file types
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        this.allowedExtension = allowedExtension
        filePickerLauncher.launch(intent)
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = activity.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    // New function to copy the selected file to a temporary directory
    private fun copyFileToTempDirectory(uri: Uri, onFileCopied: (filePath: String?) -> Unit) {
    val tempFile = File(activity.cacheDir, getFileName(uri) ?: "temp_file")
        activity.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        onFileCopied(tempFile.absolutePath) // Call the lambda with the file path
    }

    fun showFileTypeErrorDialog(context: Context, expectedFileType: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("File Type Mismatch: $expectedFileType required")
            .setIcon(android.R.drawable.ic_dialog_alert) // Set an alert icon
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setCancelable(false) // Prevents dismissal by tapping outside
            .show()
    }
}

object FileTypeChecker {
    @JvmStatic
    fun isFileTypeMatching(filePath: String, expectedFileType: String): Boolean {
        val file = File(filePath)

        // Get the file extension
        val fileExtension = file.extension
        // Compare the file extension with the expected file type
        return fileExtension.equals(expectedFileType, ignoreCase = true)
    }

}


object CreateFlashAbleZip {
    fun createTree(destinationFolderPath: String, assetsFolder: String, assets:AssetManager) {
        val destinationFolder = File(destinationFolderPath)

        // Check if the folder exists and delete it if it does
        if (destinationFolder.exists()) {
            destinationFolder.deleteRecursively()
        }

        // Create the destination folder
        if (destinationFolder.mkdirs()) {
            println("Created folder: ${destinationFolder.absolutePath}")
        } else {
            println("Failed to create folder: ${destinationFolder.absolutePath}")
        }

        // Create the subdirectory structure
        val metaInfFolder = File(destinationFolder, "META-INF/com/google/android")
        if (metaInfFolder.mkdirs()) {
            println("Created subdirectory: ${metaInfFolder.absolutePath}")
        } else {
            println("Failed to create subdirectory: ${metaInfFolder.absolutePath}")
        }

        // Copy files from assets
        copyFileFromAssets(assetsFolder, "update-binary", metaInfFolder, assets)
        copyFileFromAssets(assetsFolder, "updater-script", metaInfFolder, assets)
    }

    private fun copyFileFromAssets(assetsFolder: String, fileName: String, destinationFolder: File, assets : AssetManager) {
        val assetFilePath = "$assetsFolder/$fileName"
        val outputFile = File(destinationFolder, fileName)

        try {
            // Use AssetManager to open the asset file
            assets.open(assetFilePath).use { inputStream : InputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("Copied $fileName to ${outputFile.absolutePath}")
        } catch (e: Exception) {
            println("Failed to copy $fileName: ${e.message}")
        }
    }

    fun zipFolder(sourceFolder: String, zipFilePath: String) {
        val zipFile = File(zipFilePath)
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            val folderToZip = File(sourceFolder)
            folderToZip.walkTopDown().forEach { file ->
                val zipEntry = ZipEntry(file.relativeTo(folderToZip).path + if (file.isDirectory) "/" else "")
                zipOut.putNextEntry(zipEntry)
                if (file.isFile) {
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zipOut)
                    }
                }
                zipOut.closeEntry()
            }
        }
    }
}
