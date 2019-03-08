package com.iseasoft.iSeaMusic.cloud.interfaces;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;

/**
 * Created by codecomplete on 2/6/18.
 */

public interface ResultAdapterCallback {
    void onFolderClicked(int position, FolderMetadata folder);

    void onFileClicked(int position, FileMetadata file);

    void onRootClicked();

    void onDownloadFileCallback(Object object);
}
