package com.iseasoft.iSeaMusic.cloud.dropbox;


import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.tsutaya.musicplayer.cloud.interfaces.OnDownloadListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import jp.co.mytrax.traxcore.storage.StorageUtils;
import jp.co.mytrax.traxcore.sync.ContentService;

public class DropboxManager {
    private Context mContext;
    private OnDownloadListener mDownloadListener = null;
    private int threadNo;
    private final DbxClientV2 mDbxClient;
    private int mSize = 0;

    public DropboxManager(Context context, DbxClientV2 dbxClient, int threadNo, int size, OnDownloadListener downloadListener) {
        this.mContext = context;
        this.mDownloadListener = downloadListener;
        this.threadNo = threadNo;
        this.mDbxClient = dbxClient;
        this.mSize = size;
    }

    public DropboxManager(Context context, DbxClientV2 dbxClient, OnDownloadListener downloadListener) {
        this.mContext = context;
        this.mDownloadListener = downloadListener;
        this.mDbxClient = dbxClient;
    }

    public boolean downloadFileInFolderDropbox(FileMetadata metadata) {
        if (StorageUtils.mediaFileExists(StorageUtils.getMusicDirectory() + "/" + metadata.getName())) {
            if (null != mDownloadListener) {
                mDownloadListener.onDownloadProgress(threadNo, 1000, 1000);
            }
            threadNo++;
            if (threadNo < mSize) {
                mDownloadListener.onNextDownload(threadNo);
            } else {
                mDownloadListener.onDownloadCompleted();
            }
            return true;
        }
        try {
            java.io.File file = new java.io.File(StorageUtils.getMusicDirectory(), metadata.getName());
            DbxDownloader<FileMetadata> dl = mDbxClient.files().download(metadata.getPathLower());
            long size = dl.getResult().getSize();

            FileOutputStream fout = new FileOutputStream(file);
            dl.download(new ProgressOutputStream(size, fout, new OnDownloadListener() {
                @Override
                public void onDownloadConnecting() {

                }

                @Override
                public void onDownloadProgress(int positionFile, long finished, long length) {
                    if (mDownloadListener != null) {
                        mDownloadListener.onDownloadProgress(threadNo, finished, length);
                    }
                }

                @Override
                public void onDownloadCompleted() {

                }

                @Override
                public void onDownloadCompletedWithFile(java.io.File file) {
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
                public void onDownloadPaused() {

                }

                @Override
                public void onDownloadCanceled() {

                }

                @Override
                public void onDownloadFailed() {

                }

                @Override
                public void onNextDownload(int position) {

                }
            }));
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadCompletedWithFile(file);
                threadNo++;
                if (threadNo < mSize) {
                    mDownloadListener.onNextDownload(threadNo);
                } else {
                    mDownloadListener.onDownloadCompleted();
                }
            }
            return true;
        } catch (DbxException | IOException e) {
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadFailed();
            }
        }
        return true;
    }

    public void downloadFileFromDropbox(FileMetadata metadata) {
        java.io.File file = new java.io.File(StorageUtils.getMusicDirectory(), metadata.getName());
        DbxDownloader<FileMetadata> dl = null;
        try {
            dl = mDbxClient.files().download(metadata.getPathLower());
            long size = dl.getResult().getSize();
            FileOutputStream fout = new FileOutputStream(file);
            dl.download(new ProgressOutputStream(size, fout, mDownloadListener));
            if (mDownloadListener != null) {
                mDownloadListener.onDownloadCompletedWithFile(file);
                mDownloadListener.onDownloadCompleted();
            }
        } catch (DbxException e) {
            e.printStackTrace();
            mDownloadListener.onDownloadFailed();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mDownloadListener.onDownloadFailed();
        } catch (IOException e) {
            e.printStackTrace();
            mDownloadListener.onDownloadFailed();
        }
    }
}
