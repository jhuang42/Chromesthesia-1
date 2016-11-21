package com.example.matt.chromesthesia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;

public class SpotiPlayer extends Fragment implements Player.NotificationCallback, ConnectionStateCallback {

    public static PlaybackState mCurrentPlaybackState;


    /**
     * UI controls which may only be enabled after the player has been initialized,
     * (or effectively, after the user has logged in).
     */
    public static final int[] REQUIRES_INITIALIZED_STATE = {
            R.id.play_track_button,
            R.id.play_mono_track_button,
            R.id.play_48khz_track_button,
            R.id.play_album_button,
            R.id.play_playlist_button,
            R.id.pause_button,
            R.id.seek_button,
            R.id.low_bitrate_button,
            R.id.normal_bitrate_button,
            R.id.high_bitrate_button,
            R.id.seek_edittext,
    };

    /**
     * UI controls which should only be enabled if the player is actively playing.
     */
    public static final int[] REQUIRES_PLAYING_STATE = {
            R.id.skip_next_button,
            R.id.skip_prev_button,
            R.id.queue_song_button,
            R.id.toggle_shuffle_button,
            R.id.toggle_repeat_button,
    };

    /**
     * Used to log messages to a {@link android.widget.TextView} in this activity.
     */
    private TextView mStatusText;
    private TextView mMetadataText;
    private EditText mSeekEditText;
    /**
     * Used to scroll the {@link #mStatusText} to the bottom after updating text.
     */

    Chromesthesia chromesthesia;
    private static final String REDIRECT_URI = "http://iacaneda.com/";
    public static final String CLIENT_ID = "2de25702544a445d93cfa7d9a7c9c838";
    public static final int REQUEST_CODE = 1337;
    protected static Player mPlayer;
    public static final String TAG = "SpotifyMusic";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_SONG_URI = "spotify:track:6KywfgRqvgvfJc3JRwaZdZ";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_SONG_MONO_URI = "spotify:track:1FqY3uJypma5wkYw66QOUi";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_SONG_48kHz_URI = "spotify:track:3wxTNS3aqb9RbBLZgJdZgH";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_PLAYLIST_URI = "spotify:user:spotify:playlist:2yLXxKhhziG2xzy7eyD4TD";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_ALBUM_URI = "spotify:album:2lYmxilk8cXJlxxXmns1IU";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String TEST_QUEUE_SONG_URI = "spotify:track:5EEOjaJyWvfMglmEwf9bG3";
    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to
     * {@link SpotifyPlayer#setConnectivityStatus(Player.OperationCallback, Connectivity)}
     * Note that this implies <pre>android.permission.ACCESS_NETWORK_STATE</pre> must be
     * declared in the manifest. Not setting the correct network state in the SDK may
     * result in strange behavior.
     */
    private BroadcastReceiver mNetworkStateReceiver;
    private static Metadata mMetadata;
    private ScrollView mStatusTextScrollView;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }


    private static final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess: OK!");
        }

        @Override
        public void onError(Error error) {
            Log.d(TAG, "onError: " + error);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View rootView = inflater.inflate(R.layout.activity_demo, container, false);
        // Get a reference to any UI widgets that we'll need to use later
        mStatusText = (TextView) rootView.findViewById(R.id.status_text);
        mMetadataText = (TextView) rootView.findViewById(R.id.metadata);
        mSeekEditText = (EditText) rootView.findViewById(R.id.seek_edittext);
        mStatusTextScrollView = (ScrollView) rootView.findViewById(R.id.status_text_container);

        Button loginButton = (Button) rootView.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLoginButtonClicked(v);
            }
        });

        Button playTrackButton = (Button) rootView.findViewById(R.id.play_track_button);
        playTrackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPlayButtonClicked(v);
            }
        });

        Button playAlbumButton = (Button) rootView.findViewById(R.id.play_album_button);
        playAlbumButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPlayButtonClicked(v);
            }
        });

        Button playPlaylistButton = (Button) rootView.findViewById(R.id.play_playlist_button);
        playPlaylistButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPlayButtonClicked(v);
            }
        });

        Button pauseResumeButton = (Button) rootView.findViewById(R.id.pause_button);
        pauseResumeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPauseButtonClicked(v);
                System.out.println("paused after button click");
            }
        });

        updateView(rootView);
        Log.d(TAG, "onCreateView: Ready");
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    System.out.println("Authentication Complete, now trying to initialize mPlayer");
                    onAuthenticationComplete(response);
                    break;

                // Auth flow returned an error
                case ERROR:
                    Log.d(TAG,"Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.d(TAG,"Auth result: " + response.getType());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayer != null) {
                    Connectivity connectivity = getNetworkConnectivity(getContext());
                    Log.d(TAG, "Network state changed: " + connectivity.toString());
                    mPlayer.setConnectivityStatus(mOperationCallback, connectivity);
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mNetworkStateReceiver, filter);

        if (mPlayer != null) {
            mPlayer.addNotificationCallback((Player.NotificationCallback) getActivity());
            mPlayer.addConnectionStateCallback((ConnectionStateCallback) getActivity());
        }
    }

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    private void openLoginWindow() {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"})
                .build();

        AuthenticationClient.openLoginActivity(getActivity(), REQUEST_CODE, request);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void updateView(View view) {
        boolean loggedIn = isLoggedIn();

        // Login button should be the inverse of the logged in state
        Button loginButton = (Button) view.findViewById(R.id.login_button);
        loginButton.setText(loggedIn ? R.string.logout_button_label : R.string.login_button_label);

        // Set enabled for all widgets which depend on initialized state
        for (int id : REQUIRES_INITIALIZED_STATE) {
            view.setEnabled(loggedIn);
        }

        // Same goes for the playing state
        boolean playing = loggedIn && mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying;
        for (int id : REQUIRES_PLAYING_STATE) {
            view.findViewById(id).setEnabled(playing);
        }

        if (mMetadata != null) {
            view.findViewById(R.id.skip_next_button).setEnabled(mMetadata.nextTrack != null);
            view.findViewById(R.id.skip_prev_button).setEnabled(mMetadata.prevTrack != null);
            view.findViewById(R.id.pause_button).setEnabled(mMetadata.currentTrack != null);
        }

        final ImageView coverArtView = (ImageView) getActivity().findViewById(R.id.cover_art);
        if (mMetadata != null && mMetadata.currentTrack != null) {
            final String durationStr = String.format(" (%dms)", mMetadata.currentTrack.durationMs);
            mMetadataText.setText(mMetadata.contextName + "\n" + mMetadata.currentTrack.name + " - " + mMetadata.currentTrack.artistName + durationStr);

            Picasso.with(getContext())
                    .load(mMetadata.currentTrack.albumCoverWebUrl)
                    .transform(new com.squareup.picasso.Transformation() {
                        @Override
                        public Bitmap transform(Bitmap source) {
                            // really ugly darkening trick
                            final Bitmap copy = source.copy(source.getConfig(), true);
                            source.recycle();
                            final Canvas canvas = new Canvas(copy);
                            canvas.drawColor(0xbb000000);
                            return copy;
                        }

                        @Override
                        public String key() {
                            return "darken";
                        }
                    })
                    .into(coverArtView);
        } else {
            mMetadataText.setText("<nothing is playing>");
//            coverArtView.setBackground(null);
        }

    }

    public void onLoginButtonClicked(View view) {
        if (!isLoggedIn()) {
            Log.d(TAG, "Logging in");
            final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                    .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"})
                    .build();
            AuthenticationClient.openLoginActivity(getActivity(), REQUEST_CODE, request);
        } else {
            mPlayer.logout();
        }
    }




    private void onAuthenticationComplete(AuthenticationResponse authResponse) {
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        Log.d(TAG,"Got authentication token");
        if (mPlayer == null) {
            System.out.println("Spotify player is null. Now trying to instantiate the player");
            Config playerConfig = new Config(getContext(), authResponse.getAccessToken(), CLIENT_ID);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    Log.d(TAG,"-- Player initialized --");
                    mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(getActivity()));
                    mPlayer.addNotificationCallback((Player.NotificationCallback) getActivity());
                    mPlayer.addConnectionStateCallback((ConnectionStateCallback) getActivity());
                    System.out.println("Spotify player is fully connected.");
                    // Trigger UI refresh
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        updateView(getView());
                    }
                }

                @Override
                public void onError(Throwable error) {
                    Log.d(TAG,"Error in initialization: " + error.getMessage());
                    System.out.println("Spotify player is not connected");
                }
            });
        } else {
            mPlayer.login(authResponse.getAccessToken());
        }
    }

    public static boolean isLoggedIn() {
        return mPlayer != null && isLoggedIn();
    }

    //Start of onclick methods for the Spotify window.


    public static void onPlayButtonClicked(View view) {
        if(mPlayer == null){System.out.println("11/20/16 mPlayer is null");}
        String uri;
        switch (view.getId()) {
            case R.id.play_track_button:
                uri = TEST_SONG_URI;
                break;
            case R.id.play_mono_track_button:
                uri = TEST_SONG_MONO_URI;
                break;
            case R.id.play_48khz_track_button:
                uri = TEST_SONG_48kHz_URI;
                break;
            case R.id.play_playlist_button:
                uri = TEST_PLAYLIST_URI;
                break;
            case R.id.play_album_button:
                uri = TEST_ALBUM_URI;
                break;
            default:
                throw new IllegalArgumentException("View ID does not have an associated URI to play");
        }

        Log.d(TAG,"Starting playback for " + uri);
        mPlayer.playUri(mOperationCallback, uri, 0, 0);
    }

    public static void onPauseButtonClicked(View view) {
        if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
            mPlayer.pause(mOperationCallback);
        } else {
            mPlayer.resume(mOperationCallback);
        }
    }

    public void onSkipToPreviousButtonClicked(View view) {
        mPlayer.skipToPrevious(mOperationCallback);
    }

    public void onSkipToNextButtonClicked(View view) {
        mPlayer.skipToNext(mOperationCallback);
    }

    public void onQueueSongButtonClicked(View view) {
        mPlayer.queue(mOperationCallback, TEST_QUEUE_SONG_URI);
        Toast toast = Toast.makeText(getActivity(), R.string.song_queued_toast, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onToggleShuffleButtonClicked(View view) {
        mPlayer.setShuffle(mOperationCallback, !mCurrentPlaybackState.isShuffling);
    }

    public void onToggleRepeatButtonClicked(View view) {
        mPlayer.setRepeat(mOperationCallback, !mCurrentPlaybackState.isRepeating);
    }

    public void onSeekButtonClicked(View view) {
        final Integer seek = Integer.valueOf(mSeekEditText.getText().toString());
        mPlayer.seekToPosition(mOperationCallback, seek);
    }

    public void onLowBitrateButtonPressed(View view) {
        mPlayer.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_LOW);
    }

    public void onNormalBitrateButtonPressed(View view) {
        mPlayer.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_NORMAL);
    }

    public void onHighBitrateButtonPressed(View view) {
        mPlayer.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_HIGH);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that calling Spotify.destroyPlayer() will also remove any callbacks on whatever
        // instance was passed as the refcounted owner. So in the case of this particular example,
        // it's not strictly necessary to call these methods, however it is generally good practice
        // and also will prevent your application from doing extra work in the background when
        // paused.
        if (mPlayer != null) {
            mPlayer.removeNotificationCallback((Player.NotificationCallback) getActivity());
            mPlayer.removeConnectionStateCallback((ConnectionStateCallback) getActivity());
        }
    }


    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            updateView(getView());
        }
    }
    @Override
    public void onDestroy(){
        //Spotify player:
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            updateView(getView());
        }
    }

    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        // Remember kids, always use the English locale when changing case for non-UI strings!
        // Otherwise you'll end up with mysterious errors when running in the Turkish locale.
        // See: http://java.sys-con.com/node/46241
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        Log.i(TAG, "Player state: " + mCurrentPlaybackState);
        Log.i(TAG, "Metadata: " + mMetadata);
        updateView(getView());
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

}

