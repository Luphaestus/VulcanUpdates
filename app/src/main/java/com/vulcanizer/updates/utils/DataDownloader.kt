package com.vulcanizer.updates.utils

import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory


fun getFileNameFromUrl(url: String): String? {
    val uri = Uri.parse(url)
    return uri.lastPathSegment // This will return the last segment of the path, which is the filename
}

class DataDownloader {

    interface DownloadListener {
        fun onProgressUpdate(progress: String)
        fun onComplete(path: String)
        fun onError(errorMessage: String)
    }

    companion object {
        fun download(url: String, outputPath: String? = null, fileSize: Long = -1, listener: DownloadListener) {
            DownloadTask(listener, fileSize).execute(url, outputPath)
        }

        fun logFileContent(filePath: String) {
            try {
                val file = File(filePath)
                val fileContent = file.readText()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FileContent", "Failed to read the file content.")
            }
        }

        fun deleteFile(filePath: String): Boolean {
            return try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    Log.e("DeleteFile", "File not found: $filePath")
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DeleteFile", "Failed to delete the file.")
                false
            }
        }

        fun calculateMD5(filePath: String): String? {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return null // File doesn't exist or is not a regular file
            }

            val digest = MessageDigest.getInstance("MD5")
            val inputStream = FileInputStream(file)
            val digestInputStream = DigestInputStream(inputStream, digest)

            try {
                // Read file data and update the message digest
                val buffer = ByteArray(8192)
                while (digestInputStream.read(buffer) != -1) {
                    // Update digest
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null // Error occurred while calculating MD5
            } finally {
                digestInputStream.close()
                inputStream.close()
            }

            // Compute the MD5 checksum
            val md5Bytes = digest.digest()
            val md5Hex = StringBuilder()
            for (md5Byte in md5Bytes) {
                // Convert byte to hex string
                md5Hex.append(String.format("%02x", md5Byte.toInt() and 0xFF))
            }
            return md5Hex.toString()
        }
    }

    private class DownloadTask(private val listener: DownloadListener, private val fileSize: Long) : AsyncTask<String, String, String>() {

        override fun doInBackground(vararg params: String): String? {
            val urlString = params[0]
            val outputPath = params[1]
            var inputStream: BufferedInputStream? = null
            var outputStream: FileOutputStream? = null
            var tempFile: String? = null
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = if (fileSize != -1L) fileSize else connection.contentLength.toLong()
                inputStream = BufferedInputStream(url.openStream(), 8192)

                if (outputPath != null) {
                    outputStream = FileOutputStream(outputPath)
                } else {
                    // Generate temporary file
                    val tempDir = System.getProperty("java.io.tmpdir")
                    tempFile = tempDir + UUID.randomUUID().toString() + "." + urlString.substringAfterLast(".")
                    outputStream = FileOutputStream(tempFile)
                }

                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    val currentMB = total / (1024 * 1024)
                    val totalMB = fileLength / (1024 * 1024)
                    val progress = "$currentMB MB / $totalMB MB"
                    publishProgress(progress)
                    outputStream.write(data, 0, count)
                }
                return outputPath ?: tempFile
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } finally {
                try {
                    outputStream?.flush()
                    outputStream?.close()
                    inputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun onProgressUpdate(vararg progress: String?) {
            progress[0]?.let { listener.onProgressUpdate(it) }
        }

        override fun onPostExecute(result: String?) {
            if (result != null) {
                listener.onComplete(result)
            } else {
                listener.onError("Failed to download data.")
            }
        }
    }
}


object XMLParser {

    fun parseXMLFile(filePath: String): Document? {
        return try {
            val xmlFile = File(filePath)
            if (!xmlFile.exists()) {
                throw IllegalArgumentException("File not found at path: $filePath")
            }

            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            documentBuilder.parse(xmlFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getElementValueByTagName(doc: Document, tagName: String): String? {
        val nodeList = doc.getElementsByTagName(tagName)
        return if (nodeList.length > 0) {
            val element = nodeList.item(0) as Element
            element.textContent
        } else {
            null
        }
    }
    fun documentToMap(doc: Document): Map<String, Map<String, String>> {
        val map = mutableMapOf<String, Map<String, String>>()
        val rootElement = doc.documentElement
        val itemNodes = rootElement.childNodes

        for (i in 0 until itemNodes.length) {
            val node = itemNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val itemElement = node as Element
                val itemAttributes = mutableMapOf<String, String>()

                // Extract attributes of the current item element
                val childNodes = itemElement.childNodes
                for (j in 0 until childNodes.length) {
                    val childNode = childNodes.item(j)
                    if (childNode.nodeType == Node.ELEMENT_NODE) {
                        val childElement = childNode as Element
                        val attributeName = childElement.tagName
                        val attributeValue = childElement.textContent
                        itemAttributes[attributeName] = attributeValue
                    }
                }

                // Add the attributes map to the outer map with the element name as the key
                map[itemElement.tagName] = itemAttributes
            }
        }

        return map
    }

}