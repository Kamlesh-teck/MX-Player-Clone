package com.example.mxclone

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val videoTitles = ArrayList<String>()
    private val videoUris = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) loadDeviceVideos()
        else Toast.makeText(this, "Storage permission zaroori hai", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etVideoUrl = findViewById<EditText>(R.id.etVideoUrl)
        val btnPlayUrl = findViewById<Button>(R.id.btnPlayUrl)
        val listView = findViewById<ListView>(R.id.listViewVideos)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, videoTitles)
        listView.adapter = adapter

        // Network URL Play
        btnPlayUrl.setOnClickListener {
            val url = etVideoUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                openPlayer(url, "Network Stream")
            } else {
                Toast.makeText(this, "Video link paste karein", Toast.LENGTH_SHORT).show()
            }
        }

        // Local Video Click
        listView.setOnItemClickListener { _, _, position, _ ->
            openPlayer(videoUris[position], videoTitles[position])
        }

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadDeviceVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadDeviceVideos() {
        videoTitles.clear()
        videoUris.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                videoTitles.add(name)
                videoUris.add(uri.toString())
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun openPlayer(uriString: String, title: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URI", uriString)
            putExtra("VIDEO_TITLE", title)
        }
        startActivity(intent)
    }
}