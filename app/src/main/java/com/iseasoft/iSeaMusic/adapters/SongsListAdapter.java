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

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.appthemeengine.Config;
import com.iseasoft.iSeaMusic.MusicPlayer;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.dialogs.AddPlaylistDialog;
import com.iseasoft.iSeaMusic.models.Song;
import com.iseasoft.iSeaMusic.utils.Helpers;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;
import com.iseasoft.iSeaMusic.widgets.BubbleTextGetter;
import com.iseasoft.iSeaMusic.widgets.MusicVisualizer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class SongsListAdapter extends BaseSongAdapter implements BubbleTextGetter {

    public int currentlyPlayingPosition;
    private AppCompatActivity mContext;
    private long[] songIDs;
    private boolean isPlaylist;
    private boolean animate;
    private int lastPosition = -1;
    private String ateKey;
    private long playlistId;

    public SongsListAdapter(AppCompatActivity context, List<Song> arraylist, boolean isPlaylistSong, boolean animate) {
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(arraylist);
        this.mContext = context;
        this.isPlaylist = isPlaylistSong;
        this.songIDs = getSongIds();
        this.ateKey = Helpers.getATEKey(context);
        this.animate = animate;
        this.isGrid = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                return super.onCreateViewHolder(viewGroup, viewType);
            case DATA_VIEW_TYPE:
            default:
                if (isPlaylist) {
                    View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_song_playlist, null);
                    ItemHolder ml = new ItemHolder(v);
                    return ml;
                } else {
                    View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_song, null);
                    ItemHolder ml = new ItemHolder(v);
                    return ml;
                }
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
            default: {
                Song localItem = (Song) arraylist.get(i);
                final ItemHolder itemHolder = (ItemHolder) viewHolder;
                itemHolder.title.setText(localItem.title);
                itemHolder.artist.setText(localItem.artistName);

                ImageLoader.getInstance().displayImage(iSeaUtils.getAlbumArtUri(localItem.albumId).toString(),
                        itemHolder.albumArt, new DisplayImageOptions.Builder().cacheInMemory(true)
                                .showImageOnLoading(R.drawable.ic_empty_music2)
                                .resetViewBeforeLoading(true).build());

                if (MusicPlayer.getCurrentAudioId() == localItem.id) {
                    itemHolder.title.setTextColor(Config.accentColor(mContext, ateKey));
                    if (MusicPlayer.isPlaying()) {
                        itemHolder.visualizer.setColor(Config.accentColor(mContext, ateKey));
                        itemHolder.visualizer.setVisibility(View.VISIBLE);
                    } else {
                        itemHolder.visualizer.setVisibility(View.GONE);
                    }
                } else {
                    itemHolder.visualizer.setVisibility(View.GONE);
                    if (isPlaylist) {
                        itemHolder.title.setTextColor(Color.WHITE);
                    } else {
                        itemHolder.title.setTextColor(Config.textColorPrimary(mContext, ateKey));
                    }
                }


                if (animate && isPlaylist) {
                    if (iSeaUtils.isLollipop())
                        setAnimation(itemHolder.itemView, i);
                    else {
                        if (i > 10)
                            setAnimation(itemHolder.itemView, i);
                    }
                }


                setOnPopupMenuListener(itemHolder, i);
            }
            break;
        }

    }

    public void setPlaylistId(long playlistId) {
        this.playlistId = playlistId;
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @Override
    public List<Object> getDataSet() {
        return arraylist;
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int position) {

        itemHolder.popupMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu menu = new PopupMenu(mContext, v);

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Song song = (Song) arraylist.get(itemHolder.getAdapterPosition());
                        int position = itemHolder.getAdapterPosition();
                        switch (item.getItemId()) {
                            case R.id.popup_song_remove_playlist:
                                iSeaUtils.removeFromPlaylist(mContext, song.id, playlistId);
                                removeSongAt(position);
                                notifyItemRemoved(position);
                                break;
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
                                id[0] = ((Song) arraylist.get(position)).id;
                                MusicPlayer.addToQueue(mContext, id, -1, iSeaUtils.IdType.NA);
                                break;
                            case R.id.popup_song_addto_playlist:
                                AddPlaylistDialog.newInstance((Song) arraylist.get(position)).show(mContext.getSupportFragmentManager(), "ADD_PLAYLIST");
                                break;
                            case R.id.popup_song_share:
                                iSeaUtils.shareTrack(mContext, song.id);
                                break;
                            case R.id.popup_song_delete:
                                long[] deleteIds = {song.id};
                                iSeaUtils.showDeleteDialog(mContext, song.title, deleteIds, SongsListAdapter.this, position);
                                break;
                        }
                        return false;
                    }
                });
                menu.inflate(R.menu.popup_song);
                menu.show();
                if (isPlaylist)
                    menu.getMenu().findItem(R.id.popup_song_remove_playlist).setVisible(true);
            }
        });
    }

    public long[] getSongIds() {
        long[] ret = new long[getItemCount()];
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemViewType(i) == NATIVE_EXPRESS_AD_VIEW_TYPE) {
                ret[i] = 0;
            } else {
                ret[i] = ((Song) arraylist.get(i)).id;
            }
        }

        return ret;
    }

    @Override
    public String getTextToShowInBubble(final int pos) {
        if (arraylist == null || arraylist.size() == 0
                || getItemViewType(pos) == NATIVE_EXPRESS_AD_VIEW_TYPE)
            return "";
        Character ch = ((Song) arraylist.get(pos)).title.charAt(0);
        if (Character.isDigit(ch)) {
            return "#";
        } else
            return Character.toString(ch);
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.abc_slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void updateDataSet(List<Song> arraylist) {
        this.arraylist.clear();
        this.arraylist.addAll(arraylist);
        this.songIDs = getSongIds();
    }

    public Song getSongAt(int i) {
        return (Song) arraylist.get(i);
    }

    public void addSongTo(int i, Song song) {
        arraylist.add(i, song);
    }

    @Override
    public void removeSongAt(int i) {
        arraylist.remove(i);
        //updateDataSet(arraylist);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title, artist;
        protected ImageView albumArt, popupMenu;
        private MusicVisualizer visualizer;

        public ItemHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.song_title);
            this.artist = (TextView) view.findViewById(R.id.song_artist);
            this.albumArt = (ImageView) view.findViewById(R.id.albumArt);
            this.popupMenu = (ImageView) view.findViewById(R.id.popup_menu);
            visualizer = (MusicVisualizer) view.findViewById(R.id.visualizer);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playAll(mContext, getSongIds(), getAdapterPosition(), -1,
                            iSeaUtils.IdType.NA, false,
                            (Song) arraylist.get(getAdapterPosition()), false);
                    Handler handler1 = new Handler();
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(currentlyPlayingPosition);
                            notifyItemChanged(getAdapterPosition());
                        }
                    }, 50);
                }
            }, 100);


        }

    }
}


