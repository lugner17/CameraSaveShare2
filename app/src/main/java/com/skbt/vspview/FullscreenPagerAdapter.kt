package com.skbt.vspview

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class FullscreenPagerAdapter(
    private val context: Context,
    private val images: List<Uri>
) : RecyclerView.Adapter<FullscreenPagerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.fullImageSlide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_fullscreen_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.img.setImageURI(images[position])
    }

    override fun getItemCount(): Int = images.size
}
