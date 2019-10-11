package com.iseasoft.iSeaMusic.youtubeapi;

import android.content.Context;

import com.iseasoft.iSeaMusic.models.YoutubeVideoList;
import com.iseasoft.iSeaMusic.youtubeapi.callbacks.VideoInfoListener;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class YoutubeApiClient {
    public static final String BASE_API_URL = "https://www.googleapis.com/youtube/v3";

    private static YoutubeApiClient sInstance = null;
    private YoutubeApiService mYoutubeApiService;
    private Context context;
    private static final Object sLock = new Object();

    public static YoutubeApiClient getInstance(Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new YoutubeApiClient();
                sInstance.context = context;
                sInstance.mYoutubeApiService = YoutubeApiServiceFactory.createStatic(context, BASE_API_URL, YoutubeApiService.class);

            }
            return sInstance;
        }
    }

    public void getVideos(final VideoInfoListener listener) {
        mYoutubeApiService.getVideos(new Callback<YoutubeVideoList>() {
            @Override
            public void success(YoutubeVideoList youtubeVideos, Response response) {
                listener.videoInfoSuccess(youtubeVideos);
            }

            @Override
            public void failure(RetrofitError error) {
                listener.videoInfoFailed();
            }
        });
    }
}
