package com.zeus.suite.ai

import com.zeus.suite.bigdata.Statistics

class ReportGenerator {

    private val statistics = Statistics()

    fun generateReport(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("=" .repeat(50))
        sb.appendLine("REPORTE EJECUTIVO")
        sb.appendLine("=" .repeat(50))
        sb.appendLine()
        
        sb.appendLine("Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine()
        
        sb.appendLine("-".repeat(50))
        sb.appendLine("1. RESUMEN GENERAL")
        sb.appendLine("-".repeat(50))
        sb.appendLine("Total de registros: ${rows.size}")
        sb.appendLine("Total de columnas: ${headers.size}")
        sb.appendLine("Columnas: ${headers.joinToString(", ")}")
        sb.appendLine()
        
        sb.appendLine("-".repeat(50))
        sb.appendLine("2. ANALISIS ESTADISTICO")
        sb.appendLine("-".repeat(50))
        
        val allStats = statistics.calculateAllStats(headers, rows)
        
        if (allStats.isNotEmpty()) {
            for (stat in allStats) {
                sb.appendLine("  ${stat.columnName}:")
                sb.appendLine("    - Media: ${"%.2f".format(stat.mean)}")
                sb.appendLine("    - Mediana: ${"%.2f".format(stat.median)}")
                sb.appendLine("    - Min: ${"%.2f".format(stat.min)}")
                sb.appendLine("    - Max: ${"%.2f".format(stat.max)}")
                sb.appendLine()
            }
        } else {
            sb.appendLine("  No se encontraron columnas numericas para analizar.")
            sb.appendLine()
        }
        
        sb.appendLine("-".repeat(50))
        sb.appendLine("3. DISTRIBUCION DE DATOS")
        sb.appendLine("-".repeat(50))
        
        for (header in headers) {
            val columnIndex = headers.indexOf(header)
            val frequency = statistics.getFrequencyDistribution(rows, columnIndex)
            
            if (frequency.isNotEmpty() && frequency.size <= 10) {
                sb.appendLine("  $header:")
                for ((value, count) in frequency) {
                    val percentage = (count.toDouble() / rows.size) * 100
                    sb.appendLine("    - $value: $count (${"%.1f".format(percentage)}%)")
                }
                sb.appendLine()
            }
        }
        
        sb.appendLine("-".repeat(50))
        sb.appendLine("4. CONCLUSIONES")
        sb.appendLine("-".repeat(50))
        sb.appendLine(generateConclusions(headers, rows))
        sb.appendLine()
        
        sb.appendLine("=" .repeat(50))
        sb.appendLine("FIN DEL REPORTE")
        sb.appendLine("=" .repeat(50))
        
        return sb.toString()
    }

    private fun generateConclusions(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val sb = StringBuilder()
        val allStats = statistics.calculateAllStats(headers, rows)
        
        if (allStats.isEmpty()) {
            return "No hay suficientes datos numericos para generar conclusiones."
        }
        
        val maxStat = allStats.maxByOrNull { it.max }
        val minStat = allStats.minByOrNull { it.min }
        
        if (maxStat != null) {
            sb.appendLine("  - El valor maximo se encuentra en '${maxStat.columnName}' con ${"%.2f".format(maxStat.max)}")
        }
        
        if (minStat != null) {
            sb.appendLine("  - El valor minimo se encuentra en '${minStat.columnName}' con ${"%.2f".format(minStat.min)}")
        }
        
        sb.appendLine("  - Se analizaron ${rows.size} registros en total.")
        sb.appendLine("  - El dataset contiene ${headers.size} columnas de informacion.")
        
        return sb.toString()
    }
}