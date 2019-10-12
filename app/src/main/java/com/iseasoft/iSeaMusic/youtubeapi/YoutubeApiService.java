package com.iseasoft.iSeaMusic.youtubeapi;

import com.iseasoft.iSeaMusic.models.YoutubeMusicList;
import com.iseasoft.iSeaMusic.models.YoutubeVideoList;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface YoutubeApiService {
    String BASE_VIDEOS_LIST = "/videos?part=snippet&chart=mostPopular&maxResults=50&videoCategoryId=10&key=AIzaSyAUL7EJYg-eMnPFe_NA9vTs7wU9_JP4IKM";
    String BASE_MUSIC_LIST = "/search?part=snippet&maxResults=50&videoCategoryId=10&key=AIzaSyAUL7EJYg-eMnPFe_NA9vTs7wU9_JP4IKM";
    String BASE_PARAMETERS_ARTIST = "/?method=artist.getinfo&api_key=fdb3a51437d4281d4d64964d333531d4&format=json";


    @GET(BASE_VIDEOS_LIST)
    void getVideos(@Query("regionCode") String countryCode, Callback<YoutubeVideoList> callback);

    @GET(BASE_MUSIC_LIST)
    void getMusic(@Query("type") String type, @Query("topicId") String topicId, @Query("regionCode") String countryCode, Callback<YoutubeMusicList> callback);
}
