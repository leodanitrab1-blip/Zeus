package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFSigner(private val context: Context) {

    private val fileManager = FileManager(context)

    fun createSignatureBitmap(text: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 60f
            isAntiAlias = true
            isFakeBoldText = true
            style = Paint.Style.FILL
        }
        
        val x = (width - paint.measureText(text)) / 2
        val y = height / 2f + paint.textSize / 3f
        
        canvas.drawText(text, x, y, paint)
        
        return bitmap
    }

    fun signPDF(
        uri: Uri,
        signatureText: String,
        pageNumber: Int = 0,
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
                    val bitmap = Bitmap.createBitmap(
                        page.width,
                        page.height,
                        Bitmap.Config.ARGB_8888
                    )

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        page.width,
                        page.height,
                        i + 1
                    ).create()

                    val newPage = pdfDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)

                    if (i == pageNumber) {
                        val signatureBitmap = createSignatureBitmap(
                            signatureText,
                            page.width / 2,
                            100
                        )
                        val x = (page.width - signatureBitmap.width) / 2f
                        val y = page.height - signatureBitmap.height - 50f
                        newPage.canvas.drawBitmap(signatureBitmap, x, y, null)
                        signatureBitmap.recycle()
                    }

                    pdfDocument.finishPage(newPage)
                    bitmap.recycle()
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