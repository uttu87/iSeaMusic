package com.iseasoft.iSeaMusic.cloud.dropbox;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetTemporaryLinkResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.cloud.CloudBaseActivity;
import com.iseasoft.iSeaMusic.cloud.api.CloudDownloadManager;
import com.iseasoft.iSeaMusic.cloud.drive.ResultsAdapter;
import com.iseasoft.iSeaMusic.cloud.interfaces.OnDownloadListener;
import com.iseasoft.iSeaMusic.cloud.interfaces.ResultAdapterCallback;
import com.iseasoft.iSeaMusic.cloud.services.ContentService;
import com.iseasoft.iSeaMusic.utils.NetUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Created by @haipham on 10/20/17.
 */

public class DropboxActivity extends CloudBaseActivity implements OnItemClickListener {
    public final static String EXTRA_PATH = "path";
    public static final String EXTRA_PATH_FOLDER = "path_folder";
    public static final String PATH_DEFAULT = "";
    private String mPath;
    private ResultsAdapter mFilesAdapter;
    private ListView mListView;
    private HorizontalScrollView mLimiterScroller;
    private final ArrayList<String> mStackFolder = new ArrayList<String>();
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private ProgressDialog mDialog;
    private String mCurAudioFile = null;
    private ArrayList<CloudDownloadManager> mThreads = new ArrayList<>();
    private OnDownloadListener mOnDownloadListener;

    private AudioManager.OnAudioFocusChangeListener focusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
                            try {
                                // Lower the volume while ducking.
                                mMediaPlayer.setVolume(0.2f, 0.2f);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            break;
                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
                            try {
                                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                                    mMediaPlayer.pause();
                                    hiddenPlay();
                                }
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            break;

                        case (AudioManager.AUDIOFOCUS_LOSS):
                            try {
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.pause();
                                    hiddenPlay();
                                }
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            break;

                        case (AudioManager.AUDIOFOCUS_GAIN):
                            // Return the volume to normal and resume if paused.
                            if (mMediaPlayer != null) {
                                mMediaPlayer.setVolume(1f, 1f);
                                mMediaPlayer.start();
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox);

        mListView = (ListView) findViewById(R.id.actual_list_view);
        mLimiterScroller = (HorizontalScrollView) findViewById(R.id.new_limiter_scroller);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        mPath = path == null ? PATH_DEFAULT : path;
        mFilesAdapter = new ResultsAdapter(this, null, new ResultAdapterCallback() {
            @Override
            public void onFolderClicked(int position, FolderMetadata folder) {
                mStackFolder.add(folder.getPathLower());
                mFilesAdapter.setIsRoot(false);
                listFolder(folder.getPathLower());
            }

            @Override
            public void onFileClicked(int position, FileMetadata file) {
                if (!NetUtil.hasNetworkConnection(DropboxActivity.this)) {
                    Toast.makeText(DropboxActivity.this, getString(R.string.cloud_network_error), Toast.LENGTH_SHORT).show();
                }
                new DropboxPrepareMetadata(file.getPathLower()).execute();
            }

            @Override
            public void onRootClicked() {
                onBackFolder();
            }

            @Override
            public void onDownloadFileCallback(Object object) {
                downloadFileWithMetadata((Metadata) object);
            }
        });
        mListView.setAdapter(mFilesAdapter);
        mFilesAdapter.setIsRoot(true);
        mListView.setOnItemClickListener(this);
    }

    private void downloadFileWithMetadata(Metadata metadata) {
        if (metadata instanceof FileMetadata) {
            final FileMetadata fileMetadata = (FileMetadata) metadata;
            int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

            final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    NUMBER_OF_CORES * 2,
                    NUMBER_OF_CORES * 2,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>()
            );
            showProgressDialogDownload(DropboxActivity.this, fileMetadata.getName());
            mOnDownloadListener = new OnDownloadListener() {

                public void onDownloadConnecting() {

                }

                @Override
                public void onDownloadProgress(final int positionFile, long finished, final long length) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                showProgressUpdateDownloadSingleFile(fileMetadata.getSize(), fileMetadata.getName(), length);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onDownloadCompleted() {
                    hideProgessDialogDownload();
                }

                @Override
                public void onDownloadPaused() {

                }

                @Override
                public void onDownloadCanceled() {

                }

                @Override
                public void onDownloadFailed() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            hideProgessDialogDownload();
                            confirmDownloadError();
                        }
                    });

                }

                @Override
                public void onDownloadCompletedWithFile(File file) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                        final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                        final Uri contentUri = Uri.fromFile(file);
//                        scanIntent.setData(contentUri);
//                        sendBroadcast(scanIntent);
//                    } else {
//                        final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
//                        sendBroadcast(intent);
//                    }
                    LocalBroadcastManager.getInstance(DropboxActivity.this).sendBroadcast(new Intent(ContentService.SYNC_TASK_STOPPED_ACTION));
                    MediaScannerConnection.scanFile(
                            DropboxActivity.this,
                            new String[]{file.getAbsolutePath()},
                            null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    ContentService.startSync(DropboxActivity.this, ContentService.SYNC_MEDIASTORE_ACTION);
                                }
                            });
                }

                @Override
                public void onNextDownload(int position) {
                }
            };
            executor.execute(new CloudDownloadManager(this, DropboxClientFactory.getClient(), fileMetadata, mOnDownloadListener));
        }
    }

    private void onBackFolder() {
        if (mStackFolder.size() > 0) {
            mStackFolder.remove(mStackFolder.size() - 1);
            mPath = (mStackFolder.size() > 0) ? mStackFolder.get(mStackFolder.size() - 1) : "";
            listFolder(mPath);
        }
    }

    @Override
    public void onBackPressed() {
        if (mStackFolder.size() == 0) {
            super.onBackPressed();
        } else {
            onBackFolder();
        }
    }

    public void updateLimiterViews() {
        LinearLayout limiterViews = (LinearLayout) findViewById(R.id.new_limiter_layout);
        if (limiterViews == null) {
            return;
        }
        limiterViews.removeAllViews();
        TextView textView1 = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
        limiterViews.addView(textView1);
        mLimiterScroller.setVisibility(View.VISIBLE);
        if (mStackFolder == null && mFilesAdapter.getCount() <= 1) {
            textView1.setText(R.string.cloud_not_find_any_song);
        } else {
            textView1.setText(R.string.root_directory);
            textView1.setTag(mFilesAdapter.TAG_DELIMITER_ROOT); // used to handle click event properly
            if (mStackFolder != null && mStackFolder.size() > 0) {
                String[] limiter = mStackFolder.get(mStackFolder.size() - 1).split("/");
                for (int i = 1; i != limiter.length; ++i) {
                    TextView textView = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
                    textView.setText(R.string.limiter_separator);
                    limiterViews.addView(textView);
                    textView = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
                    textView.setText(limiter[i]);
                    textView.setTag(i);
                    limiterViews.addView(textView);
                }
            }
            if (limiterViews.getChildCount() == 1) {
                mFilesAdapter.setIsRoot(true);
            }
        }
    }

    private void logoutDropbox() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Logout");
        dialog.show();
        clearAccessToken(new DropboxClientFactory.CallBack() {
            @Override
            public void onRevoke() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
                mPath = PATH_DEFAULT;
                hiddenPlayBack();
                refreshOptionsMenu();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.linkUnlinkDropbox) {
            logoutDropbox();
            return true;
        } else if (item.getItemId() == R.id.uploadFilesToDropbox) {
            if (!NetUtil.hasNetworkConnection(DropboxActivity.this))
                Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            else {
                popupOptions();
            }
        }
        return UiNavigationHelper.navigateUp(this, item) ? true : super.onOptionsItemSelected(item);
    }

    private void popupOptions() {
        View popupView = getLayoutInflater().inflate(R.layout.layout_popup,
                null);
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
        popupView.findViewById(R.id.ll_over).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        popupView.findViewById(R.id.btnBackup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmStartUploadToDropboxActivity();
                popupWindow.dismiss();
            }
        });
        popupView.findViewById(R.id.btnDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDownloadFiles();
                popupWindow.dismiss();
            }
        });
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(popupView, Gravity.RIGHT | Gravity.TOP, 0, 0);
    }

    private void confirmDownloadFiles() {
        final List<FileMetadata> files = filesInFolder();
        if (files.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DropboxActivity.this);
            builder.setTitle(getString(R.string.cloud_confirm_download_files));
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadFolder(files);
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            Toast.makeText(this, getResources().getString(R.string.empty_folder), Toast.LENGTH_LONG).show();
        }
    }

    private List<FileMetadata> filesInFolder() {
        List<FileMetadata> files = new ArrayList<>();
        if (null != mFilesAdapter) {
            for (int i = 0; i < mFilesAdapter.getCount() - 1; i++) {
                if (mFilesAdapter.getItem(i) instanceof FileMetadata) {
                    files.add((FileMetadata) mFilesAdapter.getItem(i));
                }
            }
        }
        return files;
    }

    private void confirmStartUploadToDropboxActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DropboxActivity.this);
        builder.setTitle(getString(R.string.cloud_confirm_go_to_upload_screen));
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String pathDropbox = mStackFolder.size() > 0 ? mStackFolder.get(mStackFolder.size() - 1) : File.separator;
                Intent intent = new Intent(DropboxActivity.this, UploadFilesToDropboxActivity.class);
                intent.putExtra(EXTRA_PATH_FOLDER, pathDropbox);
                startActivity(intent);
            }
        });
        builder.show();
    }

    private void downloadFolder(final List<FileMetadata> files) {
        if (mThreads != null && mThreads.size() > 0) {
            mThreads.clear();
        }
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                NUMBER_OF_CORES * 2,
                NUMBER_OF_CORES * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
        showProgressDialogDownload(this, files.get(0).getName());
//        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
//                    @Override
//                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
//                        executor.shutdownNow();
//                    }
//                });
//                executor.shutdownNow();
//                dialog.dismiss();
//            }
//        });

        mOnDownloadListener = new OnDownloadListener() {
            @Override
            public void onDownloadConnecting() {

            }

            @Override
            public void onDownloadProgress(final int positionFile, final long finished, final long length) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showProgressUpdateDownloadMultiFile(files.get(positionFile).getSize(), files.get(positionFile).getName(), length, positionFile, files.size());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onDownloadCompleted() {
                hideProgessDialogDownload();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "ダウンロードしました", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onDownloadPaused() {

            }

            @Override
            public void onDownloadCanceled() {

            }

            @Override
            public void onDownloadFailed() {
                if (mThreads != null && mThreads.size() > 0) {
                    mThreads.clear();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        hideProgessDialogDownload();
                        confirmDownloadError();
                    }
                });

            }

            @Override
            public void onDownloadCompletedWithFile(File file) {
                LocalBroadcastManager.getInstance(DropboxActivity.this).sendBroadcast(new Intent(ContentService.SYNC_TASK_STOPPED_ACTION));
                MediaScannerConnection.scanFile(
                        DropboxActivity.this,
                        new String[]{file.getAbsolutePath()},
                        null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                ContentService.startSync(DropboxActivity.this, ContentService.SYNC_MEDIASTORE_ACTION);
                            }
                        });
            }

            @Override
            public void onNextDownload(int position) {
                executor.execute(mThreads.get(position));
            }
        };
        for (int i = 0; i < files.size(); i++) {
            mThreads.add(new CloudDownloadManager(this, DropboxClientFactory.getClient(), files, i, mOnDownloadListener));
        }
        if (null != mThreads && mThreads.size() > 0) {
            executor.execute(mThreads.get(0));
        }
    }

    @Override
    protected void clearAccessToken(DropboxClientFactory.CallBack callback) {
        super.clearAccessToken(callback);
        mFilesAdapter.notifyDataSetChanged();
        mStackFolder.clear();
        clearLimiterViews();

    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_dropbox, menu);
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!hasToken()) {
            menu.removeItem(R.id.uploadFilesToDropbox);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(getString(R.string.cloud_dropbox));
        refreshOptionsMenu();

        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void loadData() {
        new GetCurrentAccountTask(DropboxClientFactory.getClient(), new GetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result) {
                listFolder(mPath);
            }

            @Override
            public void onError(Exception e) {
                Log.e(getClass().getName(), "Failed to get account details.", e);
            }
        }).execute();
    }

    private void linkOrUnlink() {
        if (DropboxClientFactory.getClient() != null) {
            listFolder(mPath);
            return;
        }
    }

    private void listFolder(String path) {
        mPath = path;
        if (!NetUtil.hasNetworkConnection(DropboxActivity.this)) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage(getResources().getString(R.string.loading));
        dialog.show();

        new ListFolderTask(DropboxClientFactory.getClient(), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                mFilesAdapter.setFiles(result);
                updateLimiterViews();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(DropboxActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        }).execute(path);
    }

    private void clearLimiterViews() {
        LinearLayout limiterViews = (LinearLayout) findViewById(R.id.new_limiter_layout);
        if (limiterViews == null) {
            return;
        }
        limiterViews.removeAllViews();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = mFilesAdapter.createData(view);
        if (intent.getBooleanExtra(LibraryAdapter.DATA_LINK_WITH_DROPBOX, false)) {
            Auth.startOAuth2Authentication(DropboxActivity.this, "xh56yah2w4202fs");
            linkOrUnlink();
            return;
        }
    }

    private void confirmDownloadError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DropboxActivity.this);
        builder.setMessage(getString(R.string.cloud_download_error));

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private class DropboxPrepareMetadata
            extends AsyncTask<Void, Integer, CloudSongMetadata> {
        private final String mPath;
        private String mToastMessage;

        public DropboxPrepareMetadata(String path) {
            mPath = path;
        }

        @Override
        protected void onPreExecute() {
            showProgessDialog(getString(R.string.preparing_cloud_songs));
        }

        @Override
        protected CloudSongMetadata doInBackground(Void... v) {
            try {
                GetTemporaryLinkResult theFile = null;
                try {
                    theFile = DropboxClientFactory.getClient().files().getTemporaryLink(mPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (theFile == null) {
                    mToastMessage = getResources().getString(R.string.cloud_network_error);
                    return null;
                }

                String unknown = getResources().getString(R.string.unknown);
                CloudSongMetadata currentSongMeta = new CloudSongMetadata(null, theFile.getLink(), theFile.getMetadata().getRev(),
                        0, 0, theFile.getMetadata().getName(), unknown, unknown, 0, 1);

					/*
                     * download a part of the file to read tags (title, artist, etc.) and ReplayGain info
					 */
                URL url = null;
                url = new URL(theFile.getLink());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

                byte[] id3v2Header = new byte[10];
                in.read(id3v2Header, 0, 10);

                if (id3v2Header[0] == 0x49 && id3v2Header[1] == 0x44 &&
                        id3v2Header[2] == 0x33) { // the song actually has an id3v2 tag

                    // a signed int is fine, as a valid ID3 tag will never be as large as to
                    // require full 32 bits to represent its size
                    int id3v2TagSize = id3v2Header[9] + id3v2Header[8] * 128 + id3v2Header[7] * 16384 +
                            id3v2Header[6] * 2097152;

                    if ((id3v2Header[5] & 64) != 0) { // extended header is present
                        in.skip(1337); // TODO: use actual size
                    }

                    // now read tag frames
                    long grandTotal = 0; // total bytes read or skipped

                    while (grandTotal < id3v2TagSize - 10) { // if 10 bytes or less remain, they can only be padding

                        byte[] frameHeader = new byte[10];
                        grandTotal += in.read(frameHeader, 0, 10);

                        if (frameHeader[0] == 0x0) {
                            break; // we've reached padding
                        }

                        String frameId = new String(frameHeader, 0, 4, "US-ASCII");

                        // a signed int will hold the result no problem due to the entire
                        // tag's total size limit
                        int frameSize = (0xFFFFFFFF & frameHeader[7]) |
                                ((0xFFFFFFFF & frameHeader[6]) << 8) |
                                ((0xFFFFFFFF & frameHeader[5]) << 16) |
                                ((0xFFFFFFFF & frameHeader[4]) << 24);

                        if (Arrays.asList(LibraryAdapter.TAG_IDS_OF_INTEREST)
                                .contains(frameId)) { // read the actual frame

                            // "TXXX", "TIT2", "TRCK", "TPE1", "TALB"

                            byte[] frameData = new byte[frameSize];
                            grandTotal += in.read(frameData, 0, frameSize);

                            if (frameId.equals("TXXX") &&
                                    frameData[0] == 0) { // looks like Replay Gain data
                                String str = new String(frameData, "ISO-8859-1").toLowerCase();
                                int index = str.indexOf("replaygain_track_gain");
                                if (index != -1) {
                                    int index2 = str.indexOf(" ", index + 1);
                                    str = str.substring(
                                            index + "replaygain_track_gain".length() + 1,
                                            index2);
                                    currentSongMeta.rgTrack = Float.parseFloat(str);
                                } else if ((index = str.indexOf("replaygain_album_gain")) != -1) {
                                    int index2 = str.indexOf(" ", index + 1);
                                    str = str.substring(
                                            index + "replaygain_album_gain".length() + 1,
                                            index2);
                                    currentSongMeta.rgAlbum = Float.parseFloat(str);
                                }
                            } else if (frameId.equals("TIT2")) {
                                // TODO: handle all text encodings defined in ID3v2.3 and 2.4
                                // properly
                                String title = new String(frameData, 1, frameData.length - 1, "UTF-16BE");
                            } else if (frameId.equals("TRCK")) {
                                String str = new String(frameData, 1, frameData.length - 1, "UTF-8");
                                int index;
                                if ((index = str.indexOf("/")) != -1) {
                                    str = str.substring(0, index);
                                }
                                try {
                                    currentSongMeta.trackNumber = Integer.parseInt(str.trim());
                                } catch (NumberFormatException e) {
                                    currentSongMeta.trackNumber = 1;
                                }
                            } else if (frameId.equals("TPE1")) {

                                if (isValidUTF8(frameData)) {
                                    currentSongMeta.artist = new String(frameData, 1, frameData.length - 1, "UTF-8");
                                } else {
                                }
                            } else if (frameId.equals("TALB")) {
                                currentSongMeta.album = new String(frameData, 1, frameData.length - 1, "UTF-8");
                            }
                        } else {
                            grandTotal += in.skip(frameSize);
                        }

                    }
                }

                in.close();
                connection.disconnect();

                return currentSongMeta;

            } catch (Exception e) {
                Log.i("", "E: " + e.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showErrorPlayMusic(getString(R.string.cloud_communicate_error_title), getString(R.string.cloud_communicate_error_content));
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(CloudSongMetadata result) {

            if (result == null) {
                if (null != mDialog && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            } else {
                setTitleOfSong(result);
                mCurAudioFile = result.path;
                playMusicWithUrl(mCurAudioFile, DropboxActivity.this, mMediaPlayer, focusChangeListener, mDialog);
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.stop();
                        mp.release();
                        mMediaPlayer = new MediaPlayer();
                        hiddenPlayBack();
                        hiddenPlay();
                    }
                });
            }
        }
    }

    public static boolean isValidUTF8(byte[] input) {
        CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();
        try {
            cs.decode(ByteBuffer.wrap(input));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    public void play(View view) {
        musicActive();
        if (null != mMediaPlayer) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                hiddenPlay();
            } else {
                mMediaPlayer.start();
                hiddenPause();
            }
        }
    }

    private void musicActive() {
        AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int result = manager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // other app had stopped playing song now , so u can do u stuff now .
        }
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onDestroy();

    }

    private void hideProgessDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void showProgessDialog(String dialogContent) {
        hideProgessDialog();
        if (mDialog == null) {
            mDialog = new ProgressDialog(DropboxActivity.this);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(true);
            mDialog.getWindow().setGravity(Gravity.CENTER);
        }
        mDialog.setMessage(dialogContent);
        mDialog.show();
    }

    private void setTitleOfSong(CloudSongMetadata result) {
        if (null != result) {
            TextView artist = (TextView) findViewById(R.id.tvArtist);
            TextView title = (TextView) findViewById(R.id.tvTitle);
            artist.setText((null == result.artist) ? "" : result.artist);
            title.setText((null == result.title) ? "" : result.title);
        }
    }

    private void showErrorPlayMusic(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DropboxActivity.this);
        builder.setTitle(title);
        builder.setMessage(content);

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
