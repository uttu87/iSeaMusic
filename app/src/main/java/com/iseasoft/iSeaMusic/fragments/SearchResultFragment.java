package com.iseasoft.iSeaMusic.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iseasoft.iSeaMusic.MusicPlayer;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.adapters.YoutubeMusicAdapter;
import com.iseasoft.iSeaMusic.models.YoutubeMusic;
import com.iseasoft.iSeaMusic.models.YoutubeMusicList;
import com.iseasoft.iSeaMusic.utils.PreferencesUtility;
import com.iseasoft.iSeaMusic.widgets.BaseRecyclerView;
import com.iseasoft.iSeaMusic.widgets.FastScroller;
import com.iseasoft.iSeaMusic.youtubeapi.YoutubeApiClient;
import com.iseasoft.iSeaMusic.youtubeapi.callbacks.MusicInfoListener;
import com.orhanobut.logger.Logger;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SearchResultFragment extends AdsFragment {

    private String keySearch;
    private YoutubeMusicAdapter mAdapter;
    private BaseRecyclerView recyclerView;
    private FastScroller fastScroller;
    private PreferencesUtility mPreferences;
    private GridLayoutManager layoutManager;
    private RecyclerView.ItemDecoration itemDecoration;
    private boolean isGrid;
    private YoutubeMusicAdapter.OnVideoListener mListener;
    private CompositeDisposable compositeDisposable;
    private boolean isRequesting;

    public static SearchResultFragment newInstance(String keySearch) {
        SearchResultFragment fragment = new SearchResultFragment();
        fragment.keySearch = keySearch;
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferencesUtility.getInstance(getActivity());
        isGrid = false;
        compositeDisposable = new CompositeDisposable();
        isRequesting = false;
        mListener = new YoutubeMusicAdapter.OnVideoListener() {
            @Override
            public void onClick(YoutubeMusic video) {
                if (isRequesting) {
                    return;
                }
                isRequesting = true;
                MusicPlayer.setOnline(true);
                MusicPlayer.setTrackName(video.getSnippet().getTitle());
                MusicPlayer.setTrackDes(video.getSnippet().getDescription());
                MusicPlayer.setTrackUrl(video.getUrl());
                MusicPlayer.pause();
                Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().getInfo(video.getId().getVideoId()))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(streamInfo -> {
                            String videoUrl = getVideoUrl(streamInfo);
                            if (!TextUtils.isEmpty(videoUrl)) {
                                MusicPlayer.openFile(videoUrl);
                                MusicPlayer.playOrPause();
                            }
                            isRequesting = false;
                        }, e -> {
                            isRequesting = false;
                            Logger.e(e, "failed to get stream info");
                        });
                compositeDisposable.add(disposable);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recyclerview, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setEmptyView(getActivity(), rootView.findViewById(R.id.list_empty), "No media found");
        fastScroller = rootView.findViewById(R.id.fastscroller);
        fastScroller.setRecyclerView(recyclerView);
        setLayoutManager();

        YoutubeApiClient.getInstance(getActivity()).searchMusic(keySearch, new MusicInfoListener() {
            @Override
            public void videoInfoSuccess(YoutubeMusicList youtubeVideos) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    spaceBetweenAds = isGrid ? GRID_VIEW_ADS_COUNT : LIST_VIEW_ADS_COUNT;
                    mAdapter = new YoutubeMusicAdapter(getActivity(), youtubeVideos.getItems());
                    mAdapter.setGrid(isGrid);
                    mAdapter.setListener(mListener);
                    recyclerView.setAdapter(mAdapter);
                    generateDataSet(mAdapter);
                });
            }

            @Override
            public void videoInfoFailed() {

            }
        });
        return rootView;
    }

    private void setLayoutManager() {
        if (isGrid) {
            layoutManager = new GridLayoutManager(getActivity(), 2);
            fastScroller.setVisibility(View.GONE);
        } else {
            layoutManager = new GridLayoutManager(getActivity(), 1);
            fastScroller.setVisibility(View.VISIBLE);
            fastScroller.setRecyclerView(recyclerView);
        }
        recyclerView.setLayoutManager(layoutManager);
    }

    private String getVideoUrl(VideoInfo streamInfo) {
        String videoUrl = null;
        if (null == streamInfo || null == streamInfo.formats) {
            return null;
        }
        for (VideoFormat f : streamInfo.formats) {
            if ("m4a".equals(f.ext)) {
                videoUrl = f.url;
                break;
            }
        }
        return videoUrl;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter = null;
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
        mListener = null;
        compositeDisposable = null;
        isRequesting = false;
    }
}
