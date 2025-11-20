package com.skbt.vspview

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class FullScreenPhotoActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnDelete: Button

    private val images = ArrayList<Uri>()
    private var startPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_swipe)

        viewPager = findViewById(R.id.photoPager)
        btnDelete = findViewById(R.id.btnDelete)

        loadImages()

        startPosition = intent.getIntExtra("position", 0)

        viewPager.adapter = FullscreenPagerAdapter(this, images)
        viewPager.setCurrentItem(startPosition, false)

        btnDelete.setOnClickListener {
            deleteCurrentPhoto()
        }
    }

    private fun loadImages() {
        images.clear()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Pictures/VSPView%")

        val cursor = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val uri = Uri.withAppendedPath(collection, id.toString())
                images.add(uri)
            }
        }
    }

    private fun deleteCurrentPhoto() {
        if (images.isEmpty()) return

        val index = viewPager.currentItem
        val uri = images[index]

        val rows = contentResolver.delete(uri, null, null)

        if (rows > 0) {
            images.removeAt(index)

            // Сообщаем MainActivity что фото обновились
            setResult(RESULT_OK)

            if (images.isEmpty()) {
                finish()
                return
            }

            viewPager.adapter = FullscreenPagerAdapter(this, images)
            viewPager.setCurrentItem(index.coerceAtMost(images.lastIndex), false)

            Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
        }
    }
}
