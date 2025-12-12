package com.inacap.picto_comm.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.inacap.picto_comm.MainActivity
import com.inacap.picto_comm.PictoCommApplication
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.repository.FirebaseMigrationHelper
import com.inacap.picto_comm.ui.utils.PinHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Pantalla de autenticación con Google Sign-In
 *
 * Reemplaza el sistema de PIN con autenticación de Google para padres
 */
class GoogleSignInActivity : AppCompatActivity() {

    private lateinit var btnGoogleSignIn: Button
    private lateinit var btnCancelar: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "GoogleSignInActivity"
    }

    // ActivityResultLauncher para Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d(TAG, "✅ Cuenta de Google obtenida: ${account.email}")
                firebaseAuthWithGoogle(account)
            } else {
                showLoading(false)
                Toast.makeText(this, "Error: No se pudo obtener la cuenta de Google", Toast.LENGTH_LONG).show()
                Log.e(TAG, "❌ Cuenta de Google es null")
            }
        } catch (e: ApiException) {
            showLoading(false)
            Log.e(TAG, "❌ Google Sign-In falló - Código: ${e.statusCode}", e)

            val errorMessage = when (e.statusCode) {
                10 -> {
                    "Error de configuración:\n" +
                    "• Verifica que el SHA-1 esté registrado en Firebase Console\n" +
                    "• Verifica que google-services.json esté actualizado\n" +
                    "• Código de error: 10"
                }
                12501 -> "Inicio de sesión cancelado por el usuario"
                7 -> "Error de red. Verifica tu conexión a Internet"
                else -> "Error al iniciar sesión con Google\nCódigo: ${e.statusCode}\nMensaje: ${e.message}"
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_signin)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configurar Firestore para funcionar online
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)  // Desactivar cache offline temporalmente
            .build()
        firestore.firestoreSettings = settings

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Inicializar vistas
        btnGoogleSignIn = findViewById(R.id.btn_google_signin)
        btnCancelar = findViewById(R.id.btn_cancelar)
        progressBar = findViewById(R.id.progress_bar)

        // Configurar botones
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    /**
     * Inicia el flujo de Google Sign-In
     */
    private fun signInWithGoogle() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    /**
     * Autentica con Firebase usando las credenciales de Google
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
        Log.d(TAG, "ID Token presente: ${account.idToken != null}")

        if (account.idToken == null) {
            showLoading(false)
            Toast.makeText(this, "Error: No se obtuvo el token de Google. Verifica la configuración de Firebase.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "ID Token es null - verifica el Web Client ID en strings.xml")
            return
        }

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Intentando autenticar con Firebase...")
                // Autenticar con Firebase
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    Log.d(TAG, "✅ Autenticación exitosa - UID: ${user.uid}")
                    Log.d(TAG, "Nombre: ${user.displayName}, Email: ${user.email}")

                    try {
                        // Crear o actualizar usuario en Firestore
                        Log.d(TAG, "Creando/actualizando usuario en Firestore...")
                        createOrUpdateUserInFirestore(
                            uid = user.uid,
                            nombre = user.displayName ?: "Usuario",
                            email = user.email ?: "",
                            photoUrl = user.photoUrl?.toString()
                        )
                        Log.d(TAG, "✅ Usuario creado/actualizado en Firestore")

                        // Guardar sesión
                        val sessionManager = (application as PictoCommApplication).sessionManager
                        sessionManager.guardarUsuarioActivo(
                            userId = user.uid.hashCode().toLong(),
                            nombre = user.displayName ?: "Usuario",
                            tipo = TipoUsuario.PADRE.name
                        )
                        sessionManager.guardarEmail(user.email ?: "")
                        Log.d(TAG, "✅ Sesión guardada")

                        Toast.makeText(this@GoogleSignInActivity,
                            "Bienvenido/a ${user.displayName}", Toast.LENGTH_SHORT).show()

                        // Navegar a MainActivity
                        val intent = Intent(this@GoogleSignInActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                    } catch (firestoreError: Exception) {
                        Log.e(TAG, "❌ Error al crear usuario en Firestore", firestoreError)
                        showLoading(false)
                        Toast.makeText(this@GoogleSignInActivity,
                            "Error al guardar datos: ${firestoreError.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this@GoogleSignInActivity,
                        "Error: Usuario de Firebase es null", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "❌ Usuario de Firebase es null después de autenticación")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en autenticación con Firebase", e)
                Log.e(TAG, "Tipo de error: ${e.javaClass.simpleName}")
                Log.e(TAG, "Mensaje: ${e.message}")
                e.printStackTrace()
                showLoading(false)
                Toast.makeText(this@GoogleSignInActivity,
                    "Error de autenticación: ${e.message}\n\nRevisa Firebase Console y verifica que Google Sign-In esté habilitado.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Crea o actualiza el documento del usuario en Firestore
     * Si es un usuario nuevo, solicita PIN y popula Firebase con pictogramas del sistema
     */
    private suspend fun createOrUpdateUserInFirestore(
        uid: String,
        nombre: String,
        email: String,
        photoUrl: String?
    ) {
        try {
            val userRef = firestore.collection("usuarios").document(uid)
            val userDoc = userRef.get().await()
            val esNuevoUsuario = !userDoc.exists()

            // Si es usuario nuevo, solicitar configuración de PIN
            var pin = ""
            if (esNuevoUsuario) {
                Log.d(TAG, "Usuario nuevo detectado. Solicitando configuración de PIN...")
                val pinConfigurardo = PinHelper.solicitarConfiguracionPin(this)
                if (pinConfigurardo == null) {
                    // Usuario canceló la configuración de PIN
                    Toast.makeText(this, "Debes configurar un PIN para continuar", Toast.LENGTH_LONG).show()
                    throw Exception("PIN no configurado")
                }
                pin = pinConfigurardo
                Log.d(TAG, "PIN configurado exitosamente")
            } else {
                // Usuario existente, mantener el PIN actual
                pin = userDoc.getString("pin") ?: ""
            }

            val userData: HashMap<String, Any> = hashMapOf(
                "nombre" to nombre,
                "tipo" to TipoUsuario.PADRE.name,
                "email" to email,
                "photoUrl" to (photoUrl ?: ""),
                "padreId" to "",  // El padre no tiene padre
                "pin" to pin,
                "activo" to true
            )

            // Solo agregar fechaCreacion si es nuevo usuario
            if (esNuevoUsuario) {
                userData["fechaCreacion"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            }

            // Usar set con merge para crear o actualizar
            userRef.set(userData, com.google.firebase.firestore.SetOptions.merge()).await()

            Log.d(TAG, "Usuario creado/actualizado en Firestore: $uid")

            // Si es un usuario nuevo, popular pictogramas iniciales
            if (esNuevoUsuario) {
                Log.d(TAG, "Nuevo usuario detectado. Populando pictogramas iniciales...")
                val firebaseRepo = (application as PictoCommApplication).firebaseRepository
                val migrationHelper = FirebaseMigrationHelper(firebaseRepo)

                val resultado = migrationHelper.popularPictogramasIniciales(uid)
                resultado.onSuccess { count ->
                    Log.i(TAG, "Pictogramas iniciales creados: $count")
                }.onFailure { error ->
                    Log.e(TAG, "Error al crear pictogramas iniciales", error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear/actualizar usuario en Firestore", e)
            throw e
        }
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGoogleSignIn.isEnabled = !show
        btnCancelar.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()
        // Verificar si ya hay una sesión activa
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: ${currentUser.uid}")
            // Opcional: redirigir automáticamente a MainActivity
        }
    }
}
