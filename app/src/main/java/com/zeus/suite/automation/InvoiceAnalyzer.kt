package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.bigdata.ExcelHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class InvoiceAnalyzer(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)
    private val excelHandler = ExcelHandler(context)

    data class InvoiceSummary(
        val totalInvoices: Int,
        val totalAmount: Double,
        val averageAmount: Double,
        val maxInvoice: Pair<String, Double>,
        val minInvoice: Pair<String, Double>,
        val monthlyTotals: Map<String, Double>
    )

    fun analyzeInvoices(uri: Uri): InvoiceSummary? {
        val csvData = csvHandler.readCSV(uri, ",", 1000)
        val data = if (csvData != null && csvData.rows.isNotEmpty()) csvData
        else excelHandler.readExcel(uri, 1000)?.let { CSVHandler.CSVData(it.headers, it.rows) }
        
        if (data == null || data.rows.isEmpty()) return null

        val amountIndex = findColumnIndex(data.headers, listOf("monto", "total", "importe", "amount", "valor", "suma"))
        val dateIndex = findColumnIndex(data.headers, listOf("fecha", "date", "mes", "month", "periodo"))
        val invoiceIndex = findColumnIndex(data.headers, listOf("factura", "invoice", "numero", "id", "codigo", "num"))

        if (amountIndex == -1) {
            amountIndex = 0
        }

        val amounts = mutableListOf<Double>()
        val invoiceAmounts = mutableListOf<Pair<String, Double>>()
        val monthlyTotals = mutableMapOf<String, Double>()

        for (row in data.rows) {
            if (amountIndex < row.size) {
                val amount = row[amountIndex]
                    .replace("$", "").replace(",", "").replace("\"", "").trim()
                    .toDoubleOrNull() ?: 0.0
                
                amounts.add(amount)

                val invoiceId = if (invoiceIndex != -1 && invoiceIndex < row.size) {
                    row[invoiceIndex].trim()
                } else {
                    "N/A"
                }
                invoiceAmounts.add(Pair(invoiceId, amount))

                if (dateIndex != -1 && dateIndex < row.size) {
                    val dateStr = row[dateIndex].trim()
                    val month = if (dateStr.length >= 7) dateStr.take(7) else dateStr
                    monthlyTotals[month] = (monthlyTotals[month] ?: 0.0) + amount
                }
            }
        }

        if (amounts.isEmpty()) return null

        val totalAmount = amounts.sum()
        val averageAmount = totalAmount / amounts.size
        val maxInvoice = invoiceAmounts.maxByOrNull { it.second } ?: Pair("N/A", 0.0)
        val minInvoice = invoiceAmounts.minByOrNull { it.second } ?: Pair("N/A", 0.0)

        return InvoiceSummary(
            totalInvoices = amounts.size,
            totalAmount = totalAmount,
            averageAmount = averageAmount,
            maxInvoice = maxInvoice,
            minInvoice = minInvoice,
            monthlyTotals = monthlyTotals
        )
    }

    fun generateMonthlyReport(uri: Uri): File? {
        val summary = analyzeInvoices(uri) ?: return null
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val reportFile = fileManager.createOutputFile("${baseName}_resumen_mensual.txt")

        return try {
            val sb = StringBuilder()
            sb.appendLine("=== RESUMEN MENSUAL DE FACTURAS ===")
            sb.appendLine()
            sb.appendLine("Total de facturas: ${summary.totalInvoices}")
            sb.appendLine("Monto total: ${"%.2f".format(summary.totalAmount)}")
            sb.appendLine("Promedio por factura: ${"%.2f".format(summary.averageAmount)}")
            sb.appendLine()
            sb.appendLine("Factura mas alta: ${summary.maxInvoice.first} - ${"%.2f".format(summary.maxInvoice.second)}")
            sb.appendLine("Factura mas baja: ${summary.minInvoice.first} - ${"%.2f".format(summary.minInvoice.second)}")
            sb.appendLine()
            if (summary.monthlyTotals.isNotEmpty()) {
                sb.appendLine("--- Totales por Mes ---")
                val sortedMonths = summary.monthlyTotals.toList().sortedBy { it.first }
                for ((month, total) in sortedMonths) {
                    sb.appendLine("$month: ${"%.2f".format(total)}")
                }
            }
            reportFile.writeText(sb.toString())
            reportFile
        } catch (e: Exception) {
            e.printStackTrace()
            if (reportFile.exists()) reportFile.delete()
            null
        }
    }

    private fun findColumnIndex(headers: List<String>, keywords: List<String>): Int {
        for (keyword in keywords) {
            val index = headers.indexOfFirst { it.lowercase().contains(keyword.lowercase()) }
            if (index != -1) return index
        }
        return -1
    }
}