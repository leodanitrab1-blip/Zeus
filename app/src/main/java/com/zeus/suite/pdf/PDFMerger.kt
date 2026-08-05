package com.zeus.suite.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFMerger(private val context: Context) {

    private val fileManager = FileManager(context)

    fun mergePDFs(uris: List<Uri>, outputFileName: String): File? {
        return mergePDFsWithProgress(uris, outputFileName) { _, _ -> }
    }

    fun mergePDFsWithProgress(uris: List<Uri>, outputFileName: String, onProgress: (Int, String) -> Unit): File? {
        if (uris.size < 2) return null
        val outputFile = fileManager.createOutputFile(outputFileName)
        val mergedDocument = PdfDocument()
        var totalPages = 0

        try {
            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: continue
                val renderer = PdfRenderer(pfd)
                totalPages += renderer.pageCount
                renderer.close()
                pfd.close()
            }

            if (totalPages == 0) {
                mergedDocument.close()
                return null
            }

            var currentPage = 0
            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: continue
                val renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    onProgress((currentPage * 100) / totalPages, "Pagina ${currentPage + 1} de $totalPages")

                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, currentPage + 1).create()
                    val newPage = mergedDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    mergedDocument.finishPage(newPage)

                    bitmap.recycle()
                    page.close()
                    currentPage++
                }
                renderer.close()
                pfd.close()
            }

            FileOutputStream(outputFile).use { mergedDocument.writeTo(it) }
            mergedDocument.close()
            onProgress(100, "Completado")
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            mergedDocument.close()
            if (outputFile.exists()) outputFile.delete()
            return null
        }
    }

    fun mergeAlternating(uris: List<Uri>, outputFileName: String, onProgress: (Int, String) -> Unit): File? {
        if (uris.size < 2) return null
        val outputFile = fileManager.createOutputFile(outputFileName)
        val mergedDocument = PdfDocument()

        try {
            val renderers = mutableListOf<PdfRenderer>()
            val pfds = mutableListOf<android.os.ParcelFileDescriptor>()
            val pageCounts = mutableListOf<Int>()
            var totalPages = 0

            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: continue
                val renderer = PdfRenderer(pfd)
                renderers.add(renderer)
                pfds.add(pfd)
                pageCounts.add(renderer.pageCount)
                totalPages += renderer.pageCount
            }

            if (renderers.isEmpty()) {
                mergedDocument.close()
                return null
            }

            val maxPages = pageCounts.maxOrNull() ?: 0
            var currentPage = 0

            for (i in 0 until maxPages) {
                for ((index, renderer) in renderers.withIndex()) {
                    if (i < pageCounts[index]) {
                        onProgress((currentPage * 100) / totalPages, "Intercalando pagina ${currentPage + 1}")

                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, currentPage + 1).create()
                        val newPage = mergedDocument.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        mergedDocument.finishPage(newPage)

                        bitmap.recycle()
                        page.close()
                        currentPage++
                    }
                }
            }

            renderers.forEach { it.close() }
            pfds.forEach { it.close() }

            FileOutputStream(outputFile).use { mergedDocument.writeTo(it) }
            mergedDocument.close()
            onProgress(100, "Completado")
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            mergedDocument.close()
            if (outputFile.exists()) outputFile.delete()
            return null
        }
    }

    fun mergeAsPackage(uris: List<Uri>, outputFileName: String, onProgress: (Int, String) -> Unit): File? {
        return mergePDFsWithProgress(uris, outputFileName, onProgress)
    }
}
