package com.inacap.picto_comm.ui.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.PictogramaSimple
import com.inacap.picto_comm.data.model.TipoImagen
import com.inacap.picto_comm.ui.utils.IconoHelper

/**
 * Adapter para mostrar pictogramas en un grid (RecyclerView)
 *
 * Características:
 * - Usa DiffUtil para actualizaciones eficientes
 * - Soporta click y long-click
 * - Muestra icono de favorito si aplica
 * - Aplica colores por categoría
 */
class PictogramaAdapter(
    private val onClickPictograma: (PictogramaSimple) -> Unit,
    private val onLongClickPictograma: ((PictogramaSimple) -> Unit)? = null
) : ListAdapter<PictogramaSimple, PictogramaAdapter.PictogramaViewHolder>(PictogramaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictogramaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pictograma, parent, false)
        return PictogramaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PictogramaViewHolder, position: Int) {
        val pictograma = getItem(position)
        holder.bind(pictograma)
    }

    inner class PictogramaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPictograma: CardView = itemView.findViewById(R.id.card_pictograma)
        private val frameIcono: FrameLayout = itemView.findViewById(R.id.frame_icono)
        private val ivIcono: ImageView = itemView.findViewById(R.id.iv_icono)
        private val tvTexto: TextView = itemView.findViewById(R.id.tv_texto)
        private val ivFavorito: ImageView = itemView.findViewById(R.id.iv_favorito)

        fun bind(pictograma: PictogramaSimple) {
            // Texto del pictograma
            tvTexto.text = pictograma.texto

            // Color de categoría
            val colorCategoria = Color.parseColor(String.format("#%08X", pictograma.categoria.color))
            val colorCategoriaLight = Color.parseColor(
                String.format("#%08X", (pictograma.categoria.color and 0xFFFFFF) or 0x1A000000)
            )

            // Aplicar colores
            cardPictograma.setCardBackgroundColor(colorCategoriaLight)
            frameIcono.setBackgroundColor(colorCategoria)

            // Cargar imagen según el tipo
            if (pictograma.tipoImagen == TipoImagen.FOTO && pictograma.urlImagen.isNotEmpty()) {
                // Imagen personalizada desde Base64
                try {
                    val imageBytes = Base64.decode(pictograma.urlImagen, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivIcono.setImageBitmap(bitmap)
                    ivIcono.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivIcono.clearColorFilter() // NO aplicar filtro a fotos
                } catch (e: Exception) {
                    android.util.Log.e("PictogramaAdapter", "Error al decodificar imagen Base64", e)
                    // Fallback: usar icono por defecto
                    val iconoResId = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
                    ivIcono.setImageResource(iconoResId)
                    ivIcono.scaleType = ImageView.ScaleType.CENTER
                }
            } else {
                // Icono del sistema
                val iconoResId = IconoHelper.obtenerIconoParaPictograma(pictograma.recursoImagen)
                ivIcono.setImageResource(iconoResId)
                ivIcono.scaleType = ImageView.ScaleType.CENTER
            }

            // Mostrar/ocultar indicador de favorito
            ivFavorito.visibility = if (pictograma.esFavorito) View.VISIBLE else View.GONE

            // Click listener
            itemView.setOnClickListener {
                onClickPictograma(pictograma)
            }

            // Long click listener (para favoritos)
            onLongClickPictograma?.let { callback ->
                itemView.setOnLongClickListener {
                    callback(pictograma)
                    true
                }
            }
        }
    }

    /**
     * DiffUtil para comparar pictogramas y actualizar eficientemente
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
