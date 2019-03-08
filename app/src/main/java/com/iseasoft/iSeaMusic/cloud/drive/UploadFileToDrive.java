package com.iseasoft.iSeaMusic.cloud.drive;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import jp.co.mytrax.traxcore.db.domain.Media;

public class UploadFileToDrive extends AsyncTask<Void, Long, Void> {
    private final String TAG = "UploadFileToDrive";
    private final Drive mService;
    private final Media mMedia;
    private FileInputStream fileStream;
    private InputStreamContent mediaContent;
    private boolean isPauseUpload = false;
    private final String mParentId;
    private IUploadCallback mCallback;
    private java.io.File fileContent;
    private static final int UPLOAD_NORMAL = 0;
    private static final int UPLOAD_FINISH = 1;
    private static final int UPLOAD_HAS_ERROR = 2;
    private int mUploadState = 0;

    public Media getSong() {
        return mMedia;
    }

    public interface IUploadCallback {
        public void onUploadProgress(Media media, int percent);

        public void onFinishUpload(Media media);

        public void onErrorUpload(Media media, Exception e);
    }

    public void setUploadCallback(IUploadCallback callback) {
        this.mCallback = callback;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mCallback != null && !isCancelled()) {
            if (mUploadState == UPLOAD_HAS_ERROR)
                mCallback.onErrorUpload(mMedia, null);
            else
                mCallback.onFinishUpload(mMedia);
            mCallback = null;
        }
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        if (mCallback != null && fileContent != null) {
            Long fileLen = fileContent.length();
            if (fileLen == 0 && mCallback != null) {
                mCallback.onErrorUpload(mMedia, null);
                return;
            }
            int percent = (int) (100.0 * (double) progress[0] / fileLen + 0.5);
            if (percent >= 0 && percent <= 100 && mCallback != null)
                mCallback.onUploadProgress(mMedia, percent);
        }
    }

    // constructor method
    public UploadFileToDrive(Drive service, Media song, String parentId) {
        mService = service;
        mMedia = song;
        mParentId = parentId;
    }

    private void uploadSingleFile() {

        String parentId = getFolderIdOfUploadingFile();
        if (mUploadState != UPLOAD_NORMAL || parentId == null)
            return;
        try {
            String extension = FilenameUtils.getExtension(mMedia.getData());
            // File's metadata.
            File body = new File();
            body.setTitle(mMedia.getTitle() + "." + extension);
            body.setMimeType(mMedia.getMimeType());
            // Set the parent folder.
            if (parentId != null && !parentId.equals("root")) // parent isn't root folder.
            {
                // creating parent Folder reference that constrains uploading file.
                ParentReference parentReference = new ParentReference();
                parentReference.setId(parentId);
                body.setParents(Arrays.asList(parentReference));
            }

            // File's content.
            fileContent = new java.io.File(mMedia.getData()); // param is file path
            fileStream = new FileInputStream(fileContent);
            mediaContent = new InputStreamContent(mMedia.getMimeType(), new BufferedInputStream(fileStream));

            // upload file
            Insert insert = mService.files().insert(body, mediaContent);
            MediaHttpUploader uploader = insert.getMediaHttpUploader();
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(new MediaHttpUploaderProgressListener() {

                @Override
                public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException {
                    publishProgress(mediaHttpUploader.getNumBytesUploaded()); // to update UI
                }

            });
            insert.execute();
        } catch (IOException e) {
            Log.i(TAG, "uploading fail: " + Log.getStackTraceString(e));
            if (mCallback != null && !isPauseUpload)
                mCallback.onErrorUpload(mMedia, e);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mCallback != null)
            uploadSingleFile();
        return null;
    }

    private String getFolderIdOfUploadingFile() {
        String albumFolderId = null;
        String artistName = mMedia.getArtists();
        String albumName = mMedia.getAlbum();
        String artistFolderId = findFolder(artistName, mParentId);
        if (artistFolderId != null) {
            String AlbumFolderId = findFolder(albumName, artistFolderId);
            if (AlbumFolderId != null) {
                albumFolderId = AlbumFolderId;
                checkFileISExisted(mMedia.getTitle(), albumFolderId);
            } else if (!isPauseUpload)
                albumFolderId = createFolder(albumName, artistFolderId);
        } else if (mUploadState == UPLOAD_NORMAL) {
            artistFolderId = createFolder(artistName, mParentId);
            albumFolderId = createFolder(albumName, artistFolderId);
        }
        return albumFolderId;
    }

    private String findFolder(final String title, final String folderID) {
        com.google.api.services.drive.Drive.Files.List request = null;
        if (folderID == null)
            return null;

        try {
            request = mService.files().list();
            String newTitle = forRegex(title);
            String query = "'" + folderID + "' in parents" + " and trashed = false and mimeType='application/vnd.google-apps.folder' and title = '" + newTitle + "'";
            request.setQ(query).setMaxResults(1);
            request.setSpaces("drive");
            com.google.api.services.drive.model.FileList fileList = request.execute();
            if (fileList != null && fileList.getItems().size() > 0) {
                File file = fileList.getItems().get(0);
                return file.getId();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (request != null)
                request.setPageToken(null);
            mUploadState = UPLOAD_HAS_ERROR;
        }
        return null;
    }

    private void checkFileISExisted(final String title, final String folderID) {
        com.google.api.services.drive.Drive.Files.List request = null;

        try {
            request = mService.files().list();
            String newTitle = forRegex(title);
            String query = "'" + folderID + "' in parents" + " and trashed = false and " +
                    "mimeType contains 'audio/' and " +
                    "(title contains '" + newTitle + "' or title = '" + newTitle + "' )";
            request.setQ(query).setMaxResults(1);
            request.setSpaces("drive");
            com.google.api.services.drive.model.FileList fileList = request.execute();
            if (fileList != null && fileList.getItems().size() > 0)
                mUploadState = UPLOAD_FINISH;
        } catch (IOException e) {
            e.printStackTrace();
            if (request != null)
                request.setPageToken(null);
            mUploadState = UPLOAD_HAS_ERROR;
        }
    }

    private String createFolder(String title, String parentId) {
        if (parentId == null) {
            mUploadState = UPLOAD_HAS_ERROR;
            return null;
        }
        File fileMetadata = new File();
        fileMetadata.setTitle(title);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        if (!parentId.equals("root")) // parent isn't root folder.
        {
            // creating parent Folder reference that constrains uploading file.
            ParentReference parentReference = new ParentReference();
            parentReference.setId(parentId);
            fileMetadata.setParents(Arrays.asList(parentReference));
        }
        try {
            File file = mService.files().insert(fileMetadata).execute();
            if (file != null)
                return file.getId();
        } catch (IOException e) {
            Log.i(TAG, "create folder is fails: " + Log.getStackTraceString(e));
            mUploadState = UPLOAD_HAS_ERROR;
        }
        return null;
    }

    public void pauseUpload() {
        isPauseUpload = true;
        stop();
    }

    public void stop() {
        try {
            cancel(true);
            if (fileStream != null && mediaContent != null) {
                mediaContent.setCloseInputStream(true);
                fileStream.close();
            }
            mCallback = null;
        } catch (IOException e) {
            Log.i(TAG, "stop uploading fail: " + Log.getStackTraceString(e));
            mUploadState = UPLOAD_HAS_ERROR;
        }
    }

    @Override
    protected void onCancelled() {
        mCallback = null;
    }

    private String forRegex(String title) {
        if (title != null) {
            return title.replaceAll("['\"]", "%");
        }
        return "";
    }
}
