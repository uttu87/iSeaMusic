package com.iseasoft.iSeaMusic.cloud.interfaces;

import java.io.File;


public interface IDownloadCallback {
    public void onDownloadProgress();

    public void onFinishDownload(File file);

    public void onErrorDownload(Exception e);
}
