package com.zeus.suite.pdf

import android.content.Context
import com.zeus.suite.utils.FileManager
import java.io.File

class PDFCreator(private val context: Context) {

    private val fileManager = FileManager(context)

    fun createSimplePDF(
        fileName: String,
        title: String,
        content: String
    ): File? {
        val outputFile = fileManager.createOutputFile("$fileName.pdf")

        return try {
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
<< /Length 200 >>
stream
BT
/F1 24 Tf
72 720 Td
($title) Tj
/F1 12 Tf
0 -30 Td
($content) Tj
ET
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
0000000516 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
594
%%EOF
            """.trimIndent()

            outputFile.writeText(pdfContent)
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        }
    }

    fun createPDFFromText(
        fileName: String,
        lines: List<String>
    ): File? {
        val outputFile = fileManager.createOutputFile("$fileName.pdf")

        return try {
            val textContent = lines.joinToString(" ")
            val sb = StringBuilder()

            sb.appendLine("%PDF-1.4")
            sb.appendLine("1 0 obj")
            sb.appendLine("<< /Type /Catalog /Pages 2 0 R >>")
            sb.appendLine("endobj")
            sb.appendLine("2 0 obj")
            sb.appendLine("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            sb.appendLine("endobj")
            sb.appendLine("3 0 obj")
            sb.appendLine("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]")
            sb.appendLine("   /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>")
            sb.appendLine("endobj")
            sb.appendLine("4 0 obj")
            sb.appendLine("<< /Length 100 >>")
            sb.appendLine("stream")
            sb.appendLine("BT /F1 12 Tf 72 720 Td ($textContent) Tj ET")
            sb.appendLine("endstream")
            sb.appendLine("endobj")
            sb.appendLine("5 0 obj")
            sb.appendLine("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
            sb.appendLine("endobj")
            sb.appendLine("xref")
            sb.appendLine("0 6")
            sb.appendLine("0000000000 65535 f ")
            sb.appendLine("0000000009 00000 n ")
            sb.appendLine("0000000058 00000 n ")
            sb.appendLine("0000000115 00000 n ")
            sb.appendLine("0000000266 00000 n ")
            sb.appendLine("0000000416 00000 n ")
            sb.appendLine("trailer")
            sb.appendLine("<< /Size 6 /Root 1 0 R >>")
            sb.appendLine("startxref")
            sb.appendLine("494")
            sb.appendLine("%%EOF")

            outputFile.writeText(sb.toString())
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        }
    }
}