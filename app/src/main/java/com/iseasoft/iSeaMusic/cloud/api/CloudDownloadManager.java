package com.iseasoft.iSeaMusic.cloud.api;


import android.content.Context;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.tsutaya.musicplayer.cloud.drive.GDriveManager;
import com.tsutaya.musicplayer.cloud.dropbox.DropboxManager;
import com.tsutaya.musicplayer.cloud.interfaces.OnDownloadListener;
import com.tsutaya.musicplayer.cloud.utils.Cloud;
import com.tsutaya.musicplayer.cloud.utils.Download;

import java.util.List;

public class CloudDownloadManager implements Runnable {
    private int threadNo;
    private List<File> mFilesDrive;
    private List<FileMetadata> mFilesDropBox;
    private GDriveManager mGDriveManager;
    private DropboxManager mDropboxManager;
    private Cloud mType;
    private File mFile;
    private FileMetadata mFileMetadata;
    private Download mTypeDownload;

    public CloudDownloadManager(Context context, Drive service, List<File> files, int threadNo, OnDownloadListener downloadListener) {
        this.mType = Cloud.GDRIVE;
        this.threadNo = threadNo;
        this.mFilesDrive = files;
        mGDriveManager = new GDriveManager(context, service, threadNo, Download.MULTI, files.size(), downloadListener);
        mTypeDownload = Download.MULTI;
    }

    public CloudDownloadManager(Context context, Drive service, File file, int threadNo, OnDownloadListener downloadListener) {
        this.mType = Cloud.GDRIVE;
        this.threadNo = threadNo;
        this.mFile = file;
        mGDriveManager = new GDriveManager(context, service, threadNo, Download.SINGLE, 0,  downloadListener);
        mTypeDownload = Download.SINGLE;
    }

    public CloudDownloadManager(Context context, DbxClientV2 dbxClient, FileMetadata metadata, OnDownloadListener downloadListener) {
        this.mType = Cloud.DROPBOX;
        this.mFileMetadata = metadata;
        mDropboxManager = new DropboxManager(context, dbxClient,  downloadListener);
        mTypeDownload = Download.SINGLE;
    }

    public CloudDownloadManager(Context context, DbxClientV2 dbxClient, List<FileMetadata> files, int threadNo, OnDownloadListener downloadListener) {
        this.mType = Cloud.DROPBOX;
        this.threadNo = threadNo;
        this.mFilesDropBox = files;
        mDropboxManager = new DropboxManager(context, dbxClient, threadNo, files.size(), downloadListener);
        mTypeDownload = Download.MULTI;
    }


    @Override
    public void run() {
        switch (mType) {
            case GDRIVE:
                if (mTypeDownload == Download.MULTI && threadNo < mFilesDrive.size()) {
                    mGDriveManager.downloadFolder(mFilesDrive.get(threadNo));
                } else if (mTypeDownload == Download.SINGLE) {
                    mGDriveManager.downloadFile(mFile);
                }
                break;
            case DROPBOX:
                if (mTypeDownload == Download.MULTI && threadNo < mFilesDropBox.size()) {
                    mDropboxManager.downloadFileInFolderDropbox(mFilesDropBox.get(threadNo));
                } else if (mTypeDownload == Download.SINGLE) {
                    mDropboxManager.downloadFileFromDropbox(mFileMetadata);
                }
                break;
        }
    }

}