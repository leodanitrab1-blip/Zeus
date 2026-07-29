package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFConverter(private val context: Context) {

    private val fileManager = FileManager(context)

    fun imagesToPDF(uris: List<Uri>, outputFileName: String): File? {
        if (uris.isEmpty()) return null

        val outputFile = fileManager.createOutputFile(outputFileName)
        val pdfDocument = PdfDocument()

        return try {
            for ((index, uri) in uris.withIndex()) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width,
                        bitmap.height,
                        index + 1
                    ).create()

                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
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

    fun textToPDF(text: String, outputFileName: String): File? {
        val outputFile = fileManager.createOutputFile(outputFileName)
        val pdfDocument = PdfDocument()

        return try {
            val lines = text.split("\n")
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40
            val lineHeight = 14
            val maxLinesPerPage = (pageHeight - margin * 2) / lineHeight

            var currentLine = 0
            var pageNumber = 1

            while (currentLine < lines.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth, pageHeight, pageNumber
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint().apply {
                    textSize = 11f
                    color = android.graphics.Color.BLACK
                    isAntiAlias = true
                }

                var y = margin + lineHeight
                val linesThisPage = minOf(maxLinesPerPage, lines.size - currentLine)

                for (i in 0 until linesThisPage) {
                    val line = lines[currentLine + i]
                    if (line.isNotBlank()) {
                        canvas.drawText(line, margin.toFloat(), y.toFloat(), paint)
                    }
                    y += lineHeight
                }

                pdfDocument.finishPage(page)
                currentLine += linesThisPage
                pageNumber++
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