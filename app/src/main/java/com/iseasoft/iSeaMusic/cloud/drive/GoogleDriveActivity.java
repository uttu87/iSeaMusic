package com.iseasoft.iSeaMusic.cloud.drive;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.iseasoft.iSeaMusic.cloud.CloudBaseActivity;
import com.iseasoft.iSeaMusic.cloud.api.CloudDownloadManager;
import com.iseasoft.iSeaMusic.cloud.interfaces.IDownloadCallback;
import com.iseasoft.iSeaMusic.cloud.interfaces.OnDownloadListener;
import com.iseasoft.iSeaMusic.cloud.interfaces.ResultAdapterCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressLint("NewApi")
public class GoogleDriveActivity extends CloudBaseActivity {
    private static final String TAG = "GoogleDriveActivity";
    public static final String BUNDLE_PARENTID_KEY = "BUNDLE_PARENTID_KEY";
    public static final String EXTRA_PARENTID_KEY = "EXTRA_PARENTID_KEY";
    private static final String FILTER_TYPES = " and trashed = false and (mimeType='application/vnd.google-apps.folder' or mimeType contains 'audio/')";
    private static final String FOLDER_TYPE = "application/vnd.google-apps.folder";
    private ResultsAdapter mResultAdapter;
    private Context mContext;
    private ListView mListViewFiles; // List view that displays the query results.
    GoogleAccountCredential mCredential;
    ImageView mDisk;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_RELOAD_DATA = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static Drive mService;
    private List<File> mResultList;
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private HorizontalScrollView mLimiterScroller;
    private ProgressDialog mDialog;
    private String mAccessToken;
    ArrayList<HashMap<String, String>> mFolderPath = new ArrayList<HashMap<String, String>>();
    private Thread mThread;
    private String mParentId = "root";
    private ResultAdapterCallback mCallback;
    private IDownloadCallback mIDownloadCallback;
    private ArrayList<CloudDownloadManager> mThreads = new ArrayList<>();
    private OnDownloadListener mOnDownloadListener;

    /**
     * Id to identify a storage permission request.
     */
    private static final int REQUEST_ACCOUNT_PERMISSION = 101;

    /**
     * Permissions required to read and write storage.
     */
    private static String[] PERMISSIONS_ACCOUNT = {Manifest.permission.GET_ACCOUNTS};
    private RelativeLayout rlAccountPermissionContainer;
    private TextView tvAccountPermissionDescription;
    private Button btnAccountPermissionSettings;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == REQUEST_ACCOUNT_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!isHasError()) {
                    chooseAccount();
                }
                onCreateListennerDriver();
            } else {
                TransitionManager.beginDelayedTransition(rlAccountPermissionContainer,new Slide(Gravity.BOTTOM));
                rlAccountPermissionContainer.setVisibility(View.VISIBLE);
                tvAccountPermissionDescription.setText(getText(R.string.mediamonkey) + " "
                        + getText(R.string.account_permission_description) + " "
                        + getText(R.string.cloud_google_drive));
                btnAccountPermissionSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        GoogleDriveActivity.this.startActivityForResult(intent, REQUEST_ACCOUNT_PERMISSION);
                    }
                });
            }
        }
    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_google_drive, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);

        if (accountName == null) {
            menu.removeItem(R.id.uploadFilesToDropbox);
        }

        return super.onPrepareOptionsMenu(menu);
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
                confirmStartUploadToGoogledrive();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedId = item.getItemId();
        if (selectedId == R.id.linkUnlinkGoogleDrive) {
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_ACCOUNT_NAME, null);
            editor.apply();
            onBackPressed();
        } else if (selectedId == R.id.addFileIntoGoogleDrive) {
            // start UploadGoogleDriveActivity to upload local file to
            if (!NetUtil.hasNetworkConnection(GoogleDriveActivity.this))
                Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            else {
                popupOptions();
            }
        } else { // at the current, this case only apply for Back button.
            onBackPressed();
        }
        return true;
    }

    private void confirmDownloadFiles() {
        final List<File> files = filesInFolder();
        if (files.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(GoogleDriveActivity.this);
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

    private void confirmStartUploadToGoogledrive() {

        AlertDialog.Builder builder = new AlertDialog.Builder(GoogleDriveActivity.this);
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
                UploadGoogleDriveActivity.mService = mService;
                Intent intent = new Intent(GoogleDriveActivity.this, UploadGoogleDriveActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_PARENTID_KEY, mParentId);
                intent.putExtra(EXTRA_PARENTID_KEY, bundle);
                startActivityForResult(intent, REQUEST_RELOAD_DATA);
            }
        });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive);
        mListViewFiles = (ListView) findViewById(R.id.actual_list_view);
        mLimiterScroller = (HorizontalScrollView) findViewById(R.id.new_limiter_scroller);
        mDisk = (ImageView) findViewById(R.id.imgDisk);
        rlAccountPermissionContainer = findViewById(R.id.account_permission_container);
        tvAccountPermissionDescription = findViewById(R.id.txt_account_permission_description);
        btnAccountPermissionSettings = findViewById(R.id.btn_account_permission_settings);
        mContext = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(GoogleDriveActivity.this,
                    Manifest.permission.GET_ACCOUNTS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_ACCOUNT, REQUEST_ACCOUNT_PERMISSION);
            } else {
                if (!isHasError()) {
                    chooseAccount();
                }
                onCreateListennerDriver();
            }
        } else {
            if (!isHasError()) {
                chooseAccount();
            }
            onCreateListennerDriver();
        }


    }

    private void onCreateListennerDriver() {
        mIDownloadCallback = new IDownloadCallback() {

            @Override
            public void onFinishDownload(final java.io.File file) {
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ContentService.SYNC_TASK_STOPPED_ACTION));
                MediaScannerConnection.scanFile(
                        mContext,
                        new String[]{file.getAbsolutePath()},
                        null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                ContentService.startSync(mContext, ContentService.SYNC_MEDIASTORE_ACTION);
                            }
                        });
            }

            @Override
            public void onErrorDownload(Exception e) {
                // TODO Auto-generated method stub
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        confirmDownloadError();
                    }
                });

            }

            @Override
            public void onDownloadProgress() {
                // TODO Auto-generated method stub

            }
        };

        mCallback = new ResultAdapterCallback() {

            @Override
            public void onFolderClicked(int position, FolderMetadata folder) {

            }

            @Override
            public void onFileClicked(int position, FileMetadata file) {

            }

            @Override
            public void onRootClicked() {

            }

            @Override
            public void onDownloadFileCallback(Object object) {
                downloadSingleFile((File) object);
            }
        };
    }

    private void downloadSingleFile(final File file) {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();


        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                NUMBER_OF_CORES * 2,
                NUMBER_OF_CORES * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
        showProgressDialogDownload(this, file.getTitle());
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
            public void onDownloadProgress(final int positionFile, long finished, final long length) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showProgressUpdateDownloadSingleFile(file.getFileSize(), file.getTitle(), length);
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
            public void onDownloadCompletedWithFile(java.io.File file) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }

            @Override
            public void onNextDownload(int position) {
            }
        };
        executor.execute(new CloudDownloadManager(this, mService, file, 0, mOnDownloadListener));
    }

    private void confirmDownloadError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GoogleDriveActivity.this);
        builder.setMessage(getString(R.string.cloud_download_error));

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public GoogleDriveActivity() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(getString(R.string.cloud_google_drive));
    }

    @Override
    protected void loadData() {

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_RELOAD_DATA:
                if (!NetUtil.hasNetworkConnection(GoogleDriveActivity.this)) {
                    Toast.makeText(GoogleDriveActivity.this, getResources().getString(R.string.wrong_internet), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgessDialog(getResources().getString(R.string.loading));
                if (mParentId == "root")
                    getRootDriveContents();
                else
                    getFolderDriveContentsPassID(mParentId);
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.i(TAG, "accountName IN onActivityResult: " + accountName);
                    if (accountName != null) {
                        showProgessDialog(getResources().getString(R.string.loading));
                        // save account name
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();

                        mCredential.setSelectedAccountName(accountName);
                        mService = getDriveService(mCredential);
                        getAccessToken();
                        getRootDriveContents();
                    }
                } else {
                    onBackPressed();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    // account already picked
                    Log.i(TAG, "account already picked");
                } else {
                    startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;
            case REQUEST_ACCOUNT_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(GoogleDriveActivity.this,
                            Manifest.permission.GET_ACCOUNTS)
                            == PackageManager.PERMISSION_GRANTED) {
                        rlAccountPermissionContainer.setVisibility(View.GONE);
                        if (!isHasError()) {
                            chooseAccount();
                        }
                        onCreateListennerDriver();
                    }
                }
                break;
        }
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google Play Services installation via a user dialog, if possible.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Display an error dialog showing that Google Play Services is missing or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of) Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {

        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                GoogleDriveActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void chooseAccount() {
        String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            showProgessDialog(getResources().getString(R.string.loading));
            mCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE))
                    .setBackOff(new ExponentialBackOff());
            mCredential.setSelectedAccountName(accountName);
            mService = getDriveService(mCredential);
            getAccessToken();
            getRootDriveContents();
        } else {
            connectByOAuth2();
        }
    }

    private boolean isHasError() {
        if (!isGooglePlayServicesAvailable()) {
            hideProgessDialog();
            Toast.makeText(mContext, R.string.cloud_network_error, Toast.LENGTH_SHORT).show();
            return true;
        } else if (!isDeviceOnline()) {
            hideProgessDialog();
            Toast.makeText(mContext, R.string.cloud_network_error, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;

    }

    private void getAccessToken() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    token = GoogleAuthUtil.getToken(
                            GoogleDriveActivity.this,
                            mCredential.getSelectedAccount(),
                            mCredential.getSelectedAccountName());
                } catch (IOException transientEx) {
                    // Network or server error, try later
                    Log.e(TAG, transientEx.toString());
                } catch (UserRecoverableAuthException e) {
                    Log.e(TAG, e.toString());
                } catch (GoogleAuthException authEx) {
                    Log.e(TAG, authEx.toString());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    getPreferences(Context.MODE_PRIVATE).edit().putString(PREF_ACCOUNT_NAME, null).apply();
                }
                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                Log.i(TAG, "Access token retrieved:" + token);
                mAccessToken = token;
            }
        };
        task.execute();
    }

    private void connectByOAuth2() {
        mCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE)).setBackOff(new ExponentialBackOff());
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }

    private void getRootDriveContents() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mResultList = new ArrayList<File>();
                com.google.api.services.drive.Drive.Files f1 = mService.files();
                com.google.api.services.drive.Drive.Files.List request = null;

                try {
                    request = f1.list();
                    request.setQ("'root' in parents" + FILTER_TYPES).setOrderBy("folder");
                    com.google.api.services.drive.model.FileList fileList = request.execute();
                    mResultList.addAll(fileList.getItems());
                    request.setPageToken(fileList.getNextPageToken());
                    mParentId = "root";
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (request != null) {
                        request.setPageToken(null);
                    }
                } catch (IllegalArgumentException e) {
                    getPreferences(Context.MODE_PRIVATE).edit().putString(PREF_ACCOUNT_NAME, null).apply();
                    connectByOAuth2();
                }
                populateListView(true);
            }
        });
        mThread.start();
    }

    private void getFolderDriveContentsPassID(final String folderID) {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mResultList = new ArrayList<File>();
                com.google.api.services.drive.Drive.Files f1 = mService.files();
                com.google.api.services.drive.Drive.Files.List request = null;

                try {
                    request = f1.list();
                    request.setQ("'" + folderID + "' in parents" + FILTER_TYPES).setOrderBy("folder");
                    request.setSpaces("drive");
                    com.google.api.services.drive.model.FileList fileList = request.execute();
                    mResultList.clear();
                    File file = new File();
                    file.setId(folderID);
                    mResultList.add(file);
                    mResultList.addAll(fileList.getItems());
                    request.setPageToken(fileList.getNextPageToken());
                    mParentId = folderID;
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (request != null) {
                        request.setPageToken(null);
                    }
                }
                populateListView(false);
            }
        });
        mThread.start();
    }

    private List<File> filesInFolder() {
        List<File> files = new ArrayList<>();
        if (null != mResultList) {
            for (int i = 1; i < mResultList.size(); i++) {
                if (mResultList.get(i).getMimeType() != null && mResultList.get(i).getMimeType().equals(FOLDER_TYPE)) {
                    Log.i(TAG, "Folder");
                } else {
                    files.add(mResultList.get(i));
                }
            }
        }
        return files;
    }

    private void downloadFolder(final List<File> files) {
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
        showProgressDialogDownload(GoogleDriveActivity.this, files.get(0).getTitle());
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
                            showProgressUpdateDownloadMultiFile(files.get(positionFile).getFileSize(), files.get(positionFile).getTitle(), length, positionFile, files.size());
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
            public void onDownloadCompletedWithFile(java.io.File file) {
            }

            @Override
            public void onNextDownload(int position) {
                executor.execute(mThreads.get(position));
            }
        };
        for (int i = 0; i < files.size(); i++) {
            mThreads.add(new CloudDownloadManager(this, mService, files, i, mOnDownloadListener));
        }
        if (null != mThreads && mThreads.size() > 0) {
            executor.execute(mThreads.get(0));
        }
    }

    private void populateListView(final boolean isRoot) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResultAdapter = new ResultsAdapter(mContext, mCallback);
                mResultAdapter.setResultDate((List<Object>) (Object) mResultList);
                mListViewFiles.setAdapter(mResultAdapter);
                mResultAdapter.setIsRoot(isRoot);
                hideProgessDialog();
                updateLimiterViews(isRoot);
                mListViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        if (!NetUtil.hasNetworkConnection(GoogleDriveActivity.this)) {
//							Toast.makeText(GoogleDriveActivity.this, getResources().getString(R.string.cloud_network_error), Toast.LENGTH_SHORT).show();
                            showErrorPlayMusic(getString(R.string.cloud_communicate_error_title), getString(R.string.cloud_communicate_error_content));
                            return;
                        }
                        if (mResultList == null && mResultList.size() < position)
                            return;
                        if (position == 0 && !isRoot) {
                            showProgessDialog(getResources().getString(R.string.loading));
                            if (mFolderPath.size() > 1) {
                                String folderID = mFolderPath.get(mFolderPath.size() - 2).get("folderID");
                                mFolderPath.remove(mFolderPath.size() - 1);
                                mFolderPath.remove(mFolderPath.size() - 1);
                                getFolderDriveContentsPassID(folderID);
                            } else if (mFolderPath.size() == 1) {
                                mFolderPath.remove(mFolderPath.size() - 1);
                                getRootDriveContents();
                            } else {
                                getRootDriveContents();
                            }

                        } else {
                            // it's folder.
                            if (mResultList.get(position).getMimeType() != null && mResultList.get(position).getMimeType().equals(FOLDER_TYPE)) {
                                showProgessDialog(getResources().getString(R.string.loading));
                                HashMap<String, String> currentFolderInfo = new HashMap<String, String>();
                                currentFolderInfo.put("folderID", mResultList.get(position).getId());
                                currentFolderInfo.put("folderTitle", mResultList.get(position).getTitle());
                                mFolderPath.add(currentFolderInfo);
                                getFolderDriveContentsPassID(mResultList.get(position).getId());
                            } else { // it's file
                                showProgessDialog(getResources().getString(R.string.loading));
                                setTitleOfSong(mResultList.get(position));
                                playMusicWithUrl(mResultList.get(position).getDownloadUrl() + "&access_token=" + mAccessToken, GoogleDriveActivity.this, mMediaPlayer, focusChangeListener, mDialog);
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
                });
            }
        });
    }

    private void showErrorPlayMusic(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GoogleDriveActivity.this);
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

    private void closeDialogProgressWhenError() {
        hideProgessDialog();
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        super.onDestroy();
    }

    private void setTitleOfSong(File result) {
        if (null != result && result.size() > 0) {
            TextView artist = (TextView) findViewById(R.id.tvArtist);
            artist.setVisibility(View.GONE);
            TextView title = (TextView) findViewById(R.id.tvTitle);
            title.setText((null == result.getTitle()) ? "" : result.getTitle());
        }
    }

    private void showProgessDialog(String dialogContent) {
        hideProgessDialog();
        if (mDialog == null) {
            mDialog = new ProgressDialog(GoogleDriveActivity.this);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(true);
            mDialog.getWindow().setGravity(Gravity.CENTER);
            mDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    if (mThread != null) {
                        Log.i(TAG, "onCancel mThread");
                        mThread.interrupt();
                    }
                }
            });
        }

        mDialog.setMessage(dialogContent);
        mDialog.show();
    }

    private void hideProgessDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @SuppressLint("InflateParams")
    public void updateLimiterViews(boolean isRoot) {
        LinearLayout limiterViews = (LinearLayout) findViewById(R.id.new_limiter_layout);
        if (limiterViews == null) {
            return;
        }
        limiterViews.removeAllViews();
        TextView textView1 = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
        limiterViews.addView(textView1);
        mLimiterScroller.setVisibility(View.VISIBLE);
        if (isRoot && mResultList.size() == 0) {
            textView1.setText(R.string.cloud_not_find_any_song);
        } else {
            textView1.setText(R.string.root_directory);
            textView1.setTag(LibraryAdapter.TAG_DELIMITER_ROOT); // used to handle click event properly
            if (mFolderPath != null) {
                for (int i = 0; i < mFolderPath.size(); i++) {
                    TextView textView = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
                    textView.setText(R.string.limiter_separator);
                    limiterViews.addView(textView);
                    textView = (TextView) getLayoutInflater().inflate(R.layout.limiter_text_view, null);
                    textView.setText(mFolderPath.get(i).get("folderTitle"));
                    textView.setTag(i);
                    limiterViews.addView(textView);
                }
            }
        }
    }

    public void play(View view) {
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
        manager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }
}
