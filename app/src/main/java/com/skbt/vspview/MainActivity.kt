package com.skbt.vspview

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var btnVSP: Button
    private lateinit var btnATM: Button
    private lateinit var btnIVI: Button
    private lateinit var btnSVN: Button
    private lateinit var btnTV: Button
    private lateinit var btnITS: Button
    private lateinit var btnTD: Button

    private lateinit var btnOpenGallery: Button
    private lateinit var btnSendZip: Button
    private lateinit var imgPreview: ImageView

    private var currentPhotoUri: Uri? = null

    // Запрос камеры
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && pendingPrefix != null) {
                openCameraWithPrefix(pendingPrefix!!)
            }
        }

    private var pendingPrefix: String? = null

    // Камера
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoUri != null) {

                // Добавляем штамп
                TimeStampUtil.addTimestamp(this, currentPhotoUri!!)

                updatePhotoCount()
                Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgPreview = findViewById(R.id.imgPreview)

        btnVSP = findViewById(R.id.btnVSP)
        btnATM = findViewById(R.id.btnATM)
        btnIVI = findViewById(R.id.btnIVI)
        btnSVN = findViewById(R.id.btnSVN)
        btnTV = findViewById(R.id.btnTV)
        btnITS = findViewById(R.id.btnITS)
        btnTD = findViewById(R.id.btnTD)

        btnOpenGallery = findViewById(R.id.btnOpenGallery)
        btnSendZip = findViewById(R.id.btnSendZip)

        // навешиваем обработчики
        btnVSP.setOnClickListener { takePhotoWithPrefix("VSP") }
        btnATM.setOnClickListener { takePhotoWithPrefix("ATM") }
        btnIVI.setOnClickListener { takePhotoWithPrefix("IVI") }
        btnSVN.setOnClickListener { takePhotoWithPrefix("SVN") }
        btnTV.setOnClickListener { takePhotoWithPrefix("TV") }
        btnITS.setOnClickListener { takePhotoWithPrefix("ITS") }
        btnTD.setOnClickListener { takePhotoWithPrefix("TD") }

        btnOpenGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        btnSendZip.setOnClickListener {
            sendAllPhotosZip()
        }

        updatePhotoCount()
    }

    // ----------- ЛОГИКА СЪЁМКИ --------------

    private fun takePhotoWithPrefix(prefix: String) {
        pendingPrefix = prefix

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraWithPrefix(prefix)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraWithPrefix(prefix: String) {
        val resolver = contentResolver
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VSPView")
        }

        currentPhotoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        takePictureLauncher.launch(currentPhotoUri)
    }

    // ----------- ZIP ОТПРАВКА ----------------

    private fun sendAllPhotosZip() {
        val photos = getAllPhotos()

        if (photos.isEmpty()) {
            Toast.makeText(this, "Нет фото для отправки", Toast.LENGTH_SHORT).show()
            return
        }

        val zipFile = File(cacheDir, "photos.zip")
        val fos = FileOutputStream(zipFile)
        val zos = ZipOutputStream(fos)

        photos.forEach { uri ->
            val input = contentResolver.openInputStream(uri) ?: return@forEach

            val name = getFileName(uri)
            zos.putNextEntry(ZipEntry(name))

            input.copyTo(zos)
            input.close()
            zos.closeEntry()
        }

        zos.close()
        fos.close()

        val zipUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            zipFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, zipUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(sendIntent, "Отправить ZIP"))
    }

    private fun getAllPhotos(): List<Uri> {
        val list = mutableListOf<Uri>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Pictures/VSPView%"),
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val uri = Uri.withAppendedPath(collection, id.toString())
                list.add(uri)
            }
        }

        return list
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            null,
            null,
            null
        )

        var name = "unknown.jpg"

        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(0)
            }
        }
        return name
    }

    // ----------- СЧЁТЧИК ФОТО ----------------

    private fun updatePhotoCount() {
        val count = getAllPhotos().size
        btnOpenGallery.text = "Открыть галерею ($count)"
    }
}
