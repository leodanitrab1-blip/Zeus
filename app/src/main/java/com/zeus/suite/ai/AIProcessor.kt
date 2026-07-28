package com.zeus.suite.ai

import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.bigdata.DataFilter
import com.zeus.suite.bigdata.DataSearch
import com.zeus.suite.bigdata.Statistics

class AIProcessor {

    private val dataFilter = DataFilter()
    private val dataSearch = DataSearch()
    private val statistics = Statistics()

    fun answerQuestion(
        question: String,
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val lowerQuestion = question.lowercase()

        return when {
            lowerQuestion.contains("cliente") && lowerQuestion.contains("compro") -> {
                findTopCustomer(headers, rows)
            }
            lowerQuestion.contains("resumen") -> {
                generateSummary(headers, rows)
            }
            lowerQuestion.contains("anomalia") -> {
                detectAnomalies(headers, rows)
            }
            lowerQuestion.contains("reporte") -> {
                generateExecutiveReport(headers, rows)
            }
            lowerQuestion.contains("promedio") || lowerQuestion.contains("media") -> {
                calculateAverage(headers, rows)
            }
            lowerQuestion.contains("total") || lowerQuestion.contains("suma") -> {
                calculateTotal(headers, rows)
            }
            lowerQuestion.contains("maximo") || lowerQuestion.contains("mayor") -> {
                findMaximum(headers, rows)
            }
            lowerQuestion.contains("minimo") || lowerQuestion.contains("menor") -> {
                findMinimum(headers, rows)
            }
            else -> {
                "No puedo responder esa pregunta. Intente preguntar por: cliente que mas compro, resumen, anomalias, reporte ejecutivo, promedio, total, maximo o minimo."
            }
        }
    }

    private fun findTopCustomer(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val clientColumn = headers.find {
            it.contains("cliente", ignoreCase = true) ||
            it.contains("nombre", ignoreCase = true)
        }

        val amountColumn = headers.find {
            it.contains("monto", ignoreCase = true) ||
            it.contains("total", ignoreCase = true) ||
            it.contains("compra", ignoreCase = true) ||
            it.contains("importe", ignoreCase = true)
        }

        if (clientColumn == null || amountColumn == null) {
            return "No se encontraron columnas de cliente y monto en los datos."
        }

        val clientIndex = headers.indexOf(clientColumn)
        val amountIndex = headers.indexOf(amountColumn)

        val customerTotals = mutableMapOf<String, Double>()

        for (row in rows) {
            if (clientIndex < row.size && amountIndex < row.size) {
                val client = row[clientIndex]
                val amount = row[amountIndex].toDoubleOrNull() ?: 0.0
                customerTotals[client] = (customerTotals[client] ?: 0.0) + amount
            }
        }

        if (customerTotals.isEmpty()) {
            return "No se encontraron datos de clientes."
        }

        val topCustomer = customerTotals.maxByOrNull { it.value }
        return "El cliente que mas compro fue: ${topCustomer?.key} con un total de ${"%.2f".format(topCustomer?.value)}"
    }

    private fun generateSummary(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        return statistics.getSummary(headers, rows)
    }

    private fun detectAnomalies(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val anomalyDetector = AnomalyDetector()
        val anomalies = anomalyDetector.detect(headers, rows)

        if (anomalies.isEmpty()) {
            return "No se detectaron anomalias en los datos."
        }

        val sb = StringBuilder()
        sb.appendLine("=== ANOMALIAS DETECTADAS ===")
        sb.appendLine("Total de anomalias: ${anomalies.size}")
        sb.appendLine()

        for ((index, anomaly) in anomalies.withIndex()) {
            if (index < 10) {
                sb.appendLine("Anomalia ${index + 1}: $anomaly")
            }
        }

        if (anomalies.size > 10) {
            sb.appendLine("... y ${anomalies.size - 10} anomalias mas.")
        }

        return sb.toString()
    }

    private fun generateExecutiveReport(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val reportGenerator = ReportGenerator()
        return reportGenerator.generateReport(headers, rows)
    }

    private fun calculateAverage(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val allStats = statistics.calculateAllStats(headers, rows)
        val sb = StringBuilder()
        sb.appendLine("=== PROMEDIOS ===")

        for (stat in allStats) {
            sb.appendLine("${stat.columnName}: ${"%.2f".format(stat.mean)}")
        }

        return sb.toString()
    }

    private fun calculateTotal(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val allStats = statistics.calculateAllStats(headers, rows)
        val sb = StringBuilder()
        sb.appendLine("=== TOTALES ===")

        for (stat in allStats) {
            sb.appendLine("${stat.columnName}: ${"%.2f".format(stat.sum)}")
        }

        return sb.toString()
    }

    private fun findMaximum(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val allStats = statistics.calculateAllStats(headers, rows)
        val sb = StringBuilder()
        sb.appendLine("=== VALORES MAXIMOS ===")

        for (stat in allStats) {
            sb.appendLine("${stat.columnName}: ${"%.2f".format(stat.max)}")
        }

        return sb.toString()
    }

    private fun findMinimum(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val allStats = statistics.calculateAllStats(headers, rows)
        val sb = StringBuilder()
        sb.appendLine("=== VALORES MINIMOS ===")

        for (stat in allStats) {
            sb.appendLine("${stat.columnName}: ${"%.2f".format(stat.min)}")
        }

        return sb.toString()
    }
}