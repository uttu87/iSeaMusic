package com.iseasoft.iSeaMusic.cloud.dropbox;


import com.tsutaya.musicplayer.cloud.interfaces.OnDownloadListener;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {
    private OutputStream underlying;
    private OnDownloadListener mDownloadListener = null;
    private int completed;
    private long totalSize;

    public ProgressOutputStream(long totalSize, OutputStream underlying, OnDownloadListener downloadListener) {
        this.underlying = underlying;
        this.mDownloadListener = downloadListener;
        this.completed = 0;
        this.totalSize = totalSize;
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        this.underlying.write(data, off, len);
        track(len);
    }

    @Override
    public void write(byte[] data) throws IOException {
        this.underlying.write(data);
        track(data.length);
    }

    @Override
    public void write(int c) {
        try {
            this.underlying.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        track(1);
    }

    private void track(int len) {
        this.completed += len;
        this.mDownloadListener.onDownloadProgress(0, this.totalSize, this.completed);
    }
}