package com.zeus.suite.ai

import com.zeus.suite.bigdata.Statistics

class DataAnalyzer {

    private val statistics = Statistics()

    data class AnalysisResult(
        val totalRows: Int,
        val totalColumns: Int,
        val numericColumns: Int,
        val textColumns: Int,
        val emptyCells: Int,
        val completeness: Double
    )

    fun analyzeDataQuality(
        headers: List<String>,
        rows: List<List<String>>
    ): AnalysisResult {
        val totalRows = rows.size
        val totalColumns = headers.size
        var numericColumns = 0
        var textColumns = 0
        var emptyCells = 0
        val totalCells = totalRows * totalColumns

        for (header in headers) {
            val columnIndex = headers.indexOf(header)
            var hasNumeric = false
            var hasText = false

            for (row in rows) {
                if (columnIndex < row.size) {
                    val value = row[columnIndex]
                    if (value.isBlank()) {
                        emptyCells++
                    } else if (value.toDoubleOrNull() != null) {
                        hasNumeric = true
                    } else {
                        hasText = true
                    }
                } else {
                    emptyCells++
                }
            }

            if (hasNumeric) numericColumns++
            if (hasText) textColumns++
        }

        val completeness = if (totalCells > 0) {
            ((totalCells - emptyCells).toDouble() / totalCells) * 100.0
        } else {
            0.0
        }

        return AnalysisResult(
            totalRows = totalRows,
            totalColumns = totalColumns,
            numericColumns = numericColumns,
            textColumns = textColumns,
            emptyCells = emptyCells,
            completeness = completeness
        )
    }

    fun findCorrelations(
        headers: List<String>,
        rows: List<List<String>>
    ): List<Pair<String, String>> {
        val correlations = mutableListOf<Pair<String, String>>()
        val numericColumns = mutableListOf<String>()

        for (header in headers) {
            val stats = statistics.calculateColumnStats(headers, rows, header)
            if (stats != null) {
                numericColumns.add(header)
            }
        }

        for (i in numericColumns.indices) {
            for (j in i + 1 until numericColumns.size) {
                correlations.add(Pair(numericColumns[i], numericColumns[j]))
            }
        }

        return correlations
    }

    fun suggestVisualizations(
        headers: List<String>,
        rows: List<List<String>>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        val analysis = analyzeDataQuality(headers, rows)

        if (analysis.numericColumns >= 1 && analysis.textColumns >= 1) {
            suggestions.add("Grafico de barras: Comparar columna de texto vs numerica")
        }

        if (analysis.numericColumns >= 2) {
            suggestions.add("Grafico de dispersion: Relacion entre dos columnas numericas")
        }

        if (analysis.textColumns >= 1) {
            suggestions.add("Grafico circular: Distribucion de valores en columna de texto")
        }

        if (analysis.totalRows > 10) {
            suggestions.add("Grafico de lineas: Tendencia de datos numericos")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("No hay suficientes datos para sugerir visualizaciones")
        }

        return suggestions
    }
}