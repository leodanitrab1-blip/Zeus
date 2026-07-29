package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFCompressor(private val context: Context) {

    private val fileManager = FileManager(context)

    fun compressPDF(
        uri: Uri,
        quality: Int = 50,
        outputFileName: String
    ): File? {
        val outputFile = fileManager.createOutputFile(outputFileName)
        val pdfDocument = PdfDocument()

        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            
            if (pfd != null) {
                val renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount

                for (i in 0 until totalPages) {
                    val page = renderer.openPage(i)
                    
                    val scale = when {
                        quality < 30 -> 0.4f
                        quality < 60 -> 0.6f
                        quality < 80 -> 0.8f
                        else -> 1.0f
                    }
                    
                    val newWidth = (page.width * scale).toInt()
                    val newHeight = (page.height * scale).toInt()

                    val originalBitmap = Bitmap.createBitmap(
                        page.width, page.height, Bitmap.Config.RGB_565
                    )
                    
                    page.render(originalBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        originalBitmap, newWidth, newHeight, true
                    )

                    originalBitmap.recycle()

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        newWidth, newHeight, i + 1
                    ).create()

                    val newPage = pdfDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(newPage)

                    scaledBitmap.recycle()
                    page.close()
                }

                renderer.close()
                pfd.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            pdfDocument.close()
            
            if (outputFile.length() <= 0) {
                outputFile.delete()
                return null
            }
            
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }
}