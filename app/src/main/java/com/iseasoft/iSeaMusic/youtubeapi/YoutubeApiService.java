package com.iseasoft.iSeaMusic.youtubeapi;

import com.iseasoft.iSeaMusic.models.YoutubeVideoList;

import retrofit.Callback;
import retrofit.http.GET;

public interface YoutubeApiService {
    String BASE_VIDEOS_LIST = "/videos?part=snippet&chart=mostPopular&maxResults=50&regionCode=VN&videoCategoryId=10&key=AIzaSyAUL7EJYg-eMnPFe_NA9vTs7wU9_JP4IKM";
    String BASE_PARAMETERS_ARTIST = "/?method=artist.getinfo&api_key=fdb3a51437d4281d4d64964d333531d4&format=json";


    @GET(BASE_VIDEOS_LIST)
    void getVideos(Callback<YoutubeVideoList> callback);
}
