package com.iseasoft.iSeaMusic.cloud.interfaces;


public interface OnDownloadListener {
    void onDownloadConnecting();

    void onDownloadProgress(int positionFile, long finished, long length);

    void onDownloadCompleted();

    void onDownloadCompletedWithFile(java.io.File file);

    void onDownloadPaused();

    void onDownloadCanceled();

    void onDownloadFailed();

    void onNextDownload(int position);
}
