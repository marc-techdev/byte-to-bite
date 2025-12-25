package com.example.testdesign.tutorial

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.RawRes
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.testdesign.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

@UnstableApi
class ScanTutorialBottomSheet : BottomSheetDialogFragment() {

    // ---- args ----
    private var argSourceType: String = TYPE_NONE
    private var argVideoResId: Int = 0
    private var argSubtitleResId: Int = 0
    private var argUrl: String? = null
    private var argSubtitleUrl: String? = null
    private var argStartMuted: Boolean = true

    // ---- views ----
    private var poster: ShapeableImageView? = null
    private var playerView: PlayerView? = null
    private var btnPlayPause: ImageButton? = null
    private var btnMute: ImageButton? = null

    // ---- player ----
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreArgs()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bs_scan_tutorial, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        poster = view.findViewById(R.id.ivPoster)
        playerView = view.findViewById(R.id.playerView)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnMute = view.findViewById(R.id.btnMute)

        poster?.apply {
            setImageResource(R.drawable.scan_tutorial_poster_placeholder)
            contentDescription = getString(R.string.cd_scan_tutorial_poster)
        }

        // Bottom buttons: both dismiss and mark as seen
        view.findViewById<MaterialButton>(R.id.btnGotIt)?.setOnClickListener {
            ScanHelpSeenLocal.markSeen(requireContext()); dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnSkip)?.setOnClickListener {
            ScanHelpSeenLocal.markSeen(requireContext()); dismiss()
        }

        // PlayerView config (controller off; small overlay buttons used)
        playerView?.apply {
            useController = false
            // FIT shows whole frame; change to RESIZE_MODE_ZOOM if you prefer crop-to-fill
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        // Ensure the proper view is visible right away
        val hasVideo = argSourceType != TYPE_NONE
        playerView?.visibility = if (hasVideo) View.VISIBLE else View.GONE
        poster?.visibility = if (hasVideo) View.GONE else View.VISIBLE

        btnPlayPause?.setOnClickListener { togglePlayPause() }
        btnMute?.setOnClickListener { toggleMute() }
    }

    override fun onStart() {
        super.onStart()
        // Expand the sheet fully so media/buttons aren’t hidden in collapsed state
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        if (argSourceType != TYPE_NONE) initPlayer()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    private fun initPlayer() {
        if (player != null || playerView == null) return

        val p = ExoPlayer.Builder(requireContext()).build()
        val item = when (argSourceType) {
            TYPE_RAW -> buildRawMediaItem(argVideoResId, argSubtitleResId.takeIf { it != 0 })
            TYPE_URL -> buildUrlMediaItem(argUrl.orEmpty(), argSubtitleUrl)
            else -> return
        }

        playerView!!.player = p
        p.setMediaItem(item)
        p.prepare()
        p.playWhenReady = true
        p.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        p.volume = if (argStartMuted) 0f else 1f

        refreshControls(p)

        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = refreshControls(p)
            override fun onPlaybackStateChanged(playbackState: Int) = refreshControls(p)
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // Keep internal frame aspect correct inside our 16:9 box
                val frame = playerView!!.findViewById<AspectRatioFrameLayout>(
                    androidx.media3.ui.R.id.exo_content_frame
                )
                if (videoSize.height > 0) {
                    frame?.setAspectRatio(videoSize.width.toFloat() / videoSize.height.toFloat())
                }
            }
        })

        player = p
    }

    private fun refreshControls(p: ExoPlayer?) {
        if (p == null) {
            btnPlayPause?.setImageResource(android.R.drawable.ic_media_play)
            btnMute?.setImageResource(android.R.drawable.ic_lock_silent_mode)
            return
        }
        btnPlayPause?.setImageResource(
            if (p.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        btnMute?.setImageResource(
            if (p.volume <= 0f) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_lock_silent_mode_off
        )
    }

    private fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
        refreshControls(p)
    }

    private fun toggleMute() {
        val p = player ?: return
        p.volume = if (p.volume > 0f) 0f else 1f
        refreshControls(p)
    }

    private fun releasePlayer() {
        playerView?.player = null
        player?.release()
        player = null
    }

    @Suppress("DEPRECATION")
    private fun buildRawMediaItem(
        @RawRes resId: Int,
        @RawRes subtitleResId: Int?
    ): MediaItem {
        val videoUri = RawResourceDataSource.buildRawResourceUri(resId)
        val b = MediaItem.Builder().setUri(videoUri)
        subtitleResId?.let { subRes ->
            val subUri = RawResourceDataSource.buildRawResourceUri(subRes)
            b.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(subUri)
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .build()
                )
            )
        }
        return b.build()
    }

    private fun buildUrlMediaItem(url: String, subtitleUrl: String?): MediaItem {
        val b = MediaItem.Builder().setUri(url.toUri())
        subtitleUrl?.let { sUrl ->
            b.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(sUrl.toUri())
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .build()
                )
            )
        }
        return b.build()
    }

    private fun restoreArgs() {
        arguments?.let { a ->
            argSourceType = a.getString(ARG_SOURCE_TYPE, TYPE_NONE)
            argVideoResId = a.getInt(ARG_RES_ID, 0)
            argSubtitleResId = a.getInt(ARG_SUBTITLE_RES_ID, 0)
            argUrl = a.getString(ARG_URL)
            argSubtitleUrl = a.getString(ARG_SUBTITLE_URL)
            argStartMuted = a.getBoolean(ARG_START_MUTED, true)
        }
    }

    companion object {
        const val TAG = "scan_tutorial"

        private const val ARG_SOURCE_TYPE = "source_type"
        private const val ARG_RES_ID = "res_id"
        private const val ARG_SUBTITLE_RES_ID = "subtitle_res_id"
        private const val ARG_URL = "url"
        private const val ARG_SUBTITLE_URL = "subtitle_url"
        private const val ARG_START_MUTED = "start_muted"

        private const val TYPE_NONE = "none"
        private const val TYPE_RAW = "raw"
        private const val TYPE_URL = "url"

        /** Gate: show prompt first unless the user already chose. */
        fun show(host: FragmentActivity, force: Boolean = false) {
            if (!force && !ScanHelpSeenLocal.isSeen(host)) {
                ScanTutorialPrompt.maybeShow(host, VideoSource.None)
                return
            }
            val fm = host.supportFragmentManager
            if (fm.findFragmentByTag(TAG) == null) {
                ScanTutorialBottomSheet().apply {
                    arguments = Bundle().apply {
                        putString(ARG_SOURCE_TYPE, TYPE_NONE)
                        putBoolean(ARG_START_MUTED, true)
                    }
                }.show(fm, TAG)
            }
        }

        fun showRaw(
            host: FragmentActivity,
            @RawRes videoResId: Int,
            @RawRes subtitleResId: Int = 0,
            startMuted: Boolean = true,
            force: Boolean = false
        ) {
            if (!force && !ScanHelpSeenLocal.isSeen(host)) {
                ScanTutorialPrompt.maybeShow(
                    host,
                    VideoSource.RawResSource(videoResId, if (subtitleResId == 0) null else subtitleResId)
                )
                return
            }
            val fm = host.supportFragmentManager
            if (fm.findFragmentByTag(TAG) == null) {
                ScanTutorialBottomSheet().apply {
                    arguments = Bundle().apply {
                        putString(ARG_SOURCE_TYPE, TYPE_RAW)
                        putInt(ARG_RES_ID, videoResId)
                        putInt(ARG_SUBTITLE_RES_ID, subtitleResId)
                        putBoolean(ARG_START_MUTED, startMuted)
                    }
                }.show(fm, TAG)
            }
        }

        fun showUrl(
            host: FragmentActivity,
            url: String,
            subtitleUrl: String? = null,
            startMuted: Boolean = true,
            force: Boolean = false
        ) {
            if (!force && !ScanHelpSeenLocal.isSeen(host)) {
                ScanTutorialPrompt.maybeShow(
                    host,
                    VideoSource.UrlSource(url, subtitleUrl)
                )
                return
            }
            val fm = host.supportFragmentManager
            if (fm.findFragmentByTag(TAG) == null) {
                ScanTutorialBottomSheet().apply {
                    arguments = Bundle().apply {
                        putString(ARG_SOURCE_TYPE, TYPE_URL)
                        putString(ARG_URL, url)
                        subtitleUrl?.let { putString(ARG_SUBTITLE_URL, it) }
                        putBoolean(ARG_START_MUTED, startMuted)
                    }
                }.show(fm, TAG)
            }
        }
    }
}
