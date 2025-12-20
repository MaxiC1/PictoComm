package com.inacap.picto_comm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.adapters.PictogramaGestionAdapter
import com.inacap.picto_comm.ui.utils.IconoHelper
import com.inacap.picto_comm.ui.utils.PinHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Actividad para gestionar todos los pictogramas (editar/eliminar)
 * Solo accesible por usuarios PADRE con verificación de PIN
 */
class GestionarTodosPictogramasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerTodosPictogramas: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: PictogramaGestionAdapter

    // Para manejo de cámara
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var fotoCapturadaUri: Uri? = null
    private var nuevaFotoBase64: String? = null
    private var vistaPreviaTemp: ImageView? = null

    // Launcher para solicitar permisos de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamara(vistaPreviaTemp)
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestionar_todos_pictogramas)

        // Inicializar executor de cámara
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Verificar que sea usuario PADRE
        val sessionManager = (application as PictoCommApplication).sessionManager
        if (sessionManager.obtenerTipoUsuario() != TipoUsuario.PADRE.name) {
            Toast.makeText(this, "Solo el administrador puede acceder a esta pantalla", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarVistas()
        configurarToolbar()
        configurarRecyclerView()

        // Verificar PIN antes de continuar
        verificarPinYCargarDatos()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        recyclerTodosPictogramas = findViewById(R.id.recycler_todos_pictogramas)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configurarRecyclerView() {
        adapter = PictogramaGestionAdapter(
            onEditar = { pictograma ->
                mostrarDialogoEditar(pictograma)
            },
            onEliminar = { pictograma ->
                mostrarDialogoConfirmacionEliminar(pictograma)
            }
        )

        recyclerTodosPictogramas.apply {
            layoutManager = LinearLayoutManager(this@GestionarTodosPictogramasActivity)
            adapter = this@GestionarTodosPictogramasActivity.adapter
        }
    }

    /**
     * Verifica el PIN del padre antes de mostrar los datos
     */
    private fun verificarPinYCargarDatos() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                val padreId = firebaseRepository.auth.currentUser?.uid
                if (padreId == null) {
                    Toast.makeText(this@GestionarTodosPictogramasActivity,
                        "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val usuario = firebaseRepository.obtenerUsuario(padreId)
                val pinCorrecto = usuario?.pin ?: ""

                if (pinCorrecto.isEmpty()) {
                    // No tiene PIN configurado, ofrecer configurarlo o continuar
                    mostrarDialogoConfigurarPin(padreId)
                } else {
                    val pinVerificado = PinHelper.verificarPin(this@GestionarTodosPictogramasActivity, pinCorrecto)
                    if (pinVerificado) {
                        cargarTodosPictogramas()
                    } else {
                        Toast.makeText(this@GestionarTodosPictogramasActivity,
                            "Acceso denegado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@GestionarTodosPictogramasActivity,
                    "Error al verificar PIN: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Muestra un diálogo para configurar el PIN si no existe
     */
    private fun mostrarDialogoConfigurarPin(padreId: String) {
        AlertDialog.Builder(this)
            .setTitle("PIN no configurado")
            .setMessage("No tienes un PIN configurado. ¿Deseas configurar uno ahora para mayor seguridad?")
            .setPositiveButton("Configurar PIN") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    configurarPinYContinuar(padreId)
                }
            }
            .setNegativeButton("Continuar sin PIN") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this@GestionarTodosPictogramasActivity,
                    "Continuando sin PIN. Puedes configurarlo más tarde.",
                    Toast.LENGTH_SHORT).show()
                cargarTodosPictogramas()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Configura el PIN y continúa con la carga de datos
     */
    private suspend fun configurarPinYContinuar(padreId: String) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        val nuevoPin = PinHelper.solicitarConfiguracionPin(this)
        if (nuevoPin != null) {
            try {
                // Actualizar el PIN en Firestore
                firebaseRepository.firestore.collection("usuarios")
                    .document(padreId)
                    .update("pin", nuevoPin)
                    .await()

                Toast.makeText(this,
                    "PIN configurado exitosamente", Toast.LENGTH_SHORT).show()
                cargarTodosPictogramas()
            } catch (e: Exception) {
                Toast.makeText(this,
                    "Error al guardar PIN: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this,
                "Configuración de PIN cancelada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cargarTodosPictogramas() {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        progressBar.visibility = View.VISIBLE
        recyclerTodosPictogramas.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            var primeraVez = true
            try {
                firebaseRepository.obtenerTodosPictogramas().collect { pictogramas ->
                    if (primeraVez) {
                        progressBar.visibility = View.GONE
                        primeraVez = false
                    }

                    if (pictogramas.isEmpty()) {
                        recyclerTodosPictogramas.visibility = View.GONE
                        layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        recyclerTodosPictogramas.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                        adapter.submitList(pictogramas)
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@GestionarTodosPictogramasActivity,
                        "Error al cargar pictogramas: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarDialogoEditar(pictograma: PictogramaSimple) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_pictograma, null)

        val etTexto = dialogView.findViewById<EditText>(R.id.et_texto_pictograma)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinner_categoria)
        val ivVistaPrevia = dialogView.findViewById<ImageView>(R.id.iv_vista_previa_editar)
        val btnCambiarFoto = dialogView.findViewById<MaterialButton>(R.id.btn_cambiar_foto)

        // Configurar valores actuales
        etTexto.setText(pictograma.texto)

        // Configurar spinner de categorías
        val categorias = Categoria.values()
        val categoriasNombres = categorias.map { it.nombreMostrar }
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasNombres)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner
        spinnerCategoria.setSelection(categorias.indexOf(pictograma.categoria))

        // Mostrar imagen actual
        if (pictograma.tipoImagen == TipoImagen.FOTO && pictograma.urlImagen.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(pictograma.urlImagen, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivVistaPrevia.setImageBitmap(bitmap)
                ivVistaPrevia.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                val iconoResId = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
                ivVistaPrevia.setImageResource(iconoResId)
            }
        } else {
            val iconoResId = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
            ivVistaPrevia.setImageResource(iconoResId)
        }

        // Resetear foto capturada
        nuevaFotoBase64 = null

        // Configurar botón cambiar foto
        btnCambiarFoto.setOnClickListener {
            verificarPermisoYAbrirCamara(ivVistaPrevia)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Pictograma")
            .setView(dialogView)
            .setPositiveButton("Guardar", null) // Configurar después
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Configurar listener del botón positivo después de crear el diálogo
        dialog.setOnShowListener {
            val guardarButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            guardarButton.setOnClickListener {
                val nuevoTexto = etTexto.text.toString()
                val nuevaCategoria = categorias[spinnerCategoria.selectedItemPosition]

                if (nuevoTexto.isBlank()) {
                    Toast.makeText(this, "El texto no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                editarPictograma(pictograma, nuevoTexto, nuevaCategoria, nuevaFotoBase64)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun editarPictograma(
        pictograma: PictogramaSimple,
        nuevoTexto: String,
        nuevaCategoria: Categoria,
        nuevaFotoBase64: String?
    ) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                val pictogramaActualizado = if (nuevaFotoBase64 != null) {
                    // Actualizar con nueva foto
                    pictograma.copy(
                        texto = nuevoTexto,
                        categoria = nuevaCategoria,
                        tipoImagen = TipoImagen.FOTO,
                        urlImagen = nuevaFotoBase64,
                        recursoImagen = ""
                    )
                } else {
                    // Actualizar solo texto y categoría
                    pictograma.copy(
                        texto = nuevoTexto,
                        categoria = nuevaCategoria
                    )
                }

                firebaseRepository.actualizarPictograma(pictogramaActualizado)
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Pictograma actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Error al actualizar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarDialogoConfirmacionEliminar(pictograma: PictogramaSimple) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Pictograma")
            .setMessage("¿Estás seguro de que deseas eliminar \"${pictograma.texto}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { dialog, _ ->
                eliminarPictograma(pictograma)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun eliminarPictograma(pictograma: PictogramaSimple) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository

        lifecycleScope.launch {
            try {
                firebaseRepository.eliminarPictograma(pictograma.id)
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Pictograma eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Error al eliminar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ==================== FUNCIONES DE CÁMARA ====================

    private fun verificarPermisoYAbrirCamara(vistaPrevia: ImageView) {
        vistaPreviaTemp = vistaPrevia
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara(vistaPrevia)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCamara(vistaPrevia: ImageView? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_camera, null)
        val previewView = dialogView.findViewById<PreviewView>(R.id.preview_view)
        val btnCapturar = dialogView.findViewById<MaterialButton>(R.id.btn_capturar)
        val btnCerrar = dialogView.findViewById<MaterialButton>(R.id.btn_cerrar_camara)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

        btnCapturar.setOnClickListener {
            capturarFoto(dialog, vistaPrevia)
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun capturarFoto(dialog: AlertDialog, vistaPrevia: ImageView?) {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalCacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    fotoCapturadaUri = Uri.fromFile(photoFile)
                    runOnUiThread {
                        procesarYMostrarFoto(fotoCapturadaUri!!, vistaPrevia)
                        dialog.dismiss()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@GestionarTodosPictogramasActivity,
                            "Error al capturar foto: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun procesarYMostrarFoto(uri: Uri, vistaPrevia: ImageView?) {
        lifecycleScope.launch {
            val base64 = comprimirImagenABase64(uri)
            if (base64 != null) {
                nuevaFotoBase64 = base64

                // Mostrar en vista previa
                vistaPrevia?.let {
                    try {
                        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        it.setImageBitmap(bitmap)
                        it.scaleType = ImageView.ScaleType.CENTER_CROP
                    } catch (e: Exception) {
                        android.util.Log.e("GestionarTodos", "Error al mostrar vista previa", e)
                    }
                }

                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Foto capturada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@GestionarTodosPictogramasActivity,
                    "Error al procesar la imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun comprimirImagenABase64(uri: Uri): String? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Leer orientación EXIF y rotar si es necesario
            try {
                val exifInputStream = contentResolver.openInputStream(uri)
                val exif = ExifInterface(exifInputStream!!)
                exifInputStream.close()

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                bitmap = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } catch (e: Exception) {
                android.util.Log.w("GestionarTodos", "No se pudo leer EXIF", e)
            }

            // Redimensionar
            val maxDimension = 800
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height,
                1.0f
            )

            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Comprimir
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            bitmap.recycle()
            resizedBitmap.recycle()

            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("GestionarTodos", "Error al comprimir imagen", e)
            return null
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotatedBitmap = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
        if (rotatedBitmap != source) {
            source.recycle()
        }
        return rotatedBitmap
    }
}
