package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File

class FormFiller(private val context: Context) {

    private val fileManager = FileManager(context)

    data class FormField(val name: String, val value: String)

    fun fillForm(uri: Uri, fields: List<FormField>): File? {
        val fileName = fileManager.getFileName(uri)
        val outputFile = fileManager.createOutputFile("rellenado_$fileName")
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return null

        return try {
            var content = tempFile.readText()
            for (field in fields) {
                content = content.replace("{{${field.name}}}", field.value)
                content = content.replace("\${${field.name}}", field.value)
                content = content.replace("[$field.name]", field.value)
            }
            outputFile.writeText(content)
            fileManager.deleteTempFile(tempFile)
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    fun fillFormWithData(uri: Uri, data: List<Map<String, String>>, fieldMapping: Map<String, String>): List<File> {
        val results = mutableListOf<File>()
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return results

        try {
            val template = tempFile.readText()

            for ((index, row) in data.withIndex()) {
                var content = template
                for ((placeholder, columnName) in fieldMapping) {
                    val value = row[columnName] ?: ""
                    content = content.replace("{{$placeholder}}", value)
                    content = content.replace("\${$placeholder}", value)
                    content = content.replace("[$placeholder]", value)
                }

                val outputFile = fileManager.createOutputFile("${baseName}_${index + 1}.pdf")
                outputFile.writeText(content)
                results.add(outputFile)
            }

            fileManager.deleteTempFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
        }

        return results
    }

    fun extractPlaceholders(uri: Uri): List<String> {
        val fileName = fileManager.getFileName(uri)
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return emptyList()

        return try {
            val content = tempFile.readText()
            val placeholders = mutableSetOf<String>()

            val patterns = listOf(
                Regex("\\{\\{(.+?)\\}\\}"),
                Regex("\\\$\\{(.+?)\\}"),
                Regex("\\[(.+?)\\]")
            )

            for (pattern in patterns) {
                for (match in pattern.findAll(content)) {
                    placeholders.add(match.groupValues[1])
                }
            }

            fileManager.deleteTempFile(tempFile)
            placeholders.toList().sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            emptyList()
        }
    }
}