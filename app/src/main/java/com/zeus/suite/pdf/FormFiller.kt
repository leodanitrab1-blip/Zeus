package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class FormFiller(private val context: Context) {

    private val fileManager = FileManager(context)

    data class FormField(
        val name: String,
        val value: String
    )

    fun fillForm(uri: Uri, fields: List<FormField>): File? {
        val fileName = fileManager.getFileName(uri)
        val filledFileName = "rellenado_$fileName"
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return null
        val outputFile = fileManager.createOutputFile(filledFileName)

        try {
            var content = tempFile.readText()

            for (field in fields) {
                val placeholder = "{{${field.name}}}"
                content = content.replace(placeholder, field.value)
            }

            outputFile.writeText(content)
            fileManager.deleteTempFile(tempFile)
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        }
    }

    fun fillFormWithMap(uri: Uri, fieldMap: Map<String, String>): File? {
        val fields = fieldMap.map { FormField(it.key, it.value) }
        return fillForm(uri, fields)
    }

    fun getPlaceholders(uri: Uri): List<String> {
        val fileName = fileManager.getFileName(uri)
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return emptyList()

        val placeholders = mutableListOf<String>()
        try {
            val content = tempFile.readText()
            val regex = Regex("\\{\\{(.+?)\\}\\}")
            val matches = regex.findAll(content)
            
            for (match in matches) {
                val placeholder = match.groupValues[1]
                if (placeholder !in placeholders) {
                    placeholders.add(placeholder)
                }
            }

            fileManager.deleteTempFile(tempFile)
            return placeholders

        } catch (e: Exception) {
            e.printStackTrace()
            fileManager.deleteTempFile(tempFile)
            return emptyList()
        }
    }
}