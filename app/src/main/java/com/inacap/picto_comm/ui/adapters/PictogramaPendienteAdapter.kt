package com.inacap.picto_comm.ui.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen
import com.inacap.picto_comm.ui.utils.IconoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para mostrar pictogramas pendientes de aprobación
 * Solo para uso del PADRE
 */
class PictogramaPendienteAdapter(
    private val onAprobar: (PictogramaSimple) -> Unit,
    private val onRechazar: (PictogramaSimple) -> Unit,
    private val obtenerNombreUsuario: (String) -> String
) : ListAdapter<PictogramaSimple, PictogramaPendienteAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pictograma_pendiente, parent, false)
        return ViewHolder(view, onAprobar, onRechazar, obtenerNombreUsuario)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onAprobar: (PictogramaSimple) -> Unit,
        private val onRechazar: (PictogramaSimple) -> Unit,
        private val obtenerNombreUsuario: (String) -> String
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivIcono: ImageView = itemView.findViewById(R.id.iv_icono)
        private val tvTexto: TextView = itemView.findViewById(R.id.tv_texto)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        private val tvCreador: TextView = itemView.findViewById(R.id.tv_creador)
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)
        private val btnAprobar: MaterialButton = itemView.findViewById(R.id.btn_aprobar)
        private val btnRechazar: MaterialButton = itemView.findViewById(R.id.btn_rechazar)

        fun bind(pictograma: PictogramaSimple) {
            // Texto
            tvTexto.text = pictograma.texto

            // Categoría
            tvCategoria.text = itemView.context.getString(
                R.string.categoria_pictograma
            ) + " ${pictograma.categoria.nombreMostrar}"

            // Creador
            val nombreCreador = obtenerNombreUsuario(pictograma.creadoPor)
            tvCreador.text = itemView.context.getString(
                R.string.creado_por
            ) + " $nombreCreador"

            // Fecha
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaTexto = formato.format(Date(pictograma.fechaCreacion))
            tvFecha.text = itemView.context.getString(R.string.fecha_creacion) + " $fechaTexto"

            // Cargar imagen según el tipo
            if (pictograma.tipoImagen == TipoImagen.FOTO && pictograma.urlImagen.isNotEmpty()) {
                // Imagen personalizada desde Base64
                try {
                    val imageBytes = Base64.decode(pictograma.urlImagen, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivIcono.setImageBitmap(bitmap)
                    ivIcono.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (e: Exception) {
                    android.util.Log.e("PictogramaPendienteAdapter", "Error al decodificar imagen Base64", e)
                    // Fallback: usar icono por defecto
                    val iconoRes = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
                    ivIcono.setImageResource(iconoRes)
                    ivIcono.scaleType = ImageView.ScaleType.CENTER
                }
            } else {
                // Icono del sistema
                val iconoRes = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
                ivIcono.setImageResource(iconoRes)
                ivIcono.scaleType = ImageView.ScaleType.CENTER
            }

            // Color de fondo según categoría
            val colorCategoria = pictograma.categoria.color
            itemView.findViewById<View>(R.id.card_icono).setBackgroundColor(colorCategoria.toInt())

            // Botones
            btnAprobar.setOnClickListener {
                onAprobar(pictograma)
            }

            btnRechazar.setOnClickListener {
                onRechazar(pictograma)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PictogramaSimple>() {
        override fun areItemsTheSame(oldItem: PictogramaSimple, newItem: PictogramaSimple): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PictogramaSimple, newItem: PictogramaSimple): Boolean {
            return oldItem == newItem
        }
    }
}
