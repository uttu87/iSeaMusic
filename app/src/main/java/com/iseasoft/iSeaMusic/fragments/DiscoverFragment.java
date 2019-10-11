package com.iseasoft.iSeaMusic.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.adapters.YoutubeVideoAdapter;
import com.iseasoft.iSeaMusic.models.YoutubeVideoList;
import com.iseasoft.iSeaMusic.utils.PreferencesUtility;
import com.iseasoft.iSeaMusic.widgets.BaseRecyclerView;
import com.iseasoft.iSeaMusic.widgets.FastScroller;
import com.iseasoft.iSeaMusic.youtubeapi.YoutubeApiClient;
import com.iseasoft.iSeaMusic.youtubeapi.callbacks.VideoInfoListener;

public class DiscoverFragment extends Fragment {

    private YoutubeVideoAdapter mAdapter;
    private BaseRecyclerView recyclerView;
    private FastScroller fastScroller;
    private PreferencesUtility mPreferences;
    private GridLayoutManager layoutManager;
    private RecyclerView.ItemDecoration itemDecoration;
    private boolean isGrid;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferencesUtility.getInstance(getActivity());
        isGrid = mPreferences.isAlbumsInGrid();
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


        YoutubeApiClient.getInstance(getActivity()).getVideos(new VideoInfoListener() {
            @Override
            public void videoInfoSuccess(YoutubeVideoList youtubeVideos) {
                mAdapter = new YoutubeVideoAdapter(getActivity(), youtubeVideos.getItems());
                recyclerView.setAdapter(mAdapter);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter = null;
    }
}
