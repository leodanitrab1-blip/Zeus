package com.zeus.suite.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.pdf.*
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class PDFModuleFragment : Fragment() {

    companion object {
        fun newInstance(): PDFModuleFragment = PDFModuleFragment()
    }

    private lateinit var fileManager: FileManager
    private lateinit var pdfMerger: PDFMerger
    private lateinit var pdfSplitter: PDFSplitter
    private lateinit var pdfSigner: PDFSigner
    private lateinit var pdfConverter: PDFConverter
    private var pendingAction: String = ""
    private val cancelOperation = AtomicBoolean(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult
        when (pendingAction) {
            "merge" -> {
                if (uris.size >= 2) showMergeOptions(uris)
                else toast("Seleccione al menos 2 PDFs")
            }
            "split" -> showSplitOptions(uris[0])
            "sign" -> showSignDialog(uris[0])
            "convert" -> showConvertOptions(uris)
        }
    }

    private val singleFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                when (pendingAction) {
                    "split" -> showSplitOptions(uri)
                    "sign" -> showSignDialog(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_pdf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileManager = FileManager(requireContext())
        pdfMerger = PDFMerger(requireContext())
        pdfSplitter = PDFSplitter(requireContext())
        pdfSigner = PDFSigner(requireContext())
        pdfConverter = PDFConverter(requireContext())
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardMergePDF)?.setOnClickListener {
            pendingAction = "merge"; openMultiFilePicker()
        }
        view.findViewById<View>(R.id.cardSplitPDF)?.setOnClickListener {
            pendingAction = "split"; openSingleFilePicker()
        }
        view.findViewById<View>(R.id.cardSignPDF)?.setOnClickListener {
            pendingAction = "sign"; openSingleFilePicker()
        }
        view.findViewById<View>(R.id.cardConvertPDF)?.setOnClickListener {
            pendingAction = "convert"
            try { filePickerLauncher.launch(arrayOf("image/*")) }
            catch (e: Exception) { toast("Error al abrir selector") }
        }
        view.findViewById<View>(R.id.cardCompressPDF)?.visibility = View.GONE
    }

    private fun openMultiFilePicker() {
        try { filePickerLauncher.launch(arrayOf("application/pdf")) }
        catch (e: Exception) { toast("Error al abrir selector") }
    }

    private fun openSingleFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        singleFileLauncher.launch(intent)
    }

    // ==================== PROGRESS DIALOG ====================

    private fun createProgressDialog(title: String): AlertDialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = false; max = 100; progress = 0
        }

        val percentText = TextView(requireContext()).apply {
            text = "0%"; textSize = 28f; setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }

        val statusText = TextView(requireContext()).apply {
            text = "Procesando..."; textSize = 14f; setTextColor(0xFF5C6BC0.toInt())
            gravity = android.view.Gravity.CENTER
        }

        val cancelBtn = Button(requireContext()).apply {
            text = "Cancelar"; setBackgroundColor(0xFFFF1744.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { cancelOperation.set(true); toast("Cancelando...") }
        }

        layout.addView(progressBar); layout.addView(percentText)
        layout.addView(statusText); layout.addView(cancelBtn)

        return AlertDialog.Builder(requireContext())
            .setTitle(title).setView(layout).setCancelable(false).create()
    }

    private fun updateProgress(dialog: AlertDialog, progress: Int, status: String) {
        requireActivity().runOnUiThread {
            try {
                val view = dialog.findViewById<View>(android.R.id.content) ?: return@runOnUiThread
                val layout = (view as? ViewGroup)?.getChildAt(0) as? LinearLayout ?: return@runOnUiThread
                (layout.getChildAt(0) as? ProgressBar)?.progress = progress
                (layout.getChildAt(1) as? TextView)?.text = "$progress%"
                (layout.getChildAt(2) as? TextView)?.text = status
            } catch (e: Exception) {}
        }
    }

    // ==================== UNIR PDFs ====================

    private fun showMergeOptions(uris: List<Uri>) {
        val fileNames = uris.map { getFileName(it) }
        
        val options = arrayOf(
            "Unir en orden secuencial",
            "Intercalar paginas (1-2-1-2)",
            "Unir como paquete comprimido"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Unir ${uris.size} PDFs")
            .setMessage("Archivos:\n${fileNames.joinToString("\n") { "• $it" }}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> mergeSequential(uris)
                    1 -> mergeAlternating(uris)
                    2 -> mergeAsPackage(uris)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mergeSequential(uris: List<Uri>) {
        cancelOperation.set(false)
        val dialog = createProgressDialog("Uniendo PDFs")
        dialog.show()

        Thread {
            try {
                val result = pdfMerger.mergePDFsWithProgress(uris, "unido_${System.currentTimeMillis()}.pdf") { p, s ->
                    if (cancelOperation.get()) throw InterruptedException()
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "PDFs unidos", true)
                    else toast("Error al unir")
                }
            } catch (e: InterruptedException) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Cancelado") }
            }
        }.start()
    }

    private fun mergeAlternating(uris: List<Uri>) {
        cancelOperation.set(false)
        val dialog = createProgressDialog("Intercalando PDFs")
        dialog.show()

        Thread {
            try {
                val result = pdfMerger.mergeAlternating(uris, "intercalado_${System.currentTimeMillis()}.pdf") { p, s ->
                    if (cancelOperation.get()) throw InterruptedException()
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "PDFs intercalados", true)
                    else toast("Error al intercalar")
                }
            } catch (e: InterruptedException) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Cancelado") }
            }
        }.start()
    }

    private fun mergeAsPackage(uris: List<Uri>) {
        cancelOperation.set(false)
        val dialog = createProgressDialog("Creando paquete")
        dialog.show()

        Thread {
            try {
                val result = pdfMerger.mergeAsPackage(uris, "paquete_${System.currentTimeMillis()}.pdf") { p, s ->
                    if (cancelOperation.get()) throw InterruptedException()
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "Paquete creado", true)
                    else toast("Error al crear paquete")
                }
            } catch (e: InterruptedException) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Cancelado") }
            }
        }.start()
    }

    // ==================== DIVIDIR PDF ====================

    private fun showSplitOptions(uri: Uri) {
        val pageCount = pdfSplitter.getPageCount(uri)
        if (pageCount <= 1) { toast("El PDF tiene solo 1 pagina"); return }

        val options = arrayOf(
            "Dividir en partes iguales",
            "Extraer rango de paginas",
            "Extraer paginas especificas",
            "Extraer paginas pares",
            "Extraer paginas impares"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Dividir: ${getFileName(uri)}")
            .setMessage("Total: $pageCount paginas")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDivideInParts(uri, pageCount)
                    1 -> showRangeSelector(uri, pageCount)
                    2 -> showPageNumberInput(uri, pageCount)
                    3 -> extractPages(uri, (0 until pageCount).filter { it % 2 == 1 }.toList(), "pares")
                    4 -> extractPages(uri, (0 until pageCount).filter { it % 2 == 0 }.toList(), "impares")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDivideInParts(uri: Uri, totalPages: Int) {
        val parts = arrayOf("2", "3", "4", "5", "10")
        AlertDialog.Builder(requireContext())
            .setTitle("Dividir en partes iguales")
            .setItems(parts) { _, which -> splitInParts(uri, parts[which].toInt(), totalPages) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRangeSelector(uri: Uri, totalPages: Int) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        val fromInput = EditText(requireContext()).apply {
            hint = "Desde (1-$totalPages)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val toInput = EditText(requireContext()).apply {
            hint = "Hasta (1-$totalPages)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(TextView(requireContext()).apply { text = "$totalPages paginas totales"; setPadding(0, 0, 0, 16) })
        layout.addView(fromInput); layout.addView(toInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Extraer rango").setView(layout)
            .setPositiveButton("Extraer") { _, _ ->
                val from = (fromInput.text.toString().toIntOrNull() ?: 1).coerceIn(1, totalPages)
                val to = (toInput.text.toString().toIntOrNull() ?: totalPages).coerceIn(from, totalPages)
                extractPages(uri, (from - 1 until to).toList(), "rango_${from}-${to}")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showPageNumberInput(uri: Uri, totalPages: Int) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        val input = EditText(requireContext()).apply {
            hint = "Ej: 1,5,10-15,20"; inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        layout.addView(TextView(requireContext()).apply {
            text = "$totalPages paginas\nUse comas y guiones"; setPadding(0, 0, 0, 12)
        })
        layout.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Extraer paginas especificas").setView(layout)
            .setPositiveButton("Extraer") { _, _ ->
                val pages = parsePageNumbers(input.text.toString(), totalPages)
                if (pages.isNotEmpty()) extractPages(uri, pages, "personalizado")
                else toast("Paginas invalidas")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun parsePageNumbers(input: String, totalPages: Int): List<Int> {
        val pages = mutableSetOf<Int>()
        for (part in input.split(",")) {
            val t = part.trim()
            if (t.contains("-")) {
                val r = t.split("-")
                if (r.size == 2) {
                    val s = r[0].trim().toIntOrNull() ?: continue
                    val e = r[1].trim().toIntOrNull() ?: continue
                    for (i in s..e) if (i in 1..totalPages) pages.add(i - 1)
                }
            } else {
                val p = t.toIntOrNull()
                if (p != null && p in 1..totalPages) pages.add(p - 1)
            }
        }
        return pages.toList().sorted()
    }

    private fun extractPages(uri: Uri, pages: List<Int>, label: String) {
        cancelOperation.set(false)
        val dialog = createProgressDialog("Extrayendo paginas")
        dialog.show()

        Thread {
            try {
                val result = pdfSplitter.splitPDFWithProgress(uri, pages, "dividido_${label}_${System.currentTimeMillis()}.pdf") { p, s ->
                    if (cancelOperation.get()) throw InterruptedException()
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "$label - ${pages.size} paginas", true)
                    else toast("Error al extraer")
                }
            } catch (e: InterruptedException) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Cancelado") }
            }
        }.start()
    }

    private fun splitInParts(uri: Uri, parts: Int, totalPages: Int) {
        cancelOperation.set(false)
        val dialog = createProgressDialog("Dividiendo en $parts partes")
        dialog.show()

        Thread {
            try {
                val results = pdfSplitter.splitPDFInPartsWithProgress(uri, parts) { p, s ->
                    if (cancelOperation.get()) throw InterruptedException()
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (results.isNotEmpty()) {
                        val msg = "${results.size} archivos creados:\n\n${results.joinToString("\n") { "• ${it.name}" }}"
                        AlertDialog.Builder(requireContext())
                            .setTitle("PDF dividido").setMessage(msg)
                            .setPositiveButton("Aceptar", null).show()
                    } else toast("Error al dividir")
                }
            } catch (e: InterruptedException) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Cancelado") }
            }
        }.start()
    }

    // ==================== FIRMAR PDF ====================

    private fun showSignDialog(uri: Uri) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }

        val signatureView = SignatureView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val clearBtn = Button(requireContext()).apply {
            text = "Limpiar"
            setOnClickListener { signatureView.clear() }
        }

        layout.addView(TextView(requireContext()).apply {
            text = "PDF: ${getFileName(uri)}\nDibuje su firma abajo:"
            setPadding(0, 0, 0, 12)
        })
        layout.addView(signatureView)
        layout.addView(clearBtn)

        AlertDialog.Builder(requireContext())
            .setTitle("Firmar PDF").setView(layout)
            .setPositiveButton("Aplicar Firma") { _, _ ->
                val bitmap = signatureView.getSignature()
                if (bitmap != null) applySignature(uri, bitmap)
                else toast("Dibuje una firma")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun applySignature(uri: Uri, signature: Bitmap) {
        val dialog = createProgressDialog("Firmando PDF")
        dialog.show()

        Thread {
            try {
                val result = pdfSigner.signPDFWithImage(uri, signature, "firmado_${System.currentTimeMillis()}.pdf") { p, s ->
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "PDF firmado", true)
                    else toast("Error al firmar")
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Error: ${e.message}") }
            }
        }.start()
    }

    // ==================== CONVERTIR PDF ====================

    private fun showConvertOptions(uris: List<Uri>) {
        val options = arrayOf(
            "Imagenes a PDF",
            "Crear PDF desde texto",
            "Fotos desde camara a PDF"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Convertir a PDF")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> convertImages(uris)
                    1 -> showTextToPdfDialog()
                    2 -> openCamera()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun convertImages(uris: List<Uri>) {
        val dialog = createProgressDialog("Creando PDF")
        dialog.show()

        Thread {
            try {
                val result = pdfConverter.imagesToPDFWithProgress(uris, "imagenes_${System.currentTimeMillis()}.pdf") { p, s ->
                    updateProgress(dialog, p, s)
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "PDF de imagenes", true)
                    else toast("Error al convertir")
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Error: ${e.message}") }
            }
        }.start()
    }

    private fun showTextToPdfDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Escriba el texto para el PDF..."
            minLines = 8; gravity = android.view.Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Crear PDF desde texto").setView(input)
            .setPositiveButton("Crear PDF") { _, _ ->
                val text = input.text.toString()
                if (text.isNotBlank()) createTextPdf(text) else toast("Ingrese texto")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun createTextPdf(text: String) {
        val dialog = createProgressDialog("Creando PDF")
        dialog.show()

        Thread {
            try {
                val result = pdfConverter.textToPDF(text, "texto_${System.currentTimeMillis()}.pdf")
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    if (result != null) showResultDialog(result, "PDF de texto", true)
                    else toast("Error")
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread { dialog.dismiss(); toast("Error: ${e.message}") }
            }
        }.start()
    }

    private fun openCamera() {
        toast("Camara - Abriendo...")
    }

    // ==================== RESULTADOS ====================

    private fun showResultDialog(file: File, title: String, showPreview: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Archivo: ${file.name}\nTamano: ${fileManager.formatFileSize(file.length())}\n\nGuardado en Download/ZeusSuite/")
            .setPositiveButton("Abrir") { _, _ -> openFile(file) }
            .setNeutralButton("Compartir") { _, _ -> shareFile(file) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun openFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        } catch (e: Exception) { toast("No hay app para abrir PDF") }
    }

    private fun shareFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartir PDF"))
        } catch (e: Exception) { toast("Error al compartir") }
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) { val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) name = it.getString(i) }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) { val i = it.getColumnIndex(OpenableColumns.SIZE); if (i >= 0) size = it.getLong(i) }
        }
        return size
    }

    private fun toast(msg: String) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
}

// ==================== SIGNATURE VIEW ====================

class SignatureView(context: android.content.Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.BLACK; strokeWidth = 8f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val path = android.graphics.Path()
    private val bitmapBuffer: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private var canvasBuffer = Canvas(bitmapBuffer)

    init {
        setBackgroundColor(Color.WHITE)
        post {
            val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            canvasBuffer = Canvas(bmp)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvasBuffer = Canvas(bmp)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> path.moveTo(x, y)
            MotionEvent.ACTION_MOVE -> path.lineTo(x, y)
            MotionEvent.ACTION_UP -> {
                canvasBuffer.drawPath(path, paint); path.reset()
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(
            if (canvasBuffer.width > 1) Bitmap.createBitmap(canvasBuffer.width, canvasBuffer.height, Bitmap.Config.ARGB_8888).also { Canvas(it).drawBitmap(canvasBuffer.let { Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.WHITE) } }, 0f, 0f, null) }
            else bitmapBuffer,
            0f, 0f, null
        )
        canvas.drawPath(path, paint)
    }

    fun clear() {
        path.reset()
        canvasBuffer.drawColor(Color.WHITE)
        invalidate()
    }

    fun getSignature(): Bitmap? {
        if (canvasBuffer.width <= 1) return null
        val bmp = Bitmap.createBitmap(canvasBuffer.width, canvasBuffer.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(canvasBuffer.let { Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.WHITE) } }, 0f, 0f, null)
        canvas.drawPath(path, paint)
        return bmp
    }
}