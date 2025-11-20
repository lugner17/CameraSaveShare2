package com.skbt.vspview

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(
    private val items: MutableList<Uri>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
        val tvFilename: TextView = view.findViewById(R.id.tvFilename)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = items[position]

        // Установка миниатюры (быстрое, простое решение)
        holder.imgThumb.setImageURI(uri)

        // Установка имени файла (через ContentResolver)
        holder.tvFilename.text = getDisplayName(holder.itemView.context.contentResolver, uri)

        holder.itemView.setOnClickListener { onClick(uri) }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Обновляет список элементов и уведомляет RecyclerView.
     * Вызывать после удаления/добавления фото (например в onResume).
     */
    fun updateItems(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun getDisplayName(resolver: ContentResolver, uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        var name = "unknown.jpg"

        val cursor: Cursor? = try {
            resolver.query(uri, projection, null, null, null)
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }

        return name
    }
}
