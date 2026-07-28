package com.zeus.suite.ai

import com.zeus.suite.bigdata.Statistics

class AnomalyDetector {

    private val statistics = Statistics()

    fun detect(
        headers: List<String>,
        rows: List<List<String>>
    ): List<String> {
        val anomalies = mutableListOf<String>()
        
        for (header in headers) {
            val stats = statistics.calculateColumnStats(headers, rows, header)
            
            if (stats != null) {
                val columnIndex = headers.indexOf(header)
                
                val threshold = stats.standardDeviation * 2
                val upperBound = stats.mean + threshold
                val lowerBound = stats.mean - threshold
                
                for (rowIndex in rows.indices) {
                    val row = rows[rowIndex]
                    if (columnIndex < row.size) {
                        val value = row[columnIndex].toDoubleOrNull()
                        
                        if (value != null) {
                            if (value > upperBound) {
                                anomalies.add(
                                    "Fila ${rowIndex + 1}, Columna '$header': Valor $value es mayor que el limite superior ${"%.2f".format(upperBound)}"
                                )
                            } else if (value < lowerBound) {
                                anomalies.add(
                                    "Fila ${rowIndex + 1}, Columna '$header': Valor $value es menor que el limite inferior ${"%.2f".format(lowerBound)}"
                                )
                            }
                        }
                    }
                }
            }
        }
        
        detectDuplicateRows(headers, rows, anomalies)
        detectEmptyValues(headers, rows, anomalies)
        
        return anomalies
    }

    private fun detectDuplicateRows(
        headers: List<String>,
        rows: List<List<String>>,
        anomalies: MutableList<String>
    ) {
        val seenRows = mutableMapOf<String, Int>()
        
        for (rowIndex in rows.indices) {
            val rowKey = rows[rowIndex].joinToString("|")
            
            if (seenRows.containsKey(rowKey)) {
                anomalies.add(
                    "Fila ${rowIndex + 1} es duplicada de la fila ${seenRows[rowKey]!! + 1}"
                )
            } else {
                seenRows[rowKey] = rowIndex
            }
        }
    }

    private fun detectEmptyValues(
        headers: List<String>,
        rows: List<List<String>>,
        anomalies: MutableList<String>
    ) {
        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            
            if (row.all { it.isBlank() }) {
                anomalies.add("Fila ${rowIndex + 1} esta completamente vacia")
            }
            
            for (columnIndex in headers.indices) {
                if (columnIndex >= row.size || row[columnIndex].isBlank()) {
                    anomalies.add(
                        "Fila ${rowIndex + 1}, Columna '${headers[columnIndex]}': Valor vacio"
                    )
                }
            }
        }
    }

    fun getAnomalySummary(anomalies: List<String>): String {
        if (anomalies.isEmpty()) {
            return "No se detectaron anomalias en los datos."
        }
        
        val sb = StringBuilder()
        sb.appendLine("=== RESUMEN DE ANOMALIAS ===")
        sb.appendLine("Total de anomalias encontradas: ${anomalies.size}")
        sb.appendLine()
        
        val duplicateCount = anomalies.count { it.contains("duplicada") }
        val emptyCount = anomalies.count { it.contains("vacio") || it.contains("vacia") }
        val outlierCount = anomalies.count { it.contains("limite") }
        
        sb.appendLine("  - Valores atipicos: $outlierCount")
        sb.appendLine("  - Filas duplicadas: $duplicateCount")
        sb.appendLine("  - Valores vacios: $emptyCount")
        
        return sb.toString()
    }
}