package com.zeus.suite.bigdata

class DataSearch {

    data class SearchResult(
        val rowIndex: Int,
        val columnIndex: Int,
        val value: String
    )

    fun search(
        headers: List<String>,
        rows: List<List<String>>,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            for (columnIndex in row.indices) {
                if (row[columnIndex].contains(query, ignoreCase = true)) {
                    results.add(
                        SearchResult(
                            rowIndex = rowIndex,
                            columnIndex = columnIndex,
                            value = row[columnIndex]
                        )
                    )
                }
            }
        }

        return results
    }

    fun searchInColumn(
        headers: List<String>,
        rows: List<List<String>>,
        columnName: String,
        query: String
    ): List<SearchResult> {
        val columnIndex = headers.indexOfFirst {
            it.equals(columnName, ignoreCase = true)
        }

        if (columnIndex == -1) return emptyList()

        val results = mutableListOf<SearchResult>()

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            if (columnIndex < row.size && row[columnIndex].contains(query, ignoreCase = true)) {
                results.add(
                    SearchResult(
                        rowIndex = rowIndex,
                        columnIndex = columnIndex,
                        value = row[columnIndex]
                    )
                )
            }
        }

        return results
    }

    fun searchExact(
        headers: List<String>,
        rows: List<List<String>>,
        query: String
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            for (columnIndex in row.indices) {
                if (row[columnIndex].equals(query, ignoreCase = true)) {
                    results.add(
                        SearchResult(
                            rowIndex = rowIndex,
                            columnIndex = columnIndex,
                            value = row[columnIndex]
                        )
                    )
                }
            }
        }

        return results
    }

    fun getRowByIndex(
        headers: List<String>,
        rows: List<List<String>>,
        rowIndex: Int
    ): Map<String, String>? {
        if (rowIndex < 0 || rowIndex >= rows.size) return null

        val row = rows[rowIndex]
        val result = mutableMapOf<String, String>()

        for (i in headers.indices) {
            val value = if (i < row.size) row[i] else ""
            result[headers[i]] = value
        }

        return result
    }

    fun countOccurrences(
        rows: List<List<String>>,
        columnIndex: Int,
        value: String
    ): Int {
        var count = 0
        for (row in rows) {
            if (columnIndex < row.size && row[columnIndex].equals(value, ignoreCase = true)) {
                count++
            }
        }
        return count
    }
}