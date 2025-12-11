package com.inacap.picto_comm.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.inacap.picto_comm.R
import com.inacap.picto_comm.data.model.Categoria
import com.inacap.picto_comm.ui.utils.IconoHelper

/**
 * Data class para representar un item en el selector de categorías
 * Puede ser una categoría real o un item especial como "Todos" o "Favoritos"
 */
data class ItemCategoria(
    val tipo: TipoItemCategoria,
    val categoria: Categoria? = null,
    val nombre: String,
    val color: Long,
    val iconoResId: Int
)

enum class TipoItemCategoria {
    TODOS,
    FAVORITOS,
    CATEGORIA
}

/**
 * Adapter para el selector horizontal de categorías
 *
 * Características:
 * - Muestra "Todos", "Favoritos" y las 6 categorías
 * - Resalta la categoría seleccionada
 * - Aplica colores dinámicamente
 */
class CategoriaAdapter(
    private val onCategoriaSeleccionada: (ItemCategoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    private val items = mutableListOf<ItemCategoria>()
    private var itemSeleccionado: ItemCategoria? = null

    init {
        // Inicializar con todos los items
        items.add(
            ItemCategoria(
                tipo = TipoItemCategoria.TODOS,
                nombre = "Todos",
                color = 0xFF9E9E9E,
                iconoResId = R.drawable.ic_category
            )
        )
        items.add(
            ItemCategoria(
                tipo = TipoItemCategoria.FAVORITOS,
                nombre = "Favoritos",
                color = 0xFFF44336,
                iconoResId = R.drawable.ic_favorite
            )
        )

        // Agregar todas las categorías
        Categoria.values().forEach { categoria ->
            items.add(
                ItemCategoria(
                    tipo = TipoItemCategoria.CATEGORIA,
                    categoria = categoria,
                    nombre = categoria.nombreMostrar,
                    color = categoria.color,
                    iconoResId = IconoHelper.obtenerIconoParaCategoria(categoria)
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val item = items[position]
        val estaSeleccionado = item == itemSeleccionado
        holder.bind(item, estaSeleccionado)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Marca un item como seleccionado
     */
    fun seleccionarItem(item: ItemCategoria) {
        val posicionAnterior = items.indexOf(itemSeleccionado)
        val posicionNueva = items.indexOf(item)

        itemSeleccionado = item

        // Notificar cambios solo en las posiciones afectadas
        if (posicionAnterior >= 0) notifyItemChanged(posicionAnterior)
        if (posicionNueva >= 0) notifyItemChanged(posicionNueva)
    }

    /**
     * Seleccionar por categoría (null = "Todos")
     */
    fun seleccionarCategoria(categoria: Categoria?) {
        val item = if (categoria == null) {
            items.first { it.tipo == TipoItemCategoria.TODOS }
        } else {
            items.first { it.categoria == categoria }
        }
        seleccionarItem(item)
    }

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardCategoria: CardView = itemView.findViewById(R.id.card_categoria)
        private val ivIcono: ImageView = itemView.findViewById(R.id.iv_icono_categoria)
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_categoria)

        fun bind(item: ItemCategoria, estaSeleccionado: Boolean) {
            // Nombre
            tvNombre.text = item.nombre

            // Icono
            ivIcono.setImageResource(item.iconoResId)

            // Color
            val color = Color.parseColor(String.format("#%08X", item.color))
            val colorLight = if (estaSeleccionado) {
                color // Color sólido si está seleccionado
            } else {
                Color.parseColor(
                    String.format("#%08X", (item.color and 0xFFFFFF) or 0x33000000)
                ) // Color transparente si no está seleccionado
            }

            // Aplicar colores
            cardCategoria.setCardBackgroundColor(colorLight)
            
            // Color del icono y texto: blanco si está seleccionado, color de categoría si no
            if (estaSeleccionado) {
                ivIcono.setColorFilter(Color.WHITE)
                tvNombre.setTextColor(Color.WHITE)
            } else {
                ivIcono.setColorFilter(color)
                tvNombre.setTextColor(color)
            }

            // Elevación si está seleccionado
            cardCategoria.cardElevation = if (estaSeleccionado) {
                itemView.context.resources.displayMetrics.density * 4 // 4dp
            } else {
                0f
            }

            // Click listener
            itemView.setOnClickListener {
                onCategoriaSeleccionada(item)
            }
        }
    }
}
