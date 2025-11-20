package com.skbt.vspview

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnDeleteAll: Button
    private lateinit var adapter: GalleryAdapter

    private val photos = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerGallery)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)

        adapter = GalleryAdapter(photos) { uri ->
            val intent = Intent(this, FullScreenPhotoActivity::class.java)
            intent.putExtra("image_uri", uri.toString())
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter

        btnDeleteAll.setOnClickListener {
            confirmDeleteAll()
        }

        loadPhotos()
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()              // обновляем список
        setResult(RESULT_OK)      // возвращаем результат для MainActivity
    }

    private fun loadPhotos() {
        photos.clear()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Pictures/VSPView%")

        val projection = arrayOf(MediaStore.Images.Media._ID)

        val cursor = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val uri = ContentUris.withAppendedId(collection, id)
                photos.add(uri)
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun confirmDeleteAll() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удалить все фото?")
            .setMessage("Вы уверены? Все фотографии из VSPView будут удалены без возможности восстановления.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteAllPhotos()
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun deleteAllPhotos() {
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Pictures/VSPView%")

        contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs
        )

        loadPhotos()
        setResult(RESULT_OK)
    }
}
