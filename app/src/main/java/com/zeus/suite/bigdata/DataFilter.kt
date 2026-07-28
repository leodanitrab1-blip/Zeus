package com.zeus.suite.bigdata

class DataFilter {

    data class FilterCondition(
        val columnIndex: Int,
        val operator: FilterOperator,
        val value: String
    )

    enum class FilterOperator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        GREATER_THAN,
        LESS_THAN,
        STARTS_WITH,
        ENDS_WITH
    }

    fun filter(
        headers: List<String>,
        rows: List<List<String>>,
        conditions: List<FilterCondition>
    ): List<List<String>> {
        return rows.filter { row ->
            conditions.all { condition ->
                if (condition.columnIndex >= row.size) return@all false
                val cellValue = row[condition.columnIndex]
                evaluateCondition(cellValue, condition.operator, condition.value)
            }
        }
    }

    private fun evaluateCondition(
        cellValue: String,
        operator: FilterOperator,
        value: String
    ): Boolean {
        return when (operator) {
            FilterOperator.EQUALS -> cellValue.equals(value, ignoreCase = true)
            FilterOperator.NOT_EQUALS -> !cellValue.equals(value, ignoreCase = true)
            FilterOperator.CONTAINS -> cellValue.contains(value, ignoreCase = true)
            FilterOperator.GREATER_THAN -> {
                try {
                    cellValue.toDouble() > value.toDouble()
                } catch (e: NumberFormatException) {
                    false
                }
            }
            FilterOperator.LESS_THAN -> {
                try {
                    cellValue.toDouble() < value.toDouble()
                } catch (e: NumberFormatException) {
                    false
                }
            }
            FilterOperator.STARTS_WITH -> cellValue.startsWith(value, ignoreCase = true)
            FilterOperator.ENDS_WITH -> cellValue.endsWith(value, ignoreCase = true)
        }
    }

    fun filterByColumnValue(
        headers: List<String>,
        rows: List<List<String>>,
        columnName: String,
        value: String
    ): List<List<String>> {
        val columnIndex = headers.indexOfFirst {
            it.equals(columnName, ignoreCase = true)
        }

        if (columnIndex == -1) return emptyList()

        return filter(
            headers,
            rows,
            listOf(FilterCondition(columnIndex, FilterOperator.EQUALS, value))
        )
    }

    fun getUniqueValues(
        rows: List<List<String>>,
        columnIndex: Int
    ): List<String> {
        val uniqueValues = mutableSetOf<String>()
        for (row in rows) {
            if (columnIndex < row.size) {
                uniqueValues.add(row[columnIndex])
            }
        }
        return uniqueValues.toList().sorted()
    }
}