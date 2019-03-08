package com.iseasoft.iSeaMusic.cloud.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

/**
 * Async task to list items in a folder
 */
class ListFolderTask extends AsyncTask<String, Void, ListFolderResult> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onDataLoaded(ListFolderResult result);

        void onError(Exception e);
    }

    public ListFolderTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(ListFolderResult result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result);
        }
    }

    @Override
    protected ListFolderResult doInBackground(String... params) {
        try {
            ListFolderResult files = mDbxClient.files().listFolderBuilder(params[0]).withIncludeMediaInfo(true).start();
            return removeAnotherFileType(files);
        } catch (DbxException e) {
            mException = e;
        }

        return null;
    }

    private ListFolderResult removeAnotherFileType(ListFolderResult result) {
        if (null == result) {
            return null;
        }
        int i = 0;
        while (i < result.getEntries().size()) {
            Metadata item = result.getEntries().get(i);
            String fileName = item.getName();

            if (item instanceof FolderMetadata) {
                i++;
            } else if (item instanceof FileMetadata) {
                if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a")) {
                    i++;
                } else {
                    result.getEntries().remove(i);
                }
            }

        }
        return result;
    }

}
