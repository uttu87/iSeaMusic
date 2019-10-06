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
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
import com.iseasoft.iSeaMusic.dialogs.AddPlaylistDialog;
import com.iseasoft.iSeaMusic.models.Song;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;

import java.util.ArrayList;
import java.util.List;

public class AlbumSongsAdapter extends BaseSongAdapter {

    private Activity mContext;
    private long albumID;
    private long[] songIDs;

    public AlbumSongsAdapter(Activity context, List<Song> arraylist, long albumID) {
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(arraylist);
        this.mContext = context;
        this.songIDs = getSongIds();
        this.albumID = albumID;
        this.isGrid = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                return super.onCreateViewHolder(viewGroup, viewType);
            case DATA_VIEW_TYPE:
            default:
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_album_song, null);
                ItemHolder ml = new ItemHolder(v);
                return ml;
        }


    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

        int viewType = getItemViewType(i);
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                super.onBindViewHolder(viewHolder, i);
                break;
            case DATA_VIEW_TYPE:
            default:
                Song localItem = (Song) arraylist.get(i);
                ItemHolder itemHolder = (ItemHolder) viewHolder;

                itemHolder.title.setText(localItem.title);
                itemHolder.duration.setText(iSeaUtils.makeShortTimeString(mContext, (localItem.duration) / 1000));
                int tracknumber = localItem.trackNumber;
                if (tracknumber == 0) {
                    itemHolder.trackNumber.setText("-");
                } else itemHolder.trackNumber.setText(String.valueOf(tracknumber));

                setOnPopupMenuListener(itemHolder, i);
                break;
        }


    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int position) {

        itemHolder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu menu = new PopupMenu(mContext, v);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final Song song = (Song) arraylist.get(position);
                        switch (item.getItemId()) {
                            case R.id.popup_song_play:
                                MusicPlayer.playAll(mContext, getSongIds(), position, -1, iSeaUtils.IdType.NA, false);
                                break;
                            case R.id.popup_song_play_next:
                                long[] ids = new long[1];
                                ids[0] = song.id;
                                MusicPlayer.playNext(mContext, ids, -1, iSeaUtils.IdType.NA);
                                break;
                            case R.id.popup_song_goto_album:
                                NavigationUtils.goToAlbum(mContext, song.albumId);
                                break;
                            case R.id.popup_song_goto_artist:
                                NavigationUtils.goToArtist(mContext, song.artistId);
                                break;
                            case R.id.popup_song_addto_queue:
                                long[] id = new long[1];
                                id[0] = song.id;
                                MusicPlayer.addToQueue(mContext, id, -1, iSeaUtils.IdType.NA);
                                break;
                            case R.id.popup_song_addto_playlist:
                                AddPlaylistDialog.newInstance(song).show(((AppCompatActivity) mContext).getSupportFragmentManager(), "ADD_PLAYLIST");
                                break;
                            case R.id.popup_song_share:
                                iSeaUtils.shareTrack(mContext, song.id);
                                break;
                            case R.id.popup_song_delete:
                                long[] deleteIds = {song.id};
                                iSeaUtils.showDeleteDialog(mContext, song.title, deleteIds, AlbumSongsAdapter.this, position);
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

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    public long[] getSongIds() {
        long[] ret = new long[getItemCount()];
        for (int i = 0; i < getItemCount(); i++) {
            if(getItemViewType(i) == NATIVE_EXPRESS_AD_VIEW_TYPE) {
                ret[i] = 0;
            } else {
                ret[i] = ((Song)arraylist.get(i)).id;
            }
        }

        return ret;
    }

    @Override
    public void updateDataSet(List<Song> arraylist) {
        this.arraylist.clear();
        this.arraylist.addAll(arraylist);
        this.songIDs = getSongIds();
    }

    @Override
    public void removeSongAt(int i) {
        arraylist.remove(i);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title, duration, trackNumber;
        protected ImageView menu;

        public ItemHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.song_title);
            this.duration = (TextView) view.findViewById(R.id.song_duration);
            this.trackNumber = (TextView) view.findViewById(R.id.trackNumber);
            this.menu = (ImageView) view.findViewById(R.id.popup_menu);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playAll(mContext, getSongIds(), getAdapterPosition(), albumID,
                            iSeaUtils.IdType.Album, false,
                            (Song)arraylist.get(getAdapterPosition()), true);
                }
            }, 100);

        }

    }

}



