package com.example.mxclone

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var tvGestureInfo: TextView
    private lateinit var topButtonsLayout: LinearLayout
    private lateinit var audioManager: AudioManager
    private var resizeModeIndex = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        tvGestureInfo = findViewById(R.id.tvGestureInfo)
        topButtonsLayout = findViewById(R.id.topButtonsLayout)
        val btnAspect = findViewById<Button>(R.id.btnAspect)
        val btnPip = findViewById<Button>(R.id.btnPip)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Status bar aur Navigation bar hide karein (Immersive Fullscreen)
        hideSystemUI()

        // FIT aur PIP buttons ko player controls ke saath auto-hide/show karein
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            topButtonsLayout.visibility = visibility
        })

        val videoUri = intent.getStringExtra("VIDEO_URI") ?: return
        initializePlayer(videoUri)

        btnAspect.setOnClickListener {
            resizeModeIndex = (resizeModeIndex + 1) % 3
            when (resizeModeIndex) {
                0 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    btnAspect.text = "Fit"
                }
                1 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    btnAspect.text = "Zoom"
                }
                2 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    btnAspect.text = "Stretch"
                }
            }
        }

        btnPip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }

        setupGestures()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun initializePlayer(uriString: String) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = playerView.width
                player?.let {
                    if (e.x < screenWidth / 2) {
                        it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
                        showIndicator("-10s")
                    } else {
                        it.seekTo(it.currentPosition + 10000)
                        showIndicator("+10s")
                    }
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false

                if (abs(distanceY) > abs(distanceX)) {
                    // Vertical Swipe: Brightness (Left) ya Volume (Right)
                    val screenWidth = playerView.width
                    val deltaPercent = distanceY / playerView.height

                    if (e1.x < screenWidth / 2) {
                        adjustBrightness(deltaPercent)
                    } else {
                        adjustVolume(deltaPercent)
                    }
                    return true
                } else {
                    // Horizontal Swipe: Video aage-piche (Seek) karein
                    player?.let { exoPlayer ->
                        val duration = exoPlayer.duration
                        if (duration > 0) {
                            // Left swipe = piche, Right swipe = aage
                            val seekDelta = (-distanceX * 150).toLong()
                            val newPosition = (exoPlayer.currentPosition + seekDelta).coerceIn(0, duration)
                            exoPlayer.seekTo(newPosition)
                            showIndicator("${formatTime(newPosition)} / ${formatTime(duration)}")
                        }
                    }
                    return true
                }
            }
        })

        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                tvGestureInfo.postDelayed({ tvGestureInfo.visibility = View.GONE }, 600)
            }
            false
        }
    }

    private fun adjustBrightness(delta: Float) {
        val lp = window.attributes
        var brightness = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
        brightness = (brightness + delta).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = brightness
        window.attributes = lp
        showIndicator("Brightness: ${(brightness * 100).toInt()}%")
    }

    private fun adjustVolume(delta: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val change = if (delta > 0) 1 else -1
        val newVol = (curVol + change).coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        val percent = ((newVol.toFloat() / maxVol) * 100).toInt()
        showIndicator("Volume: $percent%")
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun showIndicator(text: String) {
        tvGestureInfo.text = text
        tvGestureInfo.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            player?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
