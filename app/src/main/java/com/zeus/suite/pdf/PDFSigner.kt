package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFSigner(private val context: Context) {

    private val fileManager = FileManager(context)

    fun signPDF(uri: Uri, signatureText: String, pageNumber: Int, outputFileName: String): File? {
        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK; textSize = 50f; isAntiAlias = true
        }
        canvas.drawText(signatureText, 20f, 60f, paint)
        return signPDFWithImage(uri, bitmap, outputFileName) { _, _ -> }
    }

    fun signPDFWithImage(uri: Uri, signature: Bitmap, outputFileName: String, onProgress: (Int, String) -> Unit): File? {
        val outputFile = fileManager.createOutputFile(outputFileName)
        val pdfDocument = PdfDocument()

        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            for (i in 0 until totalPages) {
                onProgress((i * 100) / totalPages, "Pagina ${i + 1} de $totalPages")

                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)

                val sigX = (page.width - signature.width) / 2f
                val sigY = page.height - signature.height - 30f
                newPage.canvas.drawBitmap(signature, sigX, sigY, null)

                pdfDocument.finishPage(newPage)
                bitmap.recycle()
                page.close()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            onProgress(100, "Completado")
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }
}
