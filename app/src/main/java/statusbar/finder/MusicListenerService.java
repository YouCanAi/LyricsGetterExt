package statusbar.finder;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import cn.lyric.getter.api.tools.Tools;
import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.model.Lyric;
import statusbar.finder.misc.Constants;

import cn.lyric.getter.api.tools.EventTools;

public class MusicListenerService extends NotificationListenerService {

    private static final int NOTIFICATION_ID_LRC = 1;

    private static final int MSG_LYRIC_UPDATE_DONE = 2;

    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;
    private NotificationManager mNotificationManager;

    private final ArrayList<String> mIgnoredPackageList = new ArrayList<>();
    private SharedPreferences mSharedPreferences;

    private Lyric mLyric;
    private String requiredLrcTitle;
    private Notification mLyricNotification;
    private long mLastSentenceFromTime = -1;

    private static String systemLanguage;
    private String drawBase64;

    private final BroadcastReceiver mIgnoredPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.BROADCAST_IGNORED_APP_CHANGED)) {
                updateIgnoredPackageList();
                unBindMediaListeners();
                bindMediaListeners();
            }
        }
    };

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_LYRIC_UPDATE_DONE && msg.getData().getString("title", "").equals(requiredLrcTitle)) {
                mLyric = (Lyric) msg.obj;

                if (mLyric == null) stopLyric();
            }
        }
    };

    private final Runnable mLyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaController == null || mMediaController.getPlaybackState().getState() != PlaybackState.STATE_PLAYING) {
                stopLyric();
                return;
            }
            updateLyric(mMediaController.getPlaybackState().getPosition());
            mHandler.postDelayed(mLyricUpdateRunnable, 250);
        }
    };

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state != null) {
                if (state.getState() == PlaybackState.STATE_PLAYING) {
                    startLyric();
                } else {
                    stopLyric();
                }
            }
        }

        @Override
        public void onSessionDestroyed() {
            stopLyric();
            super.onSessionDestroyed();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            stopLyric();
            mLyric = null;
            if (metadata == null) return;
            requiredLrcTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            if (!Constants.isLrcUpdateThreadRunning) {
                Constants.isLrcUpdateThreadRunning = true;
                new LrcUpdateThread(getApplicationContext(), mHandler, metadata).start();
            }
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
            if (controllers == null) return;
            for (MediaController controller : controllers) {
                if (mIgnoredPackageList.contains(controller.getPackageName())) continue;
                if (getMediaControllerPlaybackState(controller) == PlaybackState.STATE_PLAYING) {
                    mMediaController = controller;
                    break;
                }
            }
            if (mMediaController != null) {
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
            }
        }
    };

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    public MusicListenerService() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
//        StatusBarNotification statusBarNotification = sbn;
//        Notification notification = statusBarNotification.getNotification();
//            if (statusBarNotification.isClearable()){
//                if (notification != null) {
//                    String title = notification.extras.getString(Notification.EXTRA_TITLE);
//                    String text = notification.extras.getString(Notification.EXTRA_TEXT);
//                    if(title != null || text != null) {
//                        EventTools.INSTANCE.sendLyric(
//                                getApplicationContext(),
//                                title + " : " + text,
//                                true,
//                                Tools.INSTANCE.drawableToBase64(drawBase64),
//                                false,
//                                "",
//                                getPackageName(), 1
//                        );
//                    }
//                }
//        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        systemLanguage = Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
        drawBase64 = Tools.INSTANCE.drawableToBase64(getDrawable(R.drawable.ic_statusbar_icon));
        // Log.d("systemLanguage", systemLanguage);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mLyricNotification = buildLrcNotification();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mIgnoredPackageReceiver, new IntentFilter(Constants.BROADCAST_IGNORED_APP_CHANGED));
        updateIgnoredPackageList();
        bindMediaListeners();
    }

    @Override
    public void onListenerDisconnected() {
        stopLyric();
        unBindMediaListeners();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mIgnoredPackageReceiver);
        super.onListenerDisconnected();
    }

    private Notification buildLrcNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_LRC);
        builder.setSmallIcon(R.drawable.ic_music);
        builder.setOngoing(true);
        Notification notification = builder.build();
        notification.extras.putLong("ticker_icon", R.drawable.ic_music);
        notification.extras.putBoolean("ticker_icon_switch", false);
        notification.flags |= Constants.FLAG_ALWAYS_SHOW_TICKER;
        notification.flags |= Constants.FLAG_ONLY_UPDATE_TICKER;
        return notification;
    }

    private void bindMediaListeners() {
        ComponentName listener = new ComponentName(this, MusicListenerService.class);
        mMediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, listener);
        onActiveSessionsChangedListener.onActiveSessionsChanged(mMediaSessionManager.getActiveSessions(listener));
    }

    private void unBindMediaListeners() {
        if (mMediaSessionManager != null) mMediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
        if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
        mMediaController = null;
    }

    private void updateIgnoredPackageList() {
        mIgnoredPackageList.clear();
        String value = mSharedPreferences.getString(Constants.PREFERENCE_KEY_IGNORED_PACKAGES, "");
        String[] arr = value.split(";");
        for (String str : arr) {
            if (TextUtils.isEmpty(str)) continue;
            mIgnoredPackageList.add(str.trim());
        }
    }

    private void startLyric() {
        mLastSentenceFromTime = -1;
        mLyricNotification.tickerText = null;
        mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
        mHandler.post(mLyricUpdateRunnable);
    }

    private void stopLyric() {
        mHandler.removeCallbacks(mLyricUpdateRunnable);
        mNotificationManager.cancel(NOTIFICATION_ID_LRC);
        EventTools.INSTANCE.stopLyric(getApplicationContext());
    }

    private void updateLyric(long position) {
        if (mNotificationManager == null || mLyric == null) return;
        int delay;
        Lyric.Sentence sentence = LyricUtils.getSentence(mLyric.sentenceList, position);
        Lyric.Sentence transSentence = null;
        Log.d("mLyric.transSentenceList.size()", String.valueOf(mLyric.transSentenceList.size()));
        if (mLyric.transSentenceList.size() != 0) {
            transSentence = LyricUtils.getSentence(mLyric.transSentenceList, position);
        }
        int nextfound = LyricUtils.getSentenceIndex(mLyric.sentenceList, position,0 ,0) + 1;
        Lyric.Sentence nextSentence = null;
        if (nextfound < mLyric.sentenceList.size()) {
            nextSentence = mLyric.sentenceList.get(nextfound);
        }
        if (sentence == null) return;
        if (sentence.fromTime != mLastSentenceFromTime) {
            delay = 0;
            if (nextSentence != null) {
                delay = (int) (nextSentence.fromTime - sentence.fromTime) / 1000 - 3;
                if (delay < 0){delay = 0;}
            }
            mLyricNotification.tickerText = sentence.content;
            mLyricNotification.when = System.currentTimeMillis();
            mNotificationManager.notify(NOTIFICATION_ID_LRC, mLyricNotification);
            mLastSentenceFromTime = sentence.fromTime;
            if (Constants.isTransCheck && transSentence != null && !Objects.equals(transSentence.content, "")) {
                EventTools.INSTANCE.sendLyric(getApplicationContext(), sentence.content.trim() + "\n" +  transSentence.content.trim(), true, drawBase64, false, "", getPackageName(), delay);
            } else {
                EventTools.INSTANCE.sendLyric(getApplicationContext(), sentence.content.trim(), true, drawBase64, false, "", getPackageName(), delay);
            }
        }
    }

    private static class LrcUpdateThread extends Thread {
        private final Handler handler;
        private final MediaMetadata data;
        private final Context context;

        public LrcUpdateThread(Context context, Handler handler, MediaMetadata data) {
            super();
            this.data = data;
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            if (handler == null) return;
            Lyric lrc = LrcGetter.getLyric(context, data, systemLanguage);
            Message message = new Message();
            message.what = MSG_LYRIC_UPDATE_DONE;
            message.obj = lrc;
            Bundle bundle = new Bundle();
            bundle.putString("title", data.getString(MediaMetadata.METADATA_KEY_TITLE));
            message.setData(bundle);
            handler.sendMessage(message);
            Constants.isLrcUpdateThreadRunning = false;
        }
    }

}