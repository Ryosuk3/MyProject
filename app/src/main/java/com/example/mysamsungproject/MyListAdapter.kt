package com.example.mysamsungproject
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.*
import android.view.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.*



class MyListAdapter(val list: List<Widgets>): RecyclerView.Adapter<MyListAdapter.MyView>() {
    private var itemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }
    class MyView(view: View): RecyclerView.ViewHolder(view){
        val bName: TextView = view.findViewById(R.id.widget_name)
        val bImage: ImageView = view.findViewById(R.id.widget_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyView {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.widget_item, parent, false)
        return MyView(view)
    }

    override fun onBindViewHolder(holder: MyView, position: Int) {
        holder.bName.text = list[position].name

        // Преобразуем строковое название ресурса в идентификатор ресурса
        val context = holder.itemView.context
        val imageId = context.resources.getIdentifier(list[position].image, "drawable", context.packageName)

        if (imageId != 0) {
            holder.bImage.setImageResource(imageId)
        } else {
            // Обработка случая, когда идентификатор не найден
             // Замените на ваш placeholder
        }


        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
    }

    override fun getItemCount() = list.size
}