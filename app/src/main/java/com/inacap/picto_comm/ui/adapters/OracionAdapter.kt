package com.inacap.picto_comm.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.ui.utils.IconoHelper
import androidx.core.graphics.toColorInt

/**
 * Adapter para mostrar los pictogramas en la barra de oración (vista compacta)
 *
 * Características:
 * - Vista horizontal compacta
 * - Botón para eliminar cada pictograma
 * - Colores por categoría
 */
class OracionAdapter(
    private val onEliminarPictograma: (Int) -> Unit
) : ListAdapter<PictogramaSimple, OracionAdapter.OracionViewHolder>(PictogramaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OracionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pictograma_compacto, parent, false)
        return OracionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OracionViewHolder, position: Int) {
        val pictograma = getItem(position)
        holder.bind(pictograma, position)
    }

    inner class OracionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPictograma: CardView = itemView.findViewById(R.id.card_pictograma_compacto)
        private val ivIcono: ImageView = itemView.findViewById(R.id.iv_icono_compacto)
        private val tvTexto: TextView = itemView.findViewById(R.id.tv_texto_compacto)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btn_eliminar_compacto)

        fun bind(pictograma: PictogramaSimple, posicion: Int) {
            // Texto
            tvTexto.text = pictograma.texto

            // Icono
            val iconoResId = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
            ivIcono.setImageResource(iconoResId)

            // Colores de categoría
            val color = String.format("#%08X", pictograma.categoria.color).toColorInt()
            val colorLight =
                String.format("#%08X", (pictograma.categoria.color and 0xFFFFFF) or 0x1A000000)
                    .toColorInt()

            // Aplicar colores
            cardPictograma.setCardBackgroundColor(colorLight)
            ivIcono.setColorFilter(color)

            // Botón eliminar
            btnEliminar.setOnClickListener {
                onEliminarPictograma(posicion)
            }
        }
    }

    /**
     * DiffUtil para comparar pictogramas
     */
    class PictogramaDiffCallback : DiffUtil.ItemCallback<PictogramaSimple>() {
        override fun areItemsTheSame(oldItem: PictogramaSimple, newItem: PictogramaSimple): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PictogramaSimple, newItem: PictogramaSimple): Boolean {
            return oldItem == newItem
        }
    }
}
