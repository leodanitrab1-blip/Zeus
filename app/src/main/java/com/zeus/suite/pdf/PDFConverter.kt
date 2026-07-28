package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFConverter(private val context: Context) {

    private val fileManager = FileManager(context)

    fun convertToPDF(uri: Uri, targetFormat: String): File? {
        val originalName = fileManager.getFileName(uri)
        val baseName = originalName.substringBeforeLast(".")
        val outputFileName = "${baseName}_convertido.pdf"
        val tempFile = fileManager.copyUriToTempFile(uri, originalName) ?: return null
        val outputFile = fileManager.createOutputFile(outputFileName)

        try {
            when (targetFormat.lowercase()) {
                "txt" -> convertTextToPDF(tempFile, outputFile)
                "html" -> convertHtmlToPDF(tempFile, outputFile)
                else -> convertGenericToPDF(tempFile, outputFile)
            }

            fileManager.deleteTempFile(tempFile)
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        }
    }

    private fun convertTextToPDF(inputFile: File, outputFile: File) {
        val content = inputFile.readText()
        val pdfContent = """
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]
   /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
endobj
4 0 obj
<< /Length 44 >>
stream
BT /F1 12 Tf 72 720 Td ($content) Tj ET
endstream
endobj
5 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000266 00000 n 
0000000360 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
438
%%EOF
        """.trimIndent()

        outputFile.writeText(pdfContent)
    }

    private fun convertHtmlToPDF(inputFile: File, outputFile: File) {
        val content = inputFile.readText()
        val plainText = content.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        val tempTxtFile = File(context.cacheDir, "temp_convert.txt")
        tempTxtFile.writeText(plainText)
        convertTextToPDF(tempTxtFile, outputFile)
        tempTxtFile.delete()
    }

    private fun convertGenericToPDF(inputFile: File, outputFile: File) {
        val content = inputFile.readBytes()
        FileOutputStream(outputFile).use { output ->
            output.write(generatePDFHeader().toByteArray())
            output.write(content)
            output.write(generatePDFFooter().toByteArray())
        }
    }

    private fun generatePDFHeader(): String {
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
    }

    private fun generatePDFFooter(): String {
        return "\ntrailer\n<< /Size 2 /Root 1 0 R >>\n%%EOF"
    }
}