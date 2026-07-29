package com.zeus.suite.bigdata

class DataFilter {

    data class FilterCondition(
        val columnIndex: Int,
        val operator: FilterOperator,
        val value: String
    )

    enum class FilterOperator(val label: String) {
        EQUALS("Igual a"),
        NOT_EQUALS("Diferente de"),
        CONTAINS("Contiene"),
        NOT_CONTAINS("No contiene"),
        STARTS_WITH("Empieza con"),
        ENDS_WITH("Termina con"),
        GREATER_THAN("Mayor que"),
        LESS_THAN("Menor que"),
        IS_EMPTY("Esta vacio"),
        IS_NOT_EMPTY("No esta vacio")
    }

    fun filter(data: CSVHandler.CSVData, conditions: List<FilterCondition>): CSVHandler.CSVData {
        if (conditions.isEmpty()) return data
        val filteredRows = data.rows.filter { row ->
            conditions.all { condition ->
                if (condition.columnIndex >= row.size) {
                    return@all condition.operator == FilterOperator.IS_EMPTY
                }
                evaluateCondition(row[condition.columnIndex], condition.operator, condition.value)
            }
        }
        return CSVHandler.CSVData(data.headers, filteredRows)
    }

    private fun evaluateCondition(cellValue: String, operator: FilterOperator, value: String): Boolean {
        return when (operator) {
            FilterOperator.EQUALS -> cellValue.equals(value, ignoreCase = true)
            FilterOperator.NOT_EQUALS -> !cellValue.equals(value, ignoreCase = true)
            FilterOperator.CONTAINS -> cellValue.contains(value, ignoreCase = true)
            FilterOperator.NOT_CONTAINS -> !cellValue.contains(value, ignoreCase = true)
            FilterOperator.STARTS_WITH -> cellValue.startsWith(value, ignoreCase = true)
            FilterOperator.ENDS_WITH -> cellValue.endsWith(value, ignoreCase = true)
            FilterOperator.GREATER_THAN -> {
                val a = cellValue.toDoubleOrNull()
                val b = value.toDoubleOrNull()
                a != null && b != null && a > b
            }
            FilterOperator.LESS_THAN -> {
                val a = cellValue.toDoubleOrNull()
                val b = value.toDoubleOrNull()
                a != null && b != null && a < b
            }
            FilterOperator.IS_EMPTY -> cellValue.isBlank()
            FilterOperator.IS_NOT_EMPTY -> cellValue.isNotBlank()
        }
    }

    fun getUniqueValues(data: CSVHandler.CSVData, columnIndex: Int): List<String> {
        val values = mutableSetOf<String>()
        for (row in data.rows) {
            if (columnIndex < row.size) values.add(row[columnIndex])
        }
        return values.toList().sorted()
    }
}