package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.DeflaterOutputStream

class PDFCompressor(private val context: Context) {

    private val fileManager = FileManager(context)

    fun compressPDF(uri: Uri): File? {
        val fileName = fileManager.getFileName(uri)
        val compressedFileName = "comprimido_$fileName"
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return null
        val outputFile = fileManager.createOutputFile(compressedFileName)

        try {
            FileInputStream(tempFile).use { input ->
                FileOutputStream(outputFile).use { fileOutput ->
                    DeflaterOutputStream(fileOutput).use { deflaterOutput ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            deflaterOutput.write(buffer, 0, bytesRead)
                        }
                        deflaterOutput.finish()
                    }
                }
            }

            fileManager.deleteTempFile(tempFile)
            
            if (outputFile.length() >= tempFile.length()) {
                return null
            }
            
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        }
    }
}