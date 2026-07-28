package com.zeus.suite.bigdata

class Statistics {

    data class ColumnStats(
        val columnName: String,
        val count: Int,
        val sum: Double,
        val mean: Double,
        val median: Double,
        val min: Double,
        val max: Double,
        val standardDeviation: Double
    )

    fun calculateColumnStats(
        headers: List<String>,
        rows: List<List<String>>,
        columnName: String
    ): ColumnStats? {
        val columnIndex = headers.indexOfFirst {
            it.equals(columnName, ignoreCase = true)
        }

        if (columnIndex == -1) return null

        val numericValues = mutableListOf<Double>()

        for (row in rows) {
            if (columnIndex < row.size) {
                try {
                    numericValues.add(row[columnIndex].toDouble())
                } catch (e: NumberFormatException) {
                    // Ignorar valores no numericos
                }
            }
        }

        if (numericValues.isEmpty()) return null

        val sortedValues = numericValues.sorted()
        val count = numericValues.size
        val sum = numericValues.sum()
        val mean = sum / count
        val min = sortedValues.first()
        val max = sortedValues.last()

        val median = if (count % 2 == 0) {
            (sortedValues[count / 2 - 1] + sortedValues[count / 2]) / 2.0
        } else {
            sortedValues[count / 2]
        }

        val variance = numericValues.sumOf { (it - mean) * (it - mean) } / count
        val standardDeviation = Math.sqrt(variance)

        return ColumnStats(
            columnName = columnName,
            count = count,
            sum = sum,
            mean = mean,
            median = median,
            min = min,
            max = max,
            standardDeviation = standardDeviation
        )
    }

    fun calculateAllStats(
        headers: List<String>,
        rows: List<List<String>>
    ): List<ColumnStats> {
        val statsList = mutableListOf<ColumnStats>()

        for (header in headers) {
            val stats = calculateColumnStats(headers, rows, header)
            if (stats != null) {
                statsList.add(stats)
            }
        }

        return statsList
    }

    fun getFrequencyDistribution(
        rows: List<List<String>>,
        columnIndex: Int
    ): Map<String, Int> {
        val frequency = mutableMapOf<String, Int>()

        for (row in rows) {
            if (columnIndex < row.size) {
                val value = row[columnIndex]
                frequency[value] = (frequency[value] ?: 0) + 1
            }
        }

        return frequency.toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    fun getSummary(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.appendLine("=== RESUMEN ESTADISTICO ===")
        sb.appendLine("Filas totales: ${rows.size}")
        sb.appendLine("Columnas: ${headers.size}")
        sb.appendLine()

        val stats = calculateAllStats(headers, rows)

        for (stat in stats) {
            sb.appendLine("--- ${stat.columnName} ---")
            sb.appendLine("  Conteo: ${stat.count}")
            sb.appendLine("  Suma: ${"%.2f".format(stat.sum)}")
            sb.appendLine("  Media: ${"%.2f".format(stat.mean)}")
            sb.appendLine("  Mediana: ${"%.2f".format(stat.median)}")
            sb.appendLine("  Min: ${"%.2f".format(stat.min)}")
            sb.appendLine("  Max: ${"%.2f".format(stat.max)}")
            sb.appendLine("  Desviacion Estandar: ${"%.2f".format(stat.standardDeviation)}")
            sb.appendLine()
        }

        return sb.toString()
    }
}