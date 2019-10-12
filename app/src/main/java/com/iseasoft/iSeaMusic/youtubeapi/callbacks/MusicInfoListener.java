package com.iseasoft.iSeaMusic.youtubeapi.callbacks;

import com.iseasoft.iSeaMusic.models.YoutubeMusicList;

public interface MusicInfoListener {
    void videoInfoSuccess(YoutubeMusicList youtubeMusicList);

    void videoInfoFailed();
}
