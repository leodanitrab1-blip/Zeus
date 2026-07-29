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
                        quality < 30 -> 0.5f
                        quality < 60 -> 0.7f
                        else -> 0.85f
                    }
                    
                    val newWidth = (page.width * scale).toInt()
                    val newHeight = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.RGB_565)
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also {
                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        },
                        newWidth,
                        newHeight,
                        true
                    )

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        newWidth,
                        newHeight,
                        i + 1
                    ).create()

                    val newPage = pdfDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(newPage)

                    bitmap.recycle()
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
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }
}