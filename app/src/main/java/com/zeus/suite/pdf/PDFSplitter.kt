package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFSplitter(private val context: Context) {

    private val fileManager = FileManager(context)

    fun getPageCount(uri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) { 0 }
    }

    fun splitPDF(uri: Uri, selectedPages: List<Int>, outputFileName: String): File? {
        return splitPDFWithProgress(uri, selectedPages, outputFileName) { _, _ -> }
    }

    fun splitPDFWithProgress(uri: Uri, selectedPages: List<Int>, outputFileName: String, onProgress: (Int, String) -> Unit): File? {
        if (selectedPages.isEmpty()) return null
        val outputFile = fileManager.createOutputFile(outputFileName)
        val pdfDocument = PdfDocument()

        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            val total = selectedPages.size

            for ((index, pageIndex) in selectedPages.withIndex()) {
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) continue
                onProgress((index * 100) / total, "Pagina ${index + 1} de $total")

                val page = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, index + 1).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
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

    fun splitPDFInParts(uri: Uri, parts: Int): List<File> {
        return splitPDFInPartsWithProgress(uri, parts) { _, _ -> }
    }

    fun splitPDFInPartsWithProgress(uri: Uri, parts: Int, onProgress: (Int, String) -> Unit): List<File> {
        val outputFiles = mutableListOf<File>()
        val totalPages = getPageCount(uri)
        if (totalPages <= 1 || parts <= 1 || parts > totalPages) return outputFiles

        val pagesPerPart = totalPages / parts
        val remainder = totalPages % parts
        val baseName = fileManager.getFileName(uri).substringBeforeLast(".")

        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return outputFiles
            val renderer = PdfRenderer(pfd)
            var currentPage = 0

            for (part in 0 until parts) {
                val partPages = if (part < remainder) pagesPerPart + 1 else pagesPerPart
                if (partPages <= 0) continue

                val pdfDocument = PdfDocument()
                for (i in 0 until partPages) {
                    if (currentPage >= totalPages) break
                    onProgress((currentPage * 100) / totalPages, "Parte ${part + 1} de $parts")

                    val page = renderer.openPage(currentPage)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                    val newPage = pdfDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(newPage)

                    bitmap.recycle()
                    page.close()
                    currentPage++
                }

                val outputFile = fileManager.createOutputFile("${baseName}_parte${part + 1}.pdf")
                FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
                pdfDocument.close()
                outputFiles.add(outputFile)
            }

            renderer.close()
            pfd.close()
            onProgress(100, "Completado")
            outputFiles
        } catch (e: Exception) {
            e.printStackTrace()
            outputFiles
        }
    }
}
