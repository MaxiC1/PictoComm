package com.inacap.picto_comm.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoUsuario
import com.inacap.picto_comm.data.model.Usuario
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository para manejar datos con Firebase
 *
 * Reemplaza RoomRepository usando:
 * - Firebase Auth para autenticación
 * - Firebase Firestore para datos
 * - Firebase Storage para imágenes
 */
class FirebaseRepository {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        // Configurar Firestore para funcionar online (sin cache offline)
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        firestoreSettings = settings
    }
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val COLLECTION_USUARIOS = "usuarios"
        private const val COLLECTION_PICTOGRAMAS = "pictogramas"
        private const val COLLECTION_ORACIONES = "oraciones"
    }

    // ============================
    // USUARIOS
    // ============================

    /**
     * Crea un usuario hijo (local, sin autenticación)
     */
    suspend fun crearUsuarioHijo(nombre: String, padreId: String): Result<String> {
        return try {
            val hijoData = hashMapOf(
                "nombre" to nombre,
                "tipo" to TipoUsuario.HIJO.name,
                "email" to "",
                "photoUrl" to "",
                "padreId" to padreId,
                "pin" to "",
                "activo" to true,
                "fechaCreacion" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection(COLLECTION_USUARIOS).add(hijoData).await()
            Log.d(TAG, "✅ Usuario hijo creado: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al crear usuario hijo", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el usuario actualmente autenticado
     */
    fun getCurrentUser(): Usuario? {
        val firebaseUser = auth.currentUser ?: return null
        return Usuario(
            id = firebaseUser.uid,
            nombre = firebaseUser.displayName ?: "Usuario",
            tipo = TipoUsuario.PADRE,
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
            activo = true
        )
    }

    /**
     * Crea o actualiza un usuario en Firestore
     */
    suspend fun crearUsuario(usuario: Usuario): String {
        try {
            val userRef = if (usuario.id.isNotEmpty()) {
                firestore.collection(COLLECTION_USUARIOS).document(usuario.id)
            } else {
                firestore.collection(COLLECTION_USUARIOS).document()
            }

            val userData = usuario.toMap()
            userRef.set(userData).await()

            Log.d(TAG, "Usuario creado/actualizado: ${userRef.id}")
            return userRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear usuario", e)
            throw e
        }
    }

    /**
     * Obtiene un usuario por ID
     */
    suspend fun obtenerUsuario(userId: String): Usuario? {
        return try {
            val doc = firestore.collection(COLLECTION_USUARIOS)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject<Usuario>()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario", e)
            null
        }
    }

    /**
     * Obtiene todos los usuarios
     */
    suspend fun obtenerTodosUsuarios(): List<Usuario> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USUARIOS)
                .whereEqualTo("activo", true)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject<Usuario>()?.copy(id = doc.id)
            }.also { usuarios ->
                Log.d(TAG, "✅ Usuarios obtenidos de Firestore: ${usuarios.size}")
                usuarios.forEach { Log.d(TAG, "  - Usuario: ${it.nombre} (${it.tipo}) - ID: ${it.id}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener usuarios", e)
            emptyList()
        }
    }

    /**
     * Obtiene el usuario padre (autenticado con Google)
     */
    suspend fun obtenerPadre(): Usuario? {
        return try {
            val snapshot = firestore.collection(COLLECTION_USUARIOS)
                .whereEqualTo("tipo", TipoUsuario.PADRE.name)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject<Usuario>()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener padre", e)
            null
        }
    }

    /**
     * Obtiene los usuarios hijos del padre actual
     */
    suspend fun obtenerHijos(): List<Usuario> {
        return try {
            val padreId = auth.currentUser?.uid ?: return emptyList()

            val snapshot = firestore.collection(COLLECTION_USUARIOS)
                .whereEqualTo("tipo", TipoUsuario.HIJO.name)
                .whereEqualTo("padreId", padreId)
                .whereEqualTo("activo", true)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject<Usuario>() }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener hijos", e)
            emptyList()
        }
    }

    /**
     * Verifica si existe un padre
     */
    suspend fun existePadre(): Boolean {
        return try {
            val snapshot = firestore.collection(COLLECTION_USUARIOS)
                .whereEqualTo("tipo", TipoUsuario.PADRE.name)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar padre", e)
            false
        }
    }

    /**
     * Verifica si existen usuarios
     */
    suspend fun existenUsuarios(): Boolean {
        return try {
            val snapshot = firestore.collection(COLLECTION_USUARIOS)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar usuarios", e)
            false
        }
    }

    /**
     * Desactiva un usuario (soft delete)
     */
    suspend fun desactivarUsuario(userId: String) {
        try {
            firestore.collection(COLLECTION_USUARIOS)
                .document(userId)
                .update("activo", false)
                .await()

            Log.d(TAG, "Usuario desactivado: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al desactivar usuario", e)
            throw e
        }
    }

    // ============================
    // PICTOGRAMAS
    // ============================

    /**
     * Obtiene todos los pictogramas
     */
    fun obtenerTodosPictogramas(): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al obtener pictogramas", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al mapear pictograma", e)
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas.sortedByDescending { it.fechaCreacion }
                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene pictogramas aprobados (para usuarios hijos)
     */
    fun obtenerPictogramasAprobados(): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .whereEqualTo("aprobado", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar pictogramas aprobados", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas.sortedByDescending { it.frecuenciaUso }

                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene pictogramas por categoría
     */
    fun obtenerPictogramasPorCategoria(categoria: String): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .whereEqualTo("aprobado", true)
            .whereEqualTo("categoria", categoria)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar pictogramas por categoría", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas.sortedBy { it.texto }

                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene pictogramas favoritos
     */
    fun obtenerPictogramasFavoritos(): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .whereEqualTo("aprobado", true)
            .whereEqualTo("esFavorito", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar favoritos", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas.sortedBy { it.texto }

                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene pictogramas más usados
     */
    fun obtenerPictogramasMasUsados(limite: Int = 10): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .whereEqualTo("aprobado", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar más usados", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas
                    .sortedByDescending { it.frecuenciaUso }
                    .take(limite)

                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene pictogramas pendientes de aprobación
     */
    fun obtenerPictogramasPendientes(): Flow<List<PictogramaSimple>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_PICTOGRAMAS)
            .whereEqualTo("padreId", padreId)
            .whereEqualTo("aprobado", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al obtener pictogramas pendientes", error)
                    return@addSnapshotListener
                }

                val pictogramas = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val datos = doc.data
                        if (datos != null) {
                            PictogramaSimple.fromMap(datos).copy(id = doc.id)
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al mapear pictograma pendiente", e)
                        null
                    }
                } ?: emptyList()

                val pictogramasOrdenados = pictogramas.sortedByDescending { it.fechaCreacion }
                trySend(pictogramasOrdenados)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Crea un nuevo pictograma
     */
    suspend fun crearPictograma(pictograma: PictogramaSimple): String {
        try {
            val padreId = auth.currentUser?.uid ?: ""
            val pictogramaConPadre = pictograma.copy(
                padreId = padreId,
                fechaCreacion = System.currentTimeMillis()
            )

            val docRef = firestore.collection(COLLECTION_PICTOGRAMAS)
                .add(pictogramaConPadre.toMap())
                .await()

            Log.d(TAG, "Pictograma creado: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear pictograma", e)
            throw e
        }
    }

    /**
     * Actualiza un pictograma
     */
    suspend fun actualizarPictograma(pictograma: PictogramaSimple) {
        try {
            if (pictograma.id.isEmpty()) {
                throw IllegalArgumentException("ID de pictograma vacío")
            }

            firestore.collection(COLLECTION_PICTOGRAMAS)
                .document(pictograma.id)
                .set(pictograma.toMap())
                .await()

            Log.d(TAG, "Pictograma actualizado: ${pictograma.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar pictograma", e)
            throw e
        }
    }

    /**
     * Elimina un pictograma
     */
    suspend fun eliminarPictograma(pictogramaId: String) {
        try {
            firestore.collection(COLLECTION_PICTOGRAMAS)
                .document(pictogramaId)
                .delete()
                .await()

            Log.d(TAG, "Pictograma eliminado: $pictogramaId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar pictograma", e)
            throw e
        }
    }

    /**
     * Alterna el estado de favorito de un pictograma
     */
    suspend fun alternarFavorito(pictogramaId: String, esFavorito: Boolean) {
        try {
            firestore.collection(COLLECTION_PICTOGRAMAS)
                .document(pictogramaId)
                .update("esFavorito", esFavorito)
                .await()

            Log.d(TAG, "Favorito actualizado: $pictogramaId = $esFavorito")
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar favorito", e)
            throw e
        }
    }

    /**
     * Incrementa la frecuencia de uso de un pictograma
     */
    suspend fun incrementarUso(pictogramaId: String) {
        try {
            val docRef = firestore.collection(COLLECTION_PICTOGRAMAS).document(pictogramaId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val frecuenciaActual = snapshot.getLong("frecuenciaUso") ?: 0
                transaction.update(docRef, "frecuenciaUso", frecuenciaActual + 1)
            }.await()

            Log.d(TAG, "Uso incrementado: $pictogramaId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al incrementar uso", e)
        }
    }

    /**
     * Aprueba un pictograma
     */
    suspend fun aprobarPictograma(pictogramaId: String) {
        try {
            firestore.collection(COLLECTION_PICTOGRAMAS)
                .document(pictogramaId)
                .update("aprobado", true)
                .await()

            Log.d(TAG, "Pictograma aprobado: $pictogramaId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al aprobar pictograma", e)
            throw e
        }
    }

    /**
     * Rechaza (elimina) un pictograma
     */
    suspend fun rechazarPictograma(pictogramaId: String) {
        eliminarPictograma(pictogramaId)
    }

    /**
     * Cuenta pictogramas pendientes
     */
    suspend fun contarPictogramasPendientes(): Int {
        return try {
            val padreId = auth.currentUser?.uid ?: ""
            val snapshot = firestore.collection(COLLECTION_PICTOGRAMAS)
                .whereEqualTo("padreId", padreId)
                .whereEqualTo("aprobado", false)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error al contar pendientes", e)
            0
        }
    }

    // ============================
    // ORACIONES (Historial)
    // ============================

    /**
     * Guarda una oración en el historial
     */
    suspend fun guardarOracion(pictogramaIds: List<String>, textoCompleto: String): String {
        try {
            val padreId = auth.currentUser?.uid ?: ""
            val oracionData = hashMapOf(
                "pictogramaIds" to pictogramaIds,
                "textoCompleto" to textoCompleto,
                "fechaCreacion" to com.google.firebase.Timestamp.now(),
                "vecesUsada" to 1,
                "padreId" to padreId
            )

            val docRef = firestore.collection(COLLECTION_ORACIONES)
                .add(oracionData)
                .await()

            Log.d(TAG, "Oración guardada: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar oración", e)
            throw e
        }
    }

    /**
     * Obtiene todas las oraciones del historial
     */
    fun obtenerTodasOraciones(): Flow<List<Map<String, Any>>> = callbackFlow {
        val padreId = auth.currentUser?.uid ?: ""

        val listener = firestore.collection(COLLECTION_ORACIONES)
            .whereEqualTo("padreId", padreId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar oraciones", error)
                    return@addSnapshotListener
                }

                val oraciones = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)
                } ?: emptyList()

                // Ordenar por fecha de creación en el cliente
                val oracionesOrdenadas = oraciones.sortedByDescending {
                    (it["fechaCreacion"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                }

                trySend(oracionesOrdenadas)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Limpia todos los datos (solo para desarrollo/testing)
     */
    suspend fun limpiarTodo() {
        try {
            // ADVERTENCIA: Esto eliminará todos los datos
            Log.w(TAG, "limpiarTodo() - Esta operación no está implementada para Firebase por seguridad")
            // No implementado intencionalmente para evitar borrado accidental de datos en producción
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar datos", e)
        }
    }
}
