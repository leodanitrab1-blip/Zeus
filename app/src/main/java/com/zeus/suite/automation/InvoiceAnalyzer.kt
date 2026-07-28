package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class InvoiceAnalyzer(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)

    data class InvoiceSummary(
        val totalInvoices: Int,
        val totalAmount: Double,
        val averageAmount: Double,
        val maxInvoice: Pair<String, Double>,
        val minInvoice: Pair<String, Double>,
        val monthlyTotals: Map<String, Double>
    )

    fun analyzeInvoices(uri: Uri): InvoiceSummary? {
        val data = csvHandler.readCSV(uri) ?: return null

        val invoiceColumn = data.headers.find {
            it.contains("factura", ignoreCase = true) ||
            it.contains("invoice", ignoreCase = true) ||
            it.contains("numero", ignoreCase = true)
        }

        val amountColumn = data.headers.find {
            it.contains("monto", ignoreCase = true) ||
            it.contains("total", ignoreCase = true) ||
            it.contains("importe", ignoreCase = true) ||
            it.contains("amount", ignoreCase = true)
        }

        val dateColumn = data.headers.find {
            it.contains("fecha", ignoreCase = true) ||
            it.contains("date", ignoreCase = true) ||
            it.contains("mes", ignoreCase = true)
        }

        if (amountColumn == null) return null

        val amountIndex = data.headers.indexOf(amountColumn)
        val invoiceIndex = if (invoiceColumn != null) data.headers.indexOf(invoiceColumn) else -1
        val dateIndex = if (dateColumn != null) data.headers.indexOf(dateColumn) else -1

        val amounts = mutableListOf<Double>()
        val invoiceAmounts = mutableListOf<Pair<String, Double>>()
        val monthlyTotals = mutableMapOf<String, Double>()

        for (row in data.rows) {
            if (amountIndex < row.size) {
                val amount = row[amountIndex].toDoubleOrNull() ?: 0.0
                amounts.add(amount)

                val invoiceNumber = if (invoiceIndex != -1 && invoiceIndex < row.size) {
                    row[invoiceIndex]
                } else {
                    "N/A"
                }

                invoiceAmounts.add(Pair(invoiceNumber, amount))

                if (dateIndex != -1 && dateIndex < row.size) {
                    val month = row[dateIndex].take(7)
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
}