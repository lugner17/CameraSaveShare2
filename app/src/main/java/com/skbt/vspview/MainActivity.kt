package com.skbt.vspview

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var photoView: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnMakeVspPhoto: Button
    private lateinit var btnMakeAtmPhoto: Button
    private lateinit var btnSendZip: Button
    private lateinit var btnOpenGallery: Button

    private var currentPhotoUri: Uri? = null

    private var useVspName = false
    private var useAtmName = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { applyTimestampToImage(it) }
                updatePhotoCount()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoView = findViewById(R.id.photoView)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnMakeVspPhoto = findViewById(R.id.btnMakeVspPhoto)
        btnMakeAtmPhoto = findViewById(R.id.btnMakeAtmPhoto)
        btnSendZip = findViewById(R.id.btnSendZip)
        btnOpenGallery = findViewById(R.id.btnOpenGallery)

        // Убираем превью
        photoView.setImageDrawable(null)

        btnTakePhoto.setOnClickListener {
            useVspName = false
            useAtmName = false
            checkCameraPermission()
        }

        btnMakeVspPhoto.setOnClickListener {
            useVspName = true
            useAtmName = false
            checkCameraPermission()
        }

        btnMakeAtmPhoto.setOnClickListener {
            useVspName = false
            useAtmName = true
            checkCameraPermission()
        }

        btnSendZip.setOnClickListener {
            sendAllPhotosZip()
        }

        btnOpenGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        updatePhotoCount()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val resolver = contentResolver

        val filename = when {
            useVspName -> "VSP_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
            useAtmName -> "ATM_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
            else -> "photo_${System.currentTimeMillis()}.jpg"
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VSPView")
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        currentPhotoUri = uri

        // Сбрасываем флаги
        useVspName = false
        useAtmName = false

        takePictureLauncher.launch(uri)
    }

    private fun applyTimestampToImage(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val outputBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val text = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            style = Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        canvas.drawText(text, 30f, outputBitmap.height - 40f, paint)

        val outputStream: OutputStream? = contentResolver.openOutputStream(uri, "w")
        outputStream?.use {
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    private fun updatePhotoCount() {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Pictures/VSPView%"),
            null
        )

        val count = cursor?.count ?: 0
        cursor?.close()

        btnOpenGallery.text = "Открыть галерею ($count)"
    }

    private fun sendAllPhotosZip() {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Pictures/VSPView%"),
            null
        )

        if (cursor == null || cursor.count == 0) return

        val zipFile = File(cacheDir, "photos.zip")
        val zipOut = ZipOutputStream(FileOutputStream(zipFile))

        cursor.use {
            val idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)

                val uri = Uri.withAppendedPath(collection, id.toString())
                val input = contentResolver.openInputStream(uri) ?: continue

                zipOut.putNextEntry(ZipEntry(name))
                input.copyTo(zipOut)
                zipOut.closeEntry()
                input.close()
            }
        }

        zipOut.close()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile))
        }

        startActivity(Intent.createChooser(shareIntent, "Отправить ZIP"))
    }
}
