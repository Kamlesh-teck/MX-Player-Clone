package com.example.mxclone

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

data class VideoItem(val title: String, val uri: String, val folderName: String)

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var tvCurrentSection: TextView
    private lateinit var btnBackFolder: Button
    private lateinit var fabLastVideo: FloatingActionButton
    private lateinit var adapter: ArrayAdapter<String>

    private val displayList = ArrayList<String>()
    private val folderMap = LinkedHashMap<String, ArrayList<VideoItem>>()
    private val folderNames = ArrayList<String>()
    private var currentFolder: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) loadDeviceVideos()
        else Toast.makeText(this, "Storage permission zaroori hai", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = AppPrefs(this)
        prefs.applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etVideoUrl = findViewById<EditText>(R.id.etVideoUrl)
        val btnPlayUrl = findViewById<Button>(R.id.btnPlayUrl)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val listView = findViewById<ListView>(R.id.listViewVideos)
        tvCurrentSection = findViewById(R.id.tvCurrentSection)
        btnBackFolder = findViewById(R.id.btnBackFolder)
        fabLastVideo = findViewById(R.id.fabLastVideo)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnPlayUrl.setOnClickListener {
            val url = etVideoUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                openPlayer(url, "Network Stream")
            } else {
                Toast.makeText(this, "Video link paste karein", Toast.LENGTH_SHORT).show()
            }
        }

        btnBackFolder.setOnClickListener {
            showFolders()
        }

        fabLastVideo.setOnClickListener {
            val lastUri = prefs.lastVideoUri
            val lastTitle = prefs.lastVideoTitle ?: "Last Video"
            if (!lastUri.isNullOrEmpty()) {
                openPlayer(lastUri, lastTitle)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (currentFolder == null) {
                val selectedFolder = folderNames[position]
                showVideosInFolder(selectedFolder)
            } else {
                val video = folderMap[currentFolder]?.get(position) ?: return@setOnItemClickListener
                openPlayer(video.uri, video.title)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFolder != null) {
                    showFolders()
                } else {
                    finish()
                }
            }
        })

        checkPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()
        updateFloatingPlayButton()
    }

    private fun updateFloatingPlayButton() {
        val hasLast = !prefs.lastVideoUri.isNullOrEmpty()
        fabLastVideo.visibility = if (prefs.showFloatingPlay && hasLast) View.VISIBLE else View.GONE
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
        folderMap.clear()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
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
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val folder = cursor.getString(bucketCol) ?: "Internal Storage"
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                val list = folderMap.getOrPut(folder) { ArrayList() }
                list.add(VideoItem(name, uri, folder))
            }
        }
        showFolders()
    }

    private fun showFolders() {
        currentFolder = null
        tvCurrentSection.text = "Video Folders (${folderMap.size})"
        btnBackFolder.visibility = View.GONE

        folderNames.clear()
        displayList.clear()
        for ((folder, videos) in folderMap) {
            folderNames.add(folder)
            displayList.add("📁  $folder  (${videos.size})")
        }
        adapter.notifyDataSetChanged()
    }

    private fun showVideosInFolder(folderName: String) {
        currentFolder = folderName
        val videos = folderMap[folderName] ?: return
        tvCurrentSection.text = "$folderName (${videos.size})"
        btnBackFolder.visibility = View.VISIBLE

        displayList.clear()
        for (video in videos) {
            displayList.add("🎬  ${video.title}")
        }
        adapter.notifyDataSetChanged()
    }

    private fun openPlayer(uriString: String, title: String) {
        prefs.lastVideoUri = uriString
        prefs.lastVideoTitle = title
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URI", uriString)
            putExtra("VIDEO_TITLE", title)
        }
        startActivity(intent)
    }
}
