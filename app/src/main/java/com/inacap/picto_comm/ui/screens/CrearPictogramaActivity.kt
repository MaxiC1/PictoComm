package com.inacap.picto_comm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.ui.utils.IconoHelper
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Actividad para crear nuevos pictogramas
 * Accesible tanto para PADRE como para HIJO
 *
 * - PADRE: Los pictogramas se aprueban automáticamente
 * - HIJO: Los pictogramas quedan pendientes de aprobación
 */
class CrearPictogramaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etTexto: TextInputEditText
    private lateinit var btnCategoria: MaterialButton
    private lateinit var btnSeleccionarIcono: MaterialButton
    private lateinit var btnUsarCamara: MaterialButton
    private lateinit var btnCrear: MaterialButton
    private lateinit var ivVistaPrevia: ImageView

    private var categoriaSeleccionada: Categoria? = null
    private var iconoSeleccionado: String? = null
    private var fotoCapturadaUri: Uri? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Launcher para solicitar permisos de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Iconos predefinidos disponibles para seleccionar
    private val iconosDisponibles = listOf(
        "ic_person_yo" to "Persona",
        "ic_action_want" to "Acción",
        "ic_thing_icecream" to "Objeto",
        "ic_quality_happy" to "Cualidad",
        "ic_place_home" to "Lugar",
        "ic_time_now" to "Tiempo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_pictograma)

        cameraExecutor = Executors.newSingleThreadExecutor()

        inicializarVistas()
        configurarToolbar()
        configurarListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Comprime una imagen y la convierte a Base64 para guardar en Firestore
     * @param uri URI de la imagen
     * @return String en Base64 o null si hay error
     */
    private fun comprimirImagenABase64(uri: Uri): String? {
        try {
            // Leer la imagen desde el URI
            val inputStream = contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                android.util.Log.e("CrearPictograma", "No se pudo decodificar la imagen")
                return null
            }

            // Leer orientación EXIF y rotar si es necesario
            try {
                val exifInputStream = contentResolver.openInputStream(uri)
                val exif = ExifInterface(exifInputStream!!)
                exifInputStream.close()

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                android.util.Log.d("CrearPictograma", "Orientación EXIF: $orientation")

                bitmap = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> {
                        android.util.Log.d("CrearPictograma", "Rotando 90 grados")
                        rotateBitmap(bitmap, 90f)
                    }
                    ExifInterface.ORIENTATION_ROTATE_180 -> {
                        android.util.Log.d("CrearPictograma", "Rotando 180 grados")
                        rotateBitmap(bitmap, 180f)
                    }
                    ExifInterface.ORIENTATION_ROTATE_270 -> {
                        android.util.Log.d("CrearPictograma", "Rotando 270 grados")
                        rotateBitmap(bitmap, 270f)
                    }
                    else -> bitmap
                }
            } catch (e: Exception) {
                android.util.Log.w("CrearPictograma", "No se pudo leer EXIF, continuando sin rotación", e)
            }

            // Calcular nuevo tamaño manteniendo aspect ratio (máximo 800x800)
            val maxDimension = 800
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height,
                1.0f // No agrandar imágenes pequeñas
            )

            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()

            android.util.Log.d("CrearPictograma", "Redimensionando de ${bitmap.width}x${bitmap.height} a ${newWidth}x${newHeight}")

            // Redimensionar
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Comprimir a JPEG con calidad 75%
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()

            // Liberar recursos
            bitmap.recycle()
            resizedBitmap.recycle()

            // Convertir a Base64
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            android.util.Log.d("CrearPictograma", "Imagen comprimida: ${byteArray.size} bytes, Base64: ${base64String.length} caracteres")

            return base64String
        } catch (e: Exception) {
            android.util.Log.e("CrearPictograma", "Error al comprimir imagen", e)
            return null
        }
    }

    /**
     * Rota un bitmap según el ángulo especificado
     */
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotatedBitmap = Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
        // Liberar el bitmap original si es diferente del rotado
        if (rotatedBitmap != source) {
            source.recycle()
        }
        return rotatedBitmap
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        etTexto = findViewById(R.id.et_texto)
        btnCategoria = findViewById(R.id.btn_categoria)
        btnSeleccionarIcono = findViewById(R.id.btn_seleccionar_icono)
        btnUsarCamara = findViewById(R.id.btn_usar_camara)
        btnCrear = findViewById(R.id.btn_crear)
        ivVistaPrevia = findViewById(R.id.iv_vista_previa)
    }

    private fun configurarToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configurarListeners() {
        btnCategoria.setOnClickListener {
            mostrarSelectorCategoria()
        }

        btnSeleccionarIcono.setOnClickListener {
            mostrarSelectorIcono()
        }

        btnUsarCamara.setOnClickListener {
            mostrarMensajeCamara()
        }

        btnCrear.setOnClickListener {
            crearPictograma()
        }
    }

    private fun mostrarSelectorCategoria() {
        val categorias = Categoria.values()
        val nombres = categorias.map { it.nombreMostrar }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.seleccionar_categoria))
            .setItems(nombres) { dialog, which ->
                categoriaSeleccionada = categorias[which]
                btnCategoria.text = categorias[which].nombreMostrar
                dialog.dismiss()
            }
            .show()
    }

    private fun mostrarSelectorIcono() {
        val nombres = iconosDisponibles.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.seleccionar_icono))
            .setItems(nombres) { dialog, which ->
                iconoSeleccionado = iconosDisponibles[which].first

                // Actualizar vista previa
                val iconoRes = IconoHelper.obtenerIconoParaPictograma(iconoSeleccionado!!)
                ivVistaPrevia.setImageResource(iconoRes)

                btnSeleccionarIcono.text = "Icono: ${iconosDisponibles[which].second}"
                dialog.dismiss()
            }
            .show()
    }

    private fun mostrarMensajeCamara() {
        // Verificar permiso de cámara
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCamara() {
        // Crear diálogo personalizado para la cámara
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
            capturarFoto(dialog)
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun capturarFoto(dialog: AlertDialog) {
        val imageCapture = imageCapture ?: return

        // Crear archivo temporal para la foto
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
                        // Actualizar vista previa con la foto capturada
                        ivVistaPrevia.setImageURI(fotoCapturadaUri)
                        iconoSeleccionado = null // Limpiar selección de icono
                        btnSeleccionarIcono.text = getString(R.string.seleccionar_icono)
                        Toast.makeText(
                            this@CrearPictogramaActivity,
                            "Foto capturada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@CrearPictogramaActivity,
                            "Error al capturar foto: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun crearPictograma() {
        // Validar campos
        val texto = etTexto.text?.toString()?.trim()
        if (texto.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_texto_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionada == null) {
            Toast.makeText(this, getString(R.string.error_categoria_no_seleccionada), Toast.LENGTH_SHORT).show()
            return
        }

        // Validar que se haya seleccionado un icono O capturado una foto
        if (iconoSeleccionado == null && fotoCapturadaUri == null) {
            Toast.makeText(this, "Debes seleccionar un icono o capturar una foto", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener información del usuario
        val sessionManager = (application as PictoCommApplication).sessionManager
        val tipoUsuario = sessionManager.obtenerTipoUsuario()
        val usuarioId = sessionManager.obtenerUsuarioId()

        // Determinar si el pictograma se aprueba automáticamente
        val aprobadoAutomaticamente = tipoUsuario == TipoUsuario.PADRE.name

        lifecycleScope.launch {
            try {
                // Si se capturó una foto, comprimirla y convertirla a Base64
                if (fotoCapturadaUri != null) {
                    Toast.makeText(
                        this@CrearPictogramaActivity,
                        "Procesando imagen...",
                        Toast.LENGTH_SHORT
                    ).show()

                    android.util.Log.d("CrearPictograma", "Comprimiendo imagen desde URI: $fotoCapturadaUri")

                    // Comprimir y convertir a Base64
                    val imagenBase64 = comprimirImagenABase64(fotoCapturadaUri!!)

                    if (imagenBase64 == null) {
                        Toast.makeText(
                            this@CrearPictogramaActivity,
                            "Error al procesar la imagen. Intenta nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // Verificar tamaño (Firestore tiene límite de 1MB por documento)
                    val sizeInKB = imagenBase64.length / 1024
                    android.util.Log.d("CrearPictograma", "Tamaño de Base64: $sizeInKB KB")

                    if (sizeInKB > 800) {
                        Toast.makeText(
                            this@CrearPictogramaActivity,
                            "La imagen es demasiado grande (${sizeInKB}KB). Intenta con otra imagen más pequeña.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    val timestamp = System.currentTimeMillis()

                    // Crear pictograma con la imagen en Base64
                    val nuevoPictograma = PictogramaSimple(
                        id = "0",
                        texto = texto,
                        categoria = categoriaSeleccionada!!,
                        recursoImagen = "",
                        esFavorito = false,
                        aprobado = aprobadoAutomaticamente,
                        creadoPor = usuarioId.toString(),
                        tipoImagen = TipoImagen.FOTO,
                        urlImagen = imagenBase64, // Guardamos el Base64 en lugar de URL
                        fechaCreacion = timestamp
                    )

                    android.util.Log.d("CrearPictograma", "Pictograma creado con imagen Base64")
                    guardarPictogramaEnFirestore(nuevoPictograma, aprobadoAutomaticamente)

                } else {
                    // Crear pictograma con icono
                    val nuevoPictograma = PictogramaSimple(
                        id = "0",
                        texto = texto,
                        categoria = categoriaSeleccionada!!,
                        recursoImagen = iconoSeleccionado!!,
                        esFavorito = false,
                        aprobado = aprobadoAutomaticamente,
                        creadoPor = usuarioId.toString(),
                        tipoImagen = TipoImagen.ICONO,
                        urlImagen = "",
                        fechaCreacion = System.currentTimeMillis()
                    )

                    guardarPictogramaEnFirestore(nuevoPictograma, aprobadoAutomaticamente)
                }
            } catch (e: Exception) {
                android.util.Log.e("CrearPictograma", "Error al crear pictograma", e)
                Toast.makeText(
                    this@CrearPictogramaActivity,
                    "Error al crear pictograma: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun guardarPictogramaEnFirestore(
        pictograma: PictogramaSimple,
        aprobadoAutomaticamente: Boolean
    ) {
        val firebaseRepository = (application as PictoCommApplication).firebaseRepository
        lifecycleScope.launch {
            try {
                firebaseRepository.crearPictograma(pictograma)

                if (aprobadoAutomaticamente) {
                    Toast.makeText(
                        this@CrearPictogramaActivity,
                        getString(R.string.pictograma_creado),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@CrearPictogramaActivity,
                        getString(R.string.pictograma_pendiente_aprobacion),
                        Toast.LENGTH_LONG
                    ).show()
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CrearPictogramaActivity,
                    "Error al guardar pictograma: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
