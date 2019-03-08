package com.iseasoft.iSeaMusic.cloud.drive;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.tsutaya.musicplayer.cloud.interfaces.OnDownloadListener;
import com.tsutaya.musicplayer.cloud.utils.Download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jp.co.mytrax.traxcore.storage.StorageUtils;
import jp.co.mytrax.traxcore.sync.ContentService;

public class GDriveManager {
    private Context mContext;
    private OnDownloadListener mDownloadListener = null;
    private int threadNo;
    private Drive mService;
    private int mSize = 0;
    private long totalBytes;
    private Download mTypeDownload;

    public GDriveManager(Context context, Drive service, int threadNo, Download type, int size, OnDownloadListener downloadListener) {
        this.mContext = context;
        this.mDownloadListener = downloadListener;
        this.threadNo = threadNo;
        this.mTypeDownload = type;
        this.mService = service;
        this.mSize = size;
    }

    public boolean downloadFolder(File gFile) {
        if (StorageUtils.mediaFileExists(StorageUtils.getMusicDirectory() + "/" + gFile.getOriginalFilename())) {
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
            File file = mService.files().get(gFile.getId()).execute();
            java.io.File toFile = new java.io.File(StorageUtils.getMusicDirectory(), file.getOriginalFilename());
            toFile.createNewFile();
            return downloadFile(file, toFile, mService);
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mDownloadListener) {
                mDownloadListener.onDownloadFailed();
            }
        }
        return true;
    }

    public boolean downloadFile(File gFile) {
        try {
            File file = mService.files().get(gFile.getId()).execute();
            java.io.File toFile = new java.io.File(StorageUtils.getMusicDirectory(), file.getOriginalFilename());
            toFile.createNewFile();
            return downloadFile(file, toFile, mService);
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mDownloadListener) {
                mDownloadListener.onDownloadFailed();
            }
        }
        return true;
    }

    public boolean downloadFile(File file, java.io.File toFile, Drive service) {
        HttpResponse respEntity = null;
        try {
            // URL url = new URL(urlString);
            respEntity = service.getRequestFactory()
                    .buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
            InputStream in = respEntity.getContent();
            if (totalBytes == 0) {
                totalBytes = respEntity.getContentLoggingLimit();
            }
            try {
                FileOutputStream f = new FileOutputStream(toFile) {
                    @Override
                    public void write(byte[] buffer, int byteOffset, int byteCount) throws IOException {
                        // TODO Auto-generated method stub
                        super.write(buffer, byteOffset, byteCount);
                    }
                };
                byte[] buffer = new byte[1024];
                int len1 = 0;
                long bytesRead = 0;
                while ((len1 = in.read(buffer)) > 0) {
                    f.write(buffer, 0, len1);
                    bytesRead += len1;
                    mDownloadListener.onDownloadProgress(threadNo, 1000, bytesRead);
                }
                f.close();
            } catch (Exception e) {
                if (null != mDownloadListener) {
                    mDownloadListener.onDownloadFailed();
                }
                return false;
            }
            if (null != mDownloadListener) {
                switch (mTypeDownload) {
                    case MULTI:
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ContentService.SYNC_TASK_STOPPED_ACTION));
                        MediaScannerConnection.scanFile(
                                mContext,
                                new String[]{toFile.getAbsolutePath()},
                                null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        ContentService.startSync(mContext, ContentService.SYNC_MEDIASTORE_ACTION);
                                    }
                                });
                        mDownloadListener.onDownloadCompletedWithFile(toFile);
                        threadNo++;
                        if (threadNo < mSize) {
                            mDownloadListener.onNextDownload(threadNo);
                        } else {
                            mDownloadListener.onDownloadCompleted();
                        }
                        break;
                    case SINGLE:
                        mDownloadListener.onDownloadCompletedWithFile(toFile);
                        mDownloadListener.onDownloadCompleted();
                        break;
                }
            }
            return true;
        } catch (IOException ex) {
            if (null != mDownloadListener) {
                mDownloadListener.onDownloadFailed();
                return false;
            }

        } finally {
            if (respEntity != null) {
                try {
                    respEntity.disconnect();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
