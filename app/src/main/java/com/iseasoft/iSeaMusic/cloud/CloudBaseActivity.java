package com.iseasoft.iSeaMusic.cloud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.cloud.dropbox.DropboxActivity;
import com.iseasoft.iSeaMusic.cloud.dropbox.DropboxClientFactory;
import com.iseasoft.iSeaMusic.utils.NetUtil;

import java.io.IOException;


/**
 * Created by xuanhien091 on 10/20/17.
 */

public abstract class CloudBaseActivity extends PreferenceActivity {
    public static final String ACCOUNT_PREFS_NAME = "dropbox_prefs";
    public static final String ACCESS_TOKEN = "access-token";
    public static final String STORE_UID = "store-uid";
    public ProgressDialog mProgressDialog;

    public enum IconPlay {
        PLAY(R.drawable.ic_play_white_36dp), PAUSE(R.drawable.ic_pause_white_36dp);
        private final int image;

        private IconPlay(int drawbleId) {
            this.image = drawbleId;
        }

        public int getImage() {
            return image;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString(ACCESS_TOKEN, null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString(ACCESS_TOKEN, accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }

        String uid = Auth.getUid();
        String storedUid = prefs.getString(STORE_UID, null);
        if (uid != null && !uid.equals(storedUid)) {
            prefs.edit().putString(STORE_UID, uid).apply();
        }
    }

    private void initAndLoadData(String accessToken) {
        DropboxClientFactory.init(accessToken);
        loadData();
    }

    protected abstract void loadData();

    protected boolean hasToken() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString(ACCESS_TOKEN, null);
        return accessToken != null;
    }

    protected void clearAccessToken(DropboxClientFactory.CallBack callback) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(ACCESS_TOKEN).commit();
        prefs.edit().remove(STORE_UID).commit();
        AuthActivity.result = null;
        DropboxClientFactory.revokeClient(callback);
    }

    public void playMusicWithUrl(String audioFile, final Context context, final MediaPlayer mediaPlayer, final AudioManager.OnAudioFocusChangeListener focusChangeListener, final ProgressDialog dialog) {
        if (null != mediaPlayer) {
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.setDataSource(audioFile);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();
            // execute this code at the end of asynchronous media player preparation
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    musicActive(focusChangeListener);
                    mp.start();
                    rotateDisk();
                    unHiddenPlayBack();
                    hiddenPause();
                    hideProgessDialog(dialog);

                }
            });
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    // TODO Auto-generated method stub
                    Log.i("Buffering", "" + percent);
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                    hideProgessDialog(dialog);
                    if (!NetUtil.hasNetworkConnection(context)) {
                        showErrorPlayMusic(context, getString(R.string.cloud_communicate_error_title), getString(R.string.cloud_communicate_error_content));
                    } else {
                        showErrorPlayMusic(context, getString(R.string.cloud_playback_error_title), getString(R.string.cloud_playback_error_content));
                    }
                    return false;
                }
            });
        } catch (IOException e) {
            if (!NetUtil.hasNetworkConnection(context)) {
                showErrorPlayMusic(context, getString(R.string.cloud_communicate_error_title), getString(R.string.cloud_communicate_error_content));
            } else {
                showErrorPlayMusic(context, getString(R.string.cloud_playback_error_title), getString(R.string.cloud_playback_error_content));
            }
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showErrorPlayMusic(context, getString(R.string.cloud_playback_error_title), getString(R.string.cloud_playback_error_content));
            e.printStackTrace();
        } catch (IllegalStateException e) {
            showErrorPlayMusic(context, getString(R.string.cloud_playback_error_title), getString(R.string.cloud_playback_error_content));
            e.printStackTrace();
        }
    }

    public void musicActive(AudioManager.OnAudioFocusChangeListener focusChangeListener) {
        AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int result = manager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // other app had stopped playing song now , so u can do u stuff now .
        }
    }

    public void rotateDisk() {
        ImageView disk = (ImageView) findViewById(R.id.imgDisk);
        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        rotate.setDuration(10000);
        rotate.setRepeatCount(Animation.INFINITE);
        disk.startAnimation(rotate);
    }

    public void unHiddenPlayBack() {
        View playBack = findViewById(R.id.rl_playback);
        playBack.setVisibility(View.VISIBLE);

        View view = findViewById(R.id.actual_list_view);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view
                .getLayoutParams();
        layoutParams.addRule(RelativeLayout.ABOVE, R.id.rl_playback);
        view.setLayoutParams(layoutParams);
    }

    public void hiddenPause() {
        ImageButton view = (ImageButton) findViewById(R.id.imgBtnPlay);
        view.setImageResource(DropboxActivity.IconPlay.PAUSE.getImage());
    }

    public void hideProgessDialog(ProgressDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void hiddenPlayBack() {
        View playBack = findViewById(R.id.rl_playback);
        playBack.setVisibility(View.GONE);
    }

    public void hiddenPlay() {
        ImageButton view = (ImageButton) findViewById(R.id.imgBtnPlay);
        view.setImageResource(DropboxActivity.IconPlay.PLAY.getImage());
    }

    public void showErrorPlayMusic(Context context, String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(content);

        builder.setPositiveButton(getString(R.string.cast_tracks_chooser_dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void showProgressDialogDownload(Context context, String title) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(title);
        mProgressDialog.setProgressNumberFormat(null);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.show();
    }

    public void showProgressUpdateDownloadSingleFile(long size, String title, long length) {
        int percent = Math.round(length / (size * 1.0F) * 100);
        mProgressDialog.setMessage(title);
        mProgressDialog.setProgress(percent);
    }

    public void showProgressUpdateDownloadMultiFile(long sizeItem, String title, long length, int positionFile,long sizeList) {
        int percent = Math.round(length / (sizeItem * 1.0F) * 100);
        mProgressDialog.setMessage(title);
        mProgressDialog.setProgress(percent);
        mProgressDialog.setProgressNumberFormat((positionFile + 1) + "/" + sizeList);
    }

    public void hideProgessDialogDownload() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
