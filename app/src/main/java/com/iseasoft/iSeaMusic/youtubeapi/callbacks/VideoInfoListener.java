package com.iseasoft.iSeaMusic.youtubeapi.callbacks;

import com.iseasoft.iSeaMusic.models.YoutubeVideoList;

public interface VideoInfoListener {
    void videoInfoSuccess(YoutubeVideoList youtubeVideos);

    void videoInfoFailed();
}
