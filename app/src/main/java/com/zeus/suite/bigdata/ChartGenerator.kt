package com.zeus.suite.bigdata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class ChartGenerator(private val context: Context) {

    private val fileManager = FileManager(context)

    private val colors = listOf(
        Color.parseColor("#1565C0"),
        Color.parseColor("#FFD600"),
        Color.parseColor("#00C853"),
        Color.parseColor("#FF1744"),
        Color.parseColor("#2979FF"),
        Color.parseColor("#FF6D00"),
        Color.parseColor("#AA00FF"),
        Color.parseColor("#00BFA5")
    )

    fun generateBarChart(labels: List<String>, values: List<Float>, title: String, width: Int = 900, height: Int = 600): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 32f; isFakeBoldText = true; isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.BLACK; textSize = 18f; isAntiAlias = true
        }

        canvas.drawText(title, width / 2f - 80f, 50f, titlePaint)
        if (values.isEmpty()) return bitmap

        val maxValue = values.maxOrNull() ?: 1f
        val chartLeft = 120f; val chartTop = 80f
        val chartRight = width - 40f; val chartBottom = height - 80f
        val chartWidth = chartRight - chartLeft; val chartHeight = chartBottom - chartTop
        val barWidth = chartWidth / values.size * 0.6f
        val barSpacing = chartWidth / values.size

        for (i in values.indices) {
            val barHeight = (values[i] / maxValue) * chartHeight
            val left = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2
            val top = chartBottom - barHeight
            val right = left + barWidth

            val barPaint = Paint().apply {
                color = colors[i % colors.size]; style = Paint.Style.FILL
            }
            canvas.drawRect(left, top, right, chartBottom, barPaint)

            val valueText = if (values[i] == values[i].toLong().toFloat()) values[i].toLong().toString() else "%.1f".format(values[i])
            canvas.drawText(valueText, left, top - 10f, labelPaint)

            canvas.save()
            canvas.rotate(-45f, left + barWidth / 2, chartBottom + 30f)
            val displayLabel = if (labels[i].length > 10) labels[i].take(10) + "..." else labels[i]
            canvas.drawText(displayLabel, left, chartBottom + 40f, labelPaint)
            canvas.restore()
        }
        return bitmap
    }

    fun generatePieChart(labels: List<String>, values: List<Float>, title: String, width: Int = 800, height: Int = 800): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 32f; isFakeBoldText = true; isAntiAlias = true
        }
        val legendPaint = Paint().apply {
            color = Color.BLACK; textSize = 20f; isAntiAlias = true
        }

        canvas.drawText(title, width / 2f - 80f, 50f, titlePaint)
        if (values.isEmpty() || values.sum() == 0f) return bitmap

        val total = values.sum()
        val centerX = width / 2f; val centerY = height / 2f + 20f
        val radius = minOf(width, height) / 3f
        var startAngle = -90f

        for (i in values.indices) {
            val sweepAngle = (values[i] / total) * 360f
            val arcPaint = Paint().apply {
                color = colors[i % colors.size]; style = Paint.Style.FILL; isAntiAlias = true
            }
            canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, startAngle, sweepAngle, true, arcPaint)
            startAngle += sweepAngle
        }

        var legendY = centerY + radius + 40f
        for (i in values.indices) {
            if (legendY > height - 20f) break
            val lp = Paint().apply { color = colors[i % colors.size]; style = Paint.Style.FILL }
            canvas.drawRect(40f, legendY - 12f, 70f, legendY + 8f, lp)
            val percent = if (total > 0) (values[i] / total * 100).toInt() else 0
            val dl = if (labels[i].length > 15) labels[i].take(15) + "..." else labels[i]
            canvas.drawText("$dl ($percent%)", 80f, legendY + 5f, legendPaint)
            legendY += 35f
        }
        return bitmap
    }

    fun saveChart(bitmap: Bitmap, fileName: String): File? {
        return try {
            val file = fileManager.createOutputFile("$fileName.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            file
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }
}