package com.iseasoft.iSeaMusic.cloud.dropbox;

import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jp.co.mytrax.traxcore.db.domain.Media;


/**
 * Here we show uploading a file in a background thread, trying to show typical exception handling and flow of control for an app that uploads a file from Dropbox.
 */
public class UploadFileToDropboxTask extends AsyncTask<Object, Integer, FileMetadata> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private Callback mCallback;
    private UploadUploader mUpload;

    public interface Callback {
        void onUploadComplete(FileMetadata result);

        void onError(Exception e);

        public void onUploadProgress(Media media, int percent);

        public void onFinishUpload(Media media);

        public void onErrorUpload(Media media, Exception e);
    }

    private final String mPath;
    private final File mFile;
    private final Media mMedia;

    public UploadFileToDropboxTask(Context context, DbxClientV2 dbxClient, Media media, String dropboxPath, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
        mMedia = media;
        mFile = new File(media.getData());
        mPath = dropboxPath;
    }

    @Override
    protected FileMetadata doInBackground(Object... params) {
        try {
            String artists = mMedia.getArtists();
            String album = mMedia.getAlbum();
            String createdFolder = "";
            if (artists != null && !artists.isEmpty()) {
                createdFolder = artists + File.separator;
                if (album != null && !album.isEmpty()) {
                    createdFolder = createdFolder + album + File.separator;
                }
            }

            if (mFile != null) {
                try {
                    String remoteFileName = mFile.getPath();
                    mUpload = mDbxClient.files().uploadBuilder(remoteFileName).withMode(WriteMode.OVERWRITE)
                            .start();
                    long size = mFile.length();

                    FileInputStream fout = new FileInputStream(mFile);

                    mUpload.uploadAndFinish(new ProgressInputputStream(size, fout, new Listener() {
                        @Override
                        public void progress(long completed, long totalSize) {
                            if (isCancelled()) {
                                if (mUpload != null) {
                                    DropboxClientFactory.getOkHttpClient().dispatcher().cancelAll();
                                }
                            } else {
                                float percent = (completed / (totalSize * 1.0f)) * 100;
                                publishProgress((int) percent);
                            }
                        }
                    }));
                } catch (DbxException e) {
                    mCallback.onErrorUpload(mMedia, e);
                } catch (IOException e) {
                    mCallback.onErrorUpload(mMedia, e);
                } catch (Exception e) {
                    mCallback.onErrorUpload(mMedia, e);
                }
            }

        } catch (Exception e) {
            mCallback.onErrorUpload(mMedia, e);
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mCallback != null) {
            int percent = (int) progress[0];
            if (percent < 0 || percent > 100) {
                mCallback.onErrorUpload(mMedia, null);
            } else {
                mCallback.onUploadProgress(mMedia, percent);
            }

        }
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        if (mCallback != null) {
            mCallback.onFinishUpload(mMedia);
        }
    }

    public void stop() {
        mCallback = null;
        cancel(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mUpload != null) {
                    DropboxClientFactory.getOkHttpClient().dispatcher().cancelAll();
                }
            }
        }).start();
    }

    @Override
    protected void onCancelled() {
        mCallback = null;
        mUpload = null;
    }

    public class ProgressInputputStream extends InputStream {
        InputStream underlying;
        Listener listener;
        int completed;
        long totalSize;

        public ProgressInputputStream(long totalSize, InputStream underlying, Listener listener) {
            this.underlying = underlying;
            this.listener = listener;
            this.completed = 0;
            this.totalSize = totalSize;
        }

        @Override
        public int read(byte[] b) throws IOException {
            track(b.length);
            return this.underlying.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            track(len);
            return this.underlying.read(b, off, len);
        }

        @Override
        public int read() throws IOException {
            return 0;
        }

        private void track(int len) {
            this.completed += len;
            this.listener.progress(this.completed, this.totalSize);
        }
    }

    public interface Listener {
        public void progress(long completed, long totalSize);
    }
}