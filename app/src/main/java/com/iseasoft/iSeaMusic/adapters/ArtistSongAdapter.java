/*
 * Copyright (C) 2015 Naman Dwivedi
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.iseasoft.iSeaMusic.adapters;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.iseasoft.iSeaMusic.MusicPlayer;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.dataloaders.ArtistAlbumLoader;
import com.iseasoft.iSeaMusic.dialogs.AddPlaylistDialog;
import com.iseasoft.iSeaMusic.models.Song;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class ArtistSongAdapter extends BaseSongAdapter {

    private Activity mContext;
    private long artistID;
    private long[] songIDs;

    public ArtistSongAdapter(Activity context, List<Song> arraylist, long artistID) {
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(arraylist);
        this.mContext = context;
        this.artistID = artistID;
        this.songIDs = getSongIds();
        this.isGrid = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == 0) {
            View v0 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.artist_detail_albums_header, null);
            ItemHolder ml = new ItemHolder(v0);
            return ml;
        } else if (viewType == NATIVE_EXPRESS_AD_VIEW_TYPE) {
            return super.onCreateViewHolder(viewGroup, viewType);
        } else {
            View v2 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist_song, null);
            ItemHolder ml = new ItemHolder(v2);
            return ml;
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (getItemViewType(i) == NATIVE_EXPRESS_AD_VIEW_TYPE) {
            super.onBindViewHolder(viewHolder, i);
            return;
        }

        final ItemHolder itemHolder = (ItemHolder) viewHolder;

        if (getItemViewType(i) == 0) {
            //nothing
            setUpAlbums(itemHolder.albumsRecyclerView);
        } else {
            Song localItem = (Song) arraylist.get(i);
            itemHolder.title.setText(localItem.title);
            itemHolder.album.setText(localItem.albumName);

            ImageLoader.getInstance().displayImage(iSeaUtils.getAlbumArtUri(localItem.albumId).toString(),
                    itemHolder.albumArt, new DisplayImageOptions.Builder()
                            .cacheInMemory(true).showImageOnLoading(R.drawable.ic_empty_music2).resetViewBeforeLoading(true).build());
            setOnPopupMenuListener(itemHolder, i - 1);
        }

    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == NATIVE_EXPRESS_AD_VIEW_TYPE) {
            return;
        }
        final ItemHolder itemHolder = (ItemHolder) viewHolder;
        if (itemHolder.getItemViewType() == 0) {
            clearExtraSpacingBetweenCards(itemHolder.albumsRecyclerView);
        }

    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int position) {

        itemHolder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu menu = new PopupMenu(mContext, v);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Song songNext = (Song) arraylist.get(position + 1);
                        switch (item.getItemId()) {
                            case R.id.popup_song_play:
                                MusicPlayer.playAll(mContext, getSongIds(), position + 1, -1, iSeaUtils.IdType.NA, false);
                                break;
                            case R.id.popup_song_play_next:
                                long[] ids = new long[1];
                                ids[0] = songNext.id;
                                MusicPlayer.playNext(mContext, ids, -1, iSeaUtils.IdType.NA);
                                break;
                            case R.id.popup_song_goto_album:
                                NavigationUtils.goToAlbum(mContext, songNext.albumId);
                                break;
                            case R.id.popup_song_goto_artist:
                                NavigationUtils.goToArtist(mContext, songNext.artistId);
                                break;
                            case R.id.popup_song_addto_queue:
                                long[] id = new long[1];
                                id[0] = songNext.id;
                                MusicPlayer.addToQueue(mContext, id, -1, iSeaUtils.IdType.NA);
                                break;
                            case R.id.popup_song_addto_playlist:
                                AddPlaylistDialog.newInstance(songNext).show(((AppCompatActivity) mContext).getSupportFragmentManager(), "ADD_PLAYLIST");
                                break;
                            case R.id.popup_song_share:
                                iSeaUtils.shareTrack(mContext, songNext.id);
                                break;
                            case R.id.popup_song_delete:
                                long[] deleteIds = {songNext.id};
                                iSeaUtils.showDeleteDialog(mContext, songNext.title, deleteIds, ArtistSongAdapter.this, position + 1);
                                break;
                        }
                        return false;
                    }
                });
                menu.inflate(R.menu.popup_song);
                menu.show();
            }
        });
    }

    private void setUpAlbums(RecyclerView albumsRecyclerview) {

        albumsRecyclerview.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        albumsRecyclerview.setHasFixedSize(true);

        //to add spacing between cards
        int spacingInPixels = mContext.getResources().getDimensionPixelSize(R.dimen.spacing_card);
        albumsRecyclerview.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        albumsRecyclerview.setNestedScrollingEnabled(false);


        ArtistAlbumAdapter mAlbumAdapter = new ArtistAlbumAdapter(mContext, ArtistAlbumLoader.getAlbumsForArtist(mContext, artistID));
        albumsRecyclerview.setAdapter(mAlbumAdapter);
    }

    private void clearExtraSpacingBetweenCards(RecyclerView albumsRecyclerview) {
        //to clear any extra spacing between cards
        int spacingInPixelstoClear = -(mContext.getResources().getDimensionPixelSize(R.dimen.spacing_card));
        albumsRecyclerview.addItemDecoration(new SpacesItemDecoration(spacingInPixelstoClear));

    }

    public long[] getSongIds() {
        List<Object> actualArraylist = new ArrayList<Object>(arraylist);
        //actualArraylist.remove(0);
        long[] ret = new long[actualArraylist.size()];
        for (int i = 0; i < actualArraylist.size(); i++) {
            if (getItemViewType(i) == NATIVE_EXPRESS_AD_VIEW_TYPE) {
                ret[i] = 0;
            } else {
                ret[i] = ((Song) actualArraylist.get(i)).id;
            }
        }
        return ret;
    }

    @Override
    public void removeSongAt(int i) {
        arraylist.remove(i);
        //updateDataSet(arraylist);
    }

    @Override
    public void updateDataSet(List<Song> arraylist) {
        this.arraylist.clear();
        this.arraylist.addAll(arraylist);
        this.songIDs = getSongIds();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        } else {
            return super.getItemViewType(position);
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title, album;
        protected ImageView albumArt, menu;
        protected RecyclerView albumsRecyclerView;

        public ItemHolder(View view) {
            super(view);

            this.albumsRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_album);

            this.title = (TextView) view.findViewById(R.id.song_title);
            this.album = (TextView) view.findViewById(R.id.song_album);
            this.albumArt = (ImageView) view.findViewById(R.id.albumArt);
            this.menu = (ImageView) view.findViewById(R.id.popup_menu);


            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playAll(mContext, getSongIds(), getAdapterPosition(), artistID,
                            iSeaUtils.IdType.Artist, false,
                            (Song) arraylist.get(getAdapterPosition()), true);
                }
            }, 100);

        }

    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {

            //the padding from left
            outRect.left = space;


        }
    }
}




