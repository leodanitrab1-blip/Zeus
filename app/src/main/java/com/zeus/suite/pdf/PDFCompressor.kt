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
            
            if (pfd == null) {
                pdfDocument.close()
                return null
            }

            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            if (totalPages == 0) {
                renderer.close()
                pfd.close()
                pdfDocument.close()
                return null
            }

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                
                val scale = quality / 100f
                val newWidth = maxOf(100, (page.width * scale).toInt())
                val newHeight = maxOf(100, (page.height * scale).toInt())

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

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }

            pdfDocument.close()

            if (outputFile.exists() && outputFile.length() > 0) {
                outputFile
            } else {
                outputFile.delete()
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try { pdfDocument.close() } catch (_: Exception) {}
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }
}