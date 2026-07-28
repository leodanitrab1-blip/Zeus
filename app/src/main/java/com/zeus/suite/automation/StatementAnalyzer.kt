package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class StatementAnalyzer(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)

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
        val data = csvHandler.readCSV(uri) ?: return null

        val amountColumn = data.headers.find {
            it.contains("monto", ignoreCase = true) ||
            it.contains("importe", ignoreCase = true) ||
            it.contains("amount", ignoreCase = true) ||
            it.contains("valor", ignoreCase = true)
        }

        val typeColumn = data.headers.find {
            it.contains("tipo", ignoreCase = true) ||
            it.contains("type", ignoreCase = true) ||
            it.contains("debito", ignoreCase = true) ||
            it.contains("credito", ignoreCase = true)
        }

        val categoryColumn = data.headers.find {
            it.contains("categoria", ignoreCase = true) ||
            it.contains("category", ignoreCase = true) ||
            it.contains("concepto", ignoreCase = true) ||
            it.contains("descripcion", ignoreCase = true)
        }

        if (amountColumn == null) return null

        val amountIndex = data.headers.indexOf(amountColumn)
        val typeIndex = if (typeColumn != null) data.headers.indexOf(typeColumn) else -1
        val categoryIndex = if (categoryColumn != null) data.headers.indexOf(categoryColumn) else -1

        var totalCredits = 0.0
        var totalDebits = 0.0
        var largestCredit = 0.0
        var largestDebit = 0.0
        val amounts = mutableListOf<Double>()
        val categoryTotals = mutableMapOf<String, Double>()

        for (row in data.rows) {
            if (amountIndex < row.size) {
                val amount = row[amountIndex].toDoubleOrNull()?.let { Math.abs(it) } ?: 0.0
                amounts.add(amount)

                val isCredit = if (typeIndex != -1 && typeIndex < row.size) {
                    val type = row[typeIndex].lowercase()
                    type.contains("credito") || type.contains("credit") || type.contains("abono")
                } else {
                    amount >= 0
                }

                if (isCredit) {
                    totalCredits += amount
                    if (amount > largestCredit) largestCredit = amount
                } else {
                    totalDebits += amount
                    if (amount > largestDebit) largestDebit = amount
                }

                if (categoryIndex != -1 && categoryIndex < row.size) {
                    val category = row[categoryIndex]
                    categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
                }
            }
        }

        val netBalance = totalCredits - totalDebits
        val averageTransaction = if (amounts.isNotEmpty()) amounts.sum() / amounts.size else 0.0

        return StatementSummary(
            totalTransactions = amounts.size,
            totalCredits = totalCredits,
            totalDebits = totalDebits,
            netBalance = netBalance,
            averageTransaction = averageTransaction,
            largestCredit = largestCredit,
            largestDebit = largestDebit,
            categoryTotals = categoryTotals
        )
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
                val sortedCategories = summary.categoryTotals.toList()
                    .sortedByDescending { it.second }
                for ((category, total) in sortedCategories) {
                    sb.appendLine("$category: ${"%.2f".format(total)}")
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