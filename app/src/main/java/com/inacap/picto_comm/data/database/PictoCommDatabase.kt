package com.inacap.picto_comm.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inacap.picto_comm.data.FuenteDatosMock
import com.inacap.picto_comm.data.database.converters.Converters
import com.inacap.picto_comm.data.database.dao.OracionDao
import com.inacap.picto_comm.data.database.dao.PictogramaDao
import com.inacap.picto_comm.data.database.dao.UsuarioDao
import com.inacap.picto_comm.data.database.entities.OracionEntity
import com.inacap.picto_comm.data.database.entities.PictogramaEntity
import com.inacap.picto_comm.data.database.entities.UsuarioEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database principal de PictoComm
 *
 * Define la base de datos SQLite local con:
 * - Tabla de pictogramas (con control parental)
 * - Tabla de oraciones guardadas
 * - Tabla de usuarios (PADRE/HIJO)
 *
 * Versión 2: Agregada tabla de usuarios y campos de control parental
 */
@Database(
    entities = [
        PictogramaEntity::class,
        OracionEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PictoCommDatabase : RoomDatabase() {

    abstract fun pictogramaDao(): PictogramaDao
    abstract fun oracionDao(): OracionDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: PictoCommDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (Singleton)
         */
        fun getDatabase(context: Context, scope: CoroutineScope): PictoCommDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PictoCommDatabase::class.java,
                    "picto_comm_database"
                )
                    .fallbackToDestructiveMigration() // Permite recrear BD en cambios de versión
                    .addCallback(PictogramasDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback para poblar la base de datos en primera ejecución
         */
        private class PictogramasDatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        poblarDatabase(
                            database.pictogramaDao(),
                            database.usuarioDao()
                        )
                    }
                }
            }

            /**
             * Llena la base de datos con datos iniciales
             */
            suspend fun poblarDatabase(
                pictogramaDao: PictogramaDao,
                usuarioDao: UsuarioDao
            ) {
                // 1. Insertar pictogramas del sistema (51 pictogramas iniciales)
                val pictogramasMock = FuenteDatosMock.obtenerTodosPictogramas()
                val pictogramasEntity = pictogramasMock.map { pictograma ->
                    PictogramaEntity.fromModel(pictograma, frecuenciaUso = 0)
                }
                pictogramaDao.insertarTodos(pictogramasEntity)

                // 2. Crear usuario padre por defecto (opcional)
                // Puedes descomentar esto si quieres un usuario padre predefinido
                /*
                val usuarioPadre = UsuarioEntity(
                    nombre = "Administrador",
                    tipo = "PADRE",
                    pin = "1234", // PIN por defecto
                    email = "",
                    fechaCreacion = System.currentTimeMillis(),
                    activo = true
                )
                usuarioDao.insertar(usuarioPadre)
                */
            }
        }

        /**
         * Limpia la instancia de la base de datos (útil para testing)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
