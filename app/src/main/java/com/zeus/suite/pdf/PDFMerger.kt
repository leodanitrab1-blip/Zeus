package com.zeus.suite.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFMerger(private val context: Context) {

    private val fileManager = FileManager(context)

    fun mergePDFs(uris: List<Uri>, outputFileName: String): File? {
        if (uris.size < 2) return null

        val outputFile = fileManager.createOutputFile(outputFileName)
        val mergedDocument = PdfDocument()

        try {
            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val pageCount = renderer.pageCount

                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            page.width,
                            page.height,
                            i + 1
                        ).create()
                        
                        val newPage = mergedDocument.startPage(pageInfo)
                        page.render(newPage.canvas, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        mergedDocument.finishPage(newPage)
                        
                        page.close()
                    }
                    
                    renderer.close()
                    pfd.close()
                }
            }

            FileOutputStream(outputFile).use { fos ->
                mergedDocument.writeTo(fos)
            }
            
            mergedDocument.close()
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            mergedDocument.close()
            if (outputFile.exists()) outputFile.delete()
            return null
        }
    }
}