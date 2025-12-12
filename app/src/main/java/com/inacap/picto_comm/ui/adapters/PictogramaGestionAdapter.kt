package com.inacap.picto_comm.ui.adapters

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
import com.inacap.picto_comm.ui.utils.IconoHelper

/**
 * Adapter para gestionar todos los pictogramas (editar/eliminar)
 * Solo para uso del PADRE
 */
class PictogramaGestionAdapter(
    private val onEditar: (PictogramaSimple) -> Unit,
    private val onEliminar: (PictogramaSimple) -> Unit
) : ListAdapter<PictogramaSimple, PictogramaGestionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pictograma_gestion, parent, false)
        return ViewHolder(view, onEditar, onEliminar)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onEditar: (PictogramaSimple) -> Unit,
        private val onEliminar: (PictogramaSimple) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivIcono: ImageView = itemView.findViewById(R.id.iv_icono)
        private val tvTexto: TextView = itemView.findViewById(R.id.tv_texto)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tv_categoria)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        private val btnEditar: MaterialButton = itemView.findViewById(R.id.btn_editar)
        private val btnEliminar: MaterialButton = itemView.findViewById(R.id.btn_eliminar)

        fun bind(pictograma: PictogramaSimple) {
            // Texto
            tvTexto.text = pictograma.texto

            // Categoría
            tvCategoria.text = "Categoría: ${pictograma.categoria.nombreMostrar}"

            // Estado
            tvEstado.text = if (pictograma.aprobado) "Aprobado" else "Pendiente"
            tvEstado.setBackgroundColor(
                if (pictograma.aprobado)
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else
                    itemView.context.getColor(android.R.color.holo_orange_dark)
            )

            // Icono
            val iconoRes = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
            ivIcono.setImageResource(iconoRes)

            // Color de fondo según categoría
            val colorCategoria = pictograma.categoria.color
            itemView.findViewById<View>(R.id.card_icono).setBackgroundColor(colorCategoria.toInt())

            // Botones
            btnEditar.setOnClickListener {
                onEditar(pictograma)
            }

            btnEliminar.setOnClickListener {
                onEliminar(pictograma)
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
