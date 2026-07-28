package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFSigner(private val context: Context) {

    private val fileManager = FileManager(context)

    fun createSignatureBitmap(signatureText: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        val x = (width - paint.measureText(signatureText)) / 2
        val y = (height + paint.textSize) / 2
        
        canvas.drawText(signatureText, x, y, paint)
        
        return bitmap
    }

    fun saveSignatureBitmap(bitmap: Bitmap, fileName: String): File? {
        return try {
            val file = fileManager.createOutputFile(fileName)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun signPDF(uri: Uri, signatureText: String): File? {
        val fileName = fileManager.getFileName(uri)
        val signedFileName = "firmado_$fileName"
        val outputFile = fileManager.createOutputFile(signedFileName)
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return null

        try {
            tempFile.inputStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            fileManager.deleteTempFile(tempFile)
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