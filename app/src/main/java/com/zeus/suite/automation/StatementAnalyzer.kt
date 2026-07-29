package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.bigdata.ExcelHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class StatementAnalyzer(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)
    private val excelHandler = ExcelHandler(context)

    data class StatementSummary(
        val totalTransactions: Int,
        val totalCredits: Double,
        val totalDebits: Double,
        val netBalance: Double,
        val averageTransaction: Double,
        val largestCredit: Double,
        val largestDebit: Double,
        val categoryTotals: Map<String, Double>
    )

    fun analyzeStatements(uri: Uri): StatementSummary? {
        val csvData = csvHandler.readCSV(uri, ",", 2000)
        val data: CSVHandler.CSVData? = if (csvData != null && csvData.rows.isNotEmpty()) {
            csvData
        } else {
            excelHandler.readExcel(uri, 2000)?.let { CSVHandler.CSVData(it.headers, it.rows) }
        }
        
        if (data == null || data.rows.isEmpty()) return null

        val amountIndex = findColumnIndex(data.headers, listOf(
            "monto", "total", "importe", "amount", "valor", "suma", "saldo"
        ))
        
        if (amountIndex == -1) return null

        var totalCredits = 0.0
        var totalDebits = 0.0
        var largestCredit = 0.0
        var largestDebit = 0.0
        val amounts = mutableListOf<Double>()
        val categoryTotals = mutableMapOf<String, Double>()

        val typeIndex = findColumnIndex(data.headers, listOf(
            "tipo", "type", "movimiento", "operacion", "transaccion", "concepto"
        ))
        val categoryIndex = findColumnIndex(data.headers, listOf(
            "categoria", "category", "descripcion", "detalle", "referencia"
        ))

        for (row in data.rows) {
            if (amountIndex >= row.size) continue
            
            val amountStr = row[amountIndex]
                .replace("\"", "").replace("'", "").trim()
            
            val amount = parseAmount(amountStr)
            if (amount == null) continue

            val absAmount = Math.abs(amount)
            amounts.add(absAmount)

            val isCredit = if (typeIndex != -1 && typeIndex < row.size) {
                val type = row[typeIndex].lowercase()
                type.contains("credito") || type.contains("credit") ||
                type.contains("abono") || type.contains("ingreso") ||
                type.contains("deposito") || type.contains("entrada") ||
                type.contains("pago recibido") || type.contains("transferencia recibida")
            } else {
                amount > 0
            }

            if (isCredit) {
                totalCredits += absAmount
                if (absAmount > largestCredit) largestCredit = absAmount
            } else {
                totalDebits += absAmount
                if (absAmount > largestDebit) largestDebit = absAmount
            }

            if (categoryIndex != -1 && categoryIndex < row.size) {
                val category = row[categoryIndex].trim().ifBlank { "Sin categoria" }
                categoryTotals[category] = (categoryTotals[category] ?: 0.0) + absAmount
            }
        }

        if (amounts.isEmpty()) return null

        return StatementSummary(
            totalTransactions = amounts.size,
            totalCredits = totalCredits,
            totalDebits = totalDebits,
            netBalance = totalCredits - totalDebits,
            averageTransaction = amounts.sum() / amounts.size,
            largestCredit = largestCredit,
            largestDebit = largestDebit,
            categoryTotals = categoryTotals
        )
    }

    private fun parseAmount(str: String): Double? {
        var cleaned = str
            .replace("$", "")
            .replace("USD", "")
            .replace("MXN", "")
            .replace("EUR", "")
            .replace(" ", "")
            .trim()
        
        val isNegative = cleaned.startsWith("-") || cleaned.startsWith("(")
        cleaned = cleaned.replace("-", "").replace("(", "").replace(")", "").replace(",", "")
        
        val value = cleaned.toDoubleOrNull()
        return if (value != null) {
            if (isNegative) -value else value
        } else null
    }

    fun generateAnalysisReport(uri: Uri): File? {
        val summary = analyzeStatements(uri) ?: return null
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val reportFile = fileManager.createOutputFile("${baseName}_analisis.txt")

        return try {
            val sb = StringBuilder()
            sb.appendLine("=== ANALISIS DE ESTADOS DE CUENTA ===")
            sb.appendLine()
            sb.appendLine("Total de transacciones: ${summary.totalTransactions}")
            sb.appendLine("Total creditos: +${"%.2f".format(summary.totalCredits)}")
            sb.appendLine("Total debitos: -${"%.2f".format(summary.totalDebits)}")
            sb.appendLine("Balance neto: ${"%.2f".format(summary.netBalance)}")
            sb.appendLine("Promedio por transaccion: ${"%.2f".format(summary.averageTransaction)}")
            sb.appendLine()
            sb.appendLine("Mayor credito: ${"%.2f".format(summary.largestCredit)}")
            sb.appendLine("Mayor debito: ${"%.2f".format(summary.largestDebit)}")
            sb.appendLine()
            if (summary.categoryTotals.isNotEmpty()) {
                sb.appendLine("--- Totales por Categoria ---")
                for ((cat, total) in summary.categoryTotals.toList().sortedByDescending { it.second }) {
                    sb.appendLine("$cat: ${"%.2f".format(total)}")
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