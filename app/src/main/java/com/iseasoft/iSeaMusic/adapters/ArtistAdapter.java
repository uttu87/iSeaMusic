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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.appthemeengine.Config;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.lastfmapi.LastFmClient;
import com.iseasoft.iSeaMusic.lastfmapi.callbacks.ArtistInfoListener;
import com.iseasoft.iSeaMusic.lastfmapi.models.ArtistQuery;
import com.iseasoft.iSeaMusic.lastfmapi.models.LastfmArtist;
import com.iseasoft.iSeaMusic.models.Artist;
import com.iseasoft.iSeaMusic.utils.Helpers;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.PreferencesUtility;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;
import com.iseasoft.iSeaMusic.widgets.BubbleTextGetter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

public class ArtistAdapter extends AdsAdapter implements BubbleTextGetter {

    private Activity mContext;

    public ArtistAdapter(Activity context, List<Artist> arraylist) {
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(arraylist);
        this.mContext = context;
        this.isGrid = PreferencesUtility.getInstance(mContext).isArtistsInGrid();
    }

    public static int getOpaqueColor(@ColorInt int paramInt) {
        return 0xFF000000 | paramInt;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                return super.onCreateViewHolder(viewGroup, viewType);
            case DATA_VIEW_TYPE:
            default:
                if (isGrid) {
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist_grid, null);
                    ItemHolder ml = new ItemHolder(v);
                    return ml;
                } else {
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist, null);
                    ItemHolder ml = new ItemHolder(v);
                    return ml;
                }
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int i) {
        int viewType = getItemViewType(i);
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                super.onBindViewHolder(viewHolder, i);
                break;
            case DATA_VIEW_TYPE:
            default:
                final Artist localItem = (Artist) arraylist.get(i);
                final ItemHolder itemHolder = (ItemHolder) viewHolder;

                itemHolder.name.setText(localItem.name);
                String albumNmber = iSeaUtils.makeLabel(mContext, R.plurals.Nalbums, localItem.albumCount);
                String songCount = iSeaUtils.makeLabel(mContext, R.plurals.Nsongs, localItem.songCount);
                itemHolder.albums.setText(iSeaUtils.makeCombinedString(mContext, albumNmber, songCount));


                LastFmClient.getInstance(mContext).getArtistInfo(new ArtistQuery(localItem.name), new ArtistInfoListener() {
                    @Override
                    public void artistInfoSucess(LastfmArtist artist) {
                        if (artist != null && artist.mArtwork != null) {
                            if (isGrid) {
                                ImageLoader.getInstance().displayImage(artist.mArtwork.get(2).mUrl, itemHolder.artistImage,
                                        new DisplayImageOptions.Builder().cacheInMemory(true)
                                                .cacheOnDisk(true)
                                                .showImageOnLoading(R.drawable.ic_empty_music2)
                                                .resetViewBeforeLoading(true)
                                                .displayer(new FadeInBitmapDisplayer(400))
                                                .build(), new SimpleImageLoadingListener() {
                                            @Override
                                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                                if (isGrid && loadedImage != null) {
                                                    new Palette.Builder(loadedImage).generate(new Palette.PaletteAsyncListener() {
                                                        @Override
                                                        public void onGenerated(Palette palette) {
                                                            int color = palette.getVibrantColor(Color.parseColor("#66000000"));
                                                            itemHolder.footer.setBackgroundColor(color);
                                                            Palette.Swatch swatch = palette.getVibrantSwatch();
                                                            int textColor;
                                                            if (swatch != null) {
                                                                textColor = getOpaqueColor(swatch.getTitleTextColor());
                                                            } else
                                                                textColor = Color.parseColor("#ffffff");

                                                            itemHolder.name.setTextColor(textColor);
                                                            itemHolder.albums.setTextColor(textColor);
                                                        }
                                                    });
                                                }

                                            }

                                            @Override
                                            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                                if (isGrid) {
                                                    itemHolder.footer.setBackgroundColor(0);
                                                    if (mContext != null) {
                                                        int textColorPrimary = Config.textColorPrimary(mContext, Helpers.getATEKey(mContext));
                                                        itemHolder.name.setTextColor(textColorPrimary);
                                                        itemHolder.albums.setTextColor(textColorPrimary);
                                                    }
                                                }
                                            }
                                        });
                            } else {
                                ImageLoader.getInstance().displayImage(artist.mArtwork.get(1).mUrl, itemHolder.artistImage,
                                        new DisplayImageOptions.Builder().cacheInMemory(true)
                                                .cacheOnDisk(true)
                                                .showImageOnLoading(R.drawable.ic_empty_music2)
                                                .resetViewBeforeLoading(true)
                                                .displayer(new FadeInBitmapDisplayer(400))
                                                .build());
                            }
                        }
                    }

                    @Override
                    public void artistInfoFailed() {

                    }
                });

                if (iSeaUtils.isLollipop())
                    itemHolder.artistImage.setTransitionName("transition_artist_art" + i);
        }

    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == NATIVE_EXPRESS_AD_VIEW_TYPE) {
            return 0;
        }
        return ((Artist)arraylist.get(position)).id;
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @Override
    public String getTextToShowInBubble(final int pos) {
        if (arraylist == null || arraylist.size() == 0
                || getItemViewType(pos) == NATIVE_EXPRESS_AD_VIEW_TYPE)
            return "";

        return Character.toString(((Artist)arraylist.get(pos)).name.charAt(0));
    }

    public void updateDataSet(List<Artist> arrayList) {
        this.arraylist.clear();
        this.arraylist.addAll(arrayList);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView name, albums;
        protected ImageView artistImage;
        protected View footer;

        public ItemHolder(View view) {
            super(view);
            this.name = (TextView) view.findViewById(R.id.artist_name);
            this.albums = (TextView) view.findViewById(R.id.album_song_count);
            this.artistImage = (ImageView) view.findViewById(R.id.artistImage);
            this.footer = view.findViewById(R.id.footer);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            NavigationUtils.navigateToArtist(mContext, ((Artist)arraylist.get(getAdapterPosition())).id,
                    new Pair<View, String>(artistImage, "transition_artist_art" + getAdapterPosition()));
        }

    }
}




