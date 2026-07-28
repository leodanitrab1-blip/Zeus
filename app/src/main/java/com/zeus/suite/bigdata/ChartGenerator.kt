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

    data class ChartData(
        val label: String,
        val value: Float,
        val color: Int = Color.BLUE
    )

    fun generateBarChart(
        data: List<ChartData>,
        title: String,
        width: Int = 800,
        height: Int = 600
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 36f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
        }

        val maxValue = data.maxOfOrNull { it.value } ?: 1f
        val chartLeft = 100f
        val chartTop = 100f
        val chartRight = width - 50f
        val chartBottom = height - 80f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        canvas.drawText(title, width / 2f - 100f, 60f, titlePaint)

        val barWidth = chartWidth / data.size * 0.6f
        val barSpacing = chartWidth / data.size * 0.4f

        for (i in data.indices) {
            val item = data[i]
            val barHeight = (item.value / maxValue) * chartHeight
            val left = chartLeft + i * (barWidth + barSpacing) + barSpacing / 2
            val top = chartBottom - barHeight
            val right = left + barWidth
            val bottom = chartBottom

            val barPaint = Paint().apply {
                color = item.color
                style = Paint.Style.FILL
            }

            canvas.drawRect(left, top, right, bottom, barPaint)
            canvas.drawText(item.label, left, chartBottom + 40f, labelPaint)
        }

        return bitmap
    }

    fun generatePieChart(
        data: List<ChartData>,
        title: String,
        width: Int = 800,
        height: Int = 800
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 36f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val total = data.sumOf { it.value.toDouble() }.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 3f

        canvas.drawText(title, centerX - 100f, 60f, titlePaint)

        var startAngle = -90f
        for (item in data) {
            val sweepAngle = (item.value / total) * 360f

            val arcPaint = Paint().apply {
                color = item.color
                style = Paint.Style.FILL
            }

            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                true,
                arcPaint
            )

            startAngle += sweepAngle
        }

        return bitmap
    }

    fun saveChartAsImage(bitmap: Bitmap, fileName: String): File? {
        return try {
            val file = fileManager.createOutputFile("$fileName.png")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}