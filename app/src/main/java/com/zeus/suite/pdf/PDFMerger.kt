package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFMerger(private val context: Context) {

    private val fileManager = FileManager(context)

    fun mergePDFs(uris: List<Uri>, outputFileName: String): File? {
        if (uris.size < 2) return null

        val tempFiles = mutableListOf<File>()
        val pdfContents = mutableListOf<String>()

        for (uri in uris) {
            val fileName = fileManager.getFileName(uri)
            val tempFile = fileManager.copyUriToTempFile(uri, fileName)
            if (tempFile != null) {
                tempFiles.add(tempFile)
            }
        }

        if (tempFiles.size < 2) {
            tempFiles.forEach { fileManager.deleteTempFile(it) }
            return null
        }

        val outputFile = fileManager.createOutputFile(outputFileName)

        try {
            val mergedContent = mergePDFContents(tempFiles)
            
            if (mergedContent != null) {
                outputFile.writeText(mergedContent)
            } else {
                simpleMerge(tempFiles, outputFile)
            }

            tempFiles.forEach { fileManager.deleteTempFile(it) }
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            tempFiles.forEach { fileManager.deleteTempFile(it) }
            if (outputFile.exists()) outputFile.delete()
            return null
        }
    }

    private fun mergePDFContents(files: List<File>): String? {
        try {
            val allPages = mutableListOf<String>()
            var catalogObj = ""
            var pageCount = 0
            val fontObjects = mutableMapOf<String, String>()
            
            for (file in files) {
                val content = file.readText()
                
                if (!content.startsWith("%PDF")) continue

                val pageRegex = Regex("/(Type\\s*/Page[^>]*>>)")
                val pages = pageRegex.findAll(content).toList()
                
                for (page in pages) {
                    allPages.add(page.value)
                    pageCount++
                }

                val fontRegex = Regex("/(F\\d+)\\s+<<\\s*/Type\\s*/Font[^>]*>>")
                val fonts = fontRegex.findAll(content)
                for (font in fonts) {
                    val fontName = font.groupValues[1]
                    fontObjects[fontName] = font.value
                }
            }

            if (allPages.isEmpty()) return null

            val sb = StringBuilder()
            sb.appendLine("%PDF-1.4")
            
            var offset = 0
            
            val catalogObjNum = 1
            val pagesObjNum = 2
            
            val pagesKids = StringBuilder()
            for (i in allPages.indices) {
                val pageNum = 3 + i
                pagesKids.append("$pageNum 0 R ")
                
                val pageObj = "%%Page $pageNum\n$pageNum 0 obj\n${allPages[i]}\nendobj\n"
                sb.append(pageObj)
            }

            val catalog = "%%Catalog\n$catalogObjNum 0 obj\n<< /Type /Catalog /Pages $pagesObjNum 0 R >>\nendobj\n"
            val pages = "%%Pages\n$pagesObjNum 0 obj\n<< /Type /Pages /Kids [${pagesKids}] /Count $pageCount >>\nendobj\n"
            
            val fullPdf = "%PDF-1.4\n$catalog$pages${sb}"

            return fullPdf

        } catch (e: Exception) {
            return null
        }
    }

    private fun simpleMerge(files: List<File>, output: File) {
        FileOutputStream(output).use { outputStream ->
            for (file in files) {
                file.inputStream().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
}