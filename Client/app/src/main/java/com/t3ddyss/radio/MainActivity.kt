package com.t3ddyss.radio

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.t3ddyss.radio.databinding.ActivityMainBinding
import com.t3ddyss.radio.models.domain.PlaylistAndTrack
import com.t3ddyss.radio.models.domain.Track
import com.t3ddyss.radio.services.AudioPlaybackService
import com.t3ddyss.radio.ui.collection.CollectionFragment
import com.t3ddyss.radio.utilities.DEBUG_TAG
import com.t3ddyss.radio.utilities.PLAYING_TRACK_CHANGED
import com.t3ddyss.radio.utilities.PLAYLIST_AND_TRACK


class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<PlaybackViewModel>()
    private lateinit var service: AudioPlaybackService

    private var _receiver: TrackChangedBroadcastReceiver? = null
    private val receiver get() = _receiver!!

    private lateinit var binding: ActivityMainBinding

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(DEBUG_TAG, "Service connected")

            if (service is AudioPlaybackService.AudioPlaybackServiceBinder) {
                this@MainActivity.service = service.getService()
                binding.playerControlView.player = this@MainActivity.service.getExoPlayer()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(DEBUG_TAG, "Service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Radio)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.host_fragment, CollectionFragment.newInstance())
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        _receiver = TrackChangedBroadcastReceiver()
        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(receiver, IntentFilter(PLAYING_TRACK_CHANGED))

        Intent(this, AudioPlaybackService::class.java).also {
            startService(it)
            bindService(
                it,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(receiver)
        _receiver = null
    }

    fun setTracksAndPlay(tracks: List<Track>, startIndex: Int, playlistId: Int) {
        service.setTracksAndPlay(tracks, startIndex, playlistId)
    }

    inner class TrackChangedBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(DEBUG_TAG, "Got broadcast")

            intent?.extras?.getParcelable<PlaylistAndTrack>(PLAYLIST_AND_TRACK)?.let {
                viewModel.updateCurrentlyPlayingTrack(it)
            }
        }
    }
}