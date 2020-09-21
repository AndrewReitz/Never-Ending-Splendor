/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package never.ending.splendor.app.ui

import android.annotation.SuppressLint
import android.app.ActivityManager.TaskDescription
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.RemoteException
import android.preference.PreferenceManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import never.ending.splendor.R
import never.ending.splendor.app.MusicService
import never.ending.splendor.app.inject
import never.ending.splendor.app.utils.NetworkHelper
import timber.log.Timber
import javax.inject.Inject

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
abstract class BaseActivity : ActionBarCastActivity(), MediaBrowserProvider {
    private var mediaBrowser: MediaBrowserCompat? = null
    private var controlsFragment: PlaybackControlsFragment? = null

    @Inject lateinit var googleApiAvailability: GoogleApiAvailability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Activity onCreate")
        this.inject()
        checkPlayServices()

        // Since our app icon has the same color as colorPrimary, our entry in the Recent Apps
        // list gets weird. We need to change either the icon or the color
        // of the TaskDescription.
        @Suppress("DEPRECATION") // deprecated in api 29.
        val taskDesc = TaskDescription(
                title.toString(),
                BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_white),
                ContextCompat.getColor(this, R.color.primaryColor))
        setTaskDescription(taskDesc)

        // Connect a media browser just to get the media session token. There are other ways
        // this can be done, for example by sharing the session token directly.
        mediaBrowser = MediaBrowserCompat(this,
                ComponentName(this, MusicService::class.java), mConnectionCallback, null)
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(): Boolean {
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    val supportMediaController: MediaControllerCompat? get() = MediaControllerCompat.getMediaController(this)

    override fun onStart() {
        super.onStart()
        Timber.d("Activity onStart")
        controlsFragment = supportFragmentManager.findFragmentById(R.id.fragment_playback_controls) as PlaybackControlsFragment?
        checkNotNull(controlsFragment) { "Mising fragment with id 'controls'. Cannot continue." }
        hidePlaybackControls()
        mediaBrowser!!.connect()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Activity onStop")
        val mediaController = MediaControllerCompat.getMediaController(this)
        mediaController?.unregisterCallback(mMediaControllerCallback)
        mediaBrowser!!.disconnect()
    }

    override fun getMediaBrowser(): MediaBrowserCompat {
        return mediaBrowser!!
    }

    protected open fun onMediaControllerConnected() {
        // empty implementation, can be overridden by clients.
    }

    protected fun showPlaybackControls() {
        Timber.d("showPlaybackControls")
        if (NetworkHelper.isOnline(this)) {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                    .show(controlsFragment!!)
                    .commit()
        }
    }

    protected fun hidePlaybackControls() {
        Timber.d("hidePlaybackControls")
        supportFragmentManager.beginTransaction()
                .hide(controlsFragment!!)
                .commit()
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible.
     */
    protected fun shouldShowControls(): Boolean {
        val mediaController = MediaControllerCompat.getMediaController(this)
        return if (mediaController == null || mediaController.metadata == null || mediaController.playbackState == null) {
            false
        } else when (mediaController.playbackState.state) {
            PlaybackStateCompat.STATE_ERROR, PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_STOPPED -> false
            else -> true
        }
    }

    @Throws(RemoteException::class)
    private fun connectToSession(token: MediaSessionCompat.Token) {
        val mediaController = MediaControllerCompat(this, token)
        MediaControllerCompat.setMediaController(this, mediaController)
        mediaController.registerCallback(mMediaControllerCallback)
        if (shouldShowControls()) {
            showPlaybackControls()
        } else {
            Timber.d("connectionCallback.onConnected: hiding controls because metadata is null")
            hidePlaybackControls()
        }
        if (controlsFragment != null) {
            controlsFragment!!.onConnected()
        }
        onMediaControllerConnected()
    }

    // Callback that ensures that we are showing the controls
    private val mMediaControllerCallback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        @SuppressLint("BinaryOperationInTimber")
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            if (shouldShowControls()) {
                showPlaybackControls()
            } else {
                Timber.d("mediaControllerCallback.onPlaybackStateChanged: " +
                        "hiding controls because state is %s", state.state)
                hidePlaybackControls()
            }
        }

        @SuppressLint("BinaryOperationInTimber")
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            if (shouldShowControls()) {
                showPlaybackControls()
            } else {
                Timber.d("mediaControllerCallback.onMetadataChanged: " +
                        "hiding controls because metadata is null")
                hidePlaybackControls()
            }
        }
    }
    private val mConnectionCallback: MediaBrowserCompat.ConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Timber.d("onConnected")
            try {
                connectToSession(mediaBrowser!!.sessionToken)
            } catch (e: RemoteException) {
                Timber.e(e, "could not connect media controller")
                hidePlaybackControls()
            }
        }
    }
}
