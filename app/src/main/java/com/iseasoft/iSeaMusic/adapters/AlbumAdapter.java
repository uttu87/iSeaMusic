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
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.afollestad.appthemeengine.Config;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.models.Album;
import com.iseasoft.iSeaMusic.utils.Helpers;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.PreferencesUtility;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class AlbumAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Defining variables for view types
    private static final int DATA_VIEW_TYPE = 0;
    private static final int NATIVE_EXPRESS_AD_VIEW_TYPE = 1;

    private List<Object> arraylist;
    private Activity mContext;
    private boolean isGrid;


    public AlbumAdapter(Activity context, List<Object> arraylist) {
        this.arraylist = arraylist;
        this.mContext = context;
        this.isGrid = PreferencesUtility.getInstance(mContext).isAlbumsInGrid();

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        View v;
        switch (viewType) {
            case DATA_VIEW_TYPE:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(isGrid ? R.layout.item_album_grid : R.layout.item_album_list, null);
                ItemHolder ml = new ItemHolder(v);
                return ml;
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
            default:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(isGrid ? R.layout.item_album_grid_ads : R.layout.item_album_list_ads, null);
                return new NativeExpressAdViewHolder(v);
        }

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int i) {
        int viewType = getItemViewType(i);
        switch (viewType) {
            case DATA_VIEW_TYPE:
                final ItemHolder itemHolder = (ItemHolder) viewHolder;
                Album localItem = (Album) arraylist.get(i);

                itemHolder.title.setText(localItem.title);
                itemHolder.artist.setText(localItem.artistName);

                ImageLoader.getInstance().displayImage(iSeaUtils.getAlbumArtUri(localItem.id).toString(), itemHolder.albumArt,
                        new DisplayImageOptions.Builder().cacheInMemory(true)
                                .showImageOnLoading(R.drawable.ic_empty_music2)
                                .resetViewBeforeLoading(true)
                                .displayer(new FadeInBitmapDisplayer(400))
                                .build(), new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                if (isGrid) {
                                    new Palette.Builder(loadedImage).generate(new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(Palette palette) {
                                            Palette.Swatch swatch = palette.getVibrantSwatch();
                                            if (swatch != null) {
                                                int color = swatch.getRgb();
                                                itemHolder.footer.setBackgroundColor(color);
                                                int textColor = iSeaUtils.getBlackWhiteColor(swatch.getTitleTextColor());
                                                itemHolder.title.setTextColor(textColor);
                                                itemHolder.artist.setTextColor(textColor);
                                            } else {
                                                Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                                                if (mutedSwatch != null) {
                                                    int color = mutedSwatch.getRgb();
                                                    itemHolder.footer.setBackgroundColor(color);
                                                    int textColor = iSeaUtils.getBlackWhiteColor(mutedSwatch.getTitleTextColor());
                                                    itemHolder.title.setTextColor(textColor);
                                                    itemHolder.artist.setTextColor(textColor);
                                                }
                                            }


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
                                        itemHolder.title.setTextColor(textColorPrimary);
                                        itemHolder.artist.setTextColor(textColorPrimary);
                                    }
                                }
                            }
                        });

                if (iSeaUtils.isLollipop()) {
                    itemHolder.albumArt.setTransitionName("transition_album_art" + i);
                }
                break;
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
            default:
                final NativeExpressAdViewHolder nativeExpressHolder = (NativeExpressAdViewHolder) viewHolder;
                UnifiedNativeAd unifiedNativeAd = (UnifiedNativeAd) arraylist.get(i);
                nativeExpressHolder.setContent(unifiedNativeAd);
                break;
        }

    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        // Logic for returning view type based on spaceBetweenAds variable
        // Here if remainder after dividing the position with (spaceBetweenAds + 1) comes equal to spaceBetweenAds,
        // then return NATIVE_EXPRESS_AD_VIEW_TYPE otherwise DATA_VIEW_TYPE
        // By the logic defined below, an ad unit will be showed after every spaceBetweenAds numbers of data items
        Object item = arraylist.get(position);
        if (item instanceof Album) {
            return DATA_VIEW_TYPE;
        }
        return NATIVE_EXPRESS_AD_VIEW_TYPE;
        //return (position % (spaceBetweenAds + 1) == spaceBetweenAds) ? NATIVE_EXPRESS_AD_VIEW_TYPE : DATA_VIEW_TYPE;
    }


    public void updateDataSet(List<Object> arraylist) {
        this.arraylist = arraylist;
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView title, artist;
        protected ImageView albumArt;
        protected View footer;

        public ItemHolder(View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.album_title);
            this.artist = (TextView) view.findViewById(R.id.album_artist);
            this.albumArt = (ImageView) view.findViewById(R.id.album_art);
            this.footer = view.findViewById(R.id.footer);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Album album = (Album) arraylist.get(getAdapterPosition());
            NavigationUtils.navigateToAlbum(mContext, album.id,
                    new Pair<View, String>(albumArt, "transition_album_art" + getAdapterPosition()));
        }

    }

    // View Holder for Admob Native Express Ad Unit
    public class NativeExpressAdViewHolder extends RecyclerView.ViewHolder {
        protected UnifiedNativeAdView templateView;
        protected TextView title, artist;
        protected ImageView albumArt;
        protected RatingBar ratingBar;
        protected View footer;

        NativeExpressAdViewHolder(View view) {
            super(view);
            this.templateView = (UnifiedNativeAdView) view.findViewById(R.id.template_ads);
            this.title = (TextView) view.findViewById(R.id.album_title);
            this.artist = (TextView) view.findViewById(R.id.album_artist);
            this.albumArt = (ImageView) view.findViewById(R.id.album_art);
            this.ratingBar = (RatingBar) view.findViewById(R.id.rating_bar);
            this.footer = view.findViewById(R.id.footer);
        }

        public void setContent(UnifiedNativeAd unifiedNativeAd) {
            templateView.setNativeAd(unifiedNativeAd);
            title.setText(unifiedNativeAd.getHeadline());
            artist.setText(unifiedNativeAd.getBody());
            albumArt.setImageDrawable(unifiedNativeAd.getIcon().getDrawable());
            templateView.setCallToActionView(templateView);
            Double starRating = unifiedNativeAd.getStarRating();
            if (starRating != null && starRating > 0) {
                ratingBar.setVisibility(VISIBLE);
                ratingBar.setRating(starRating.floatValue());
                ratingBar.setMax(5);
                templateView.setStarRatingView(ratingBar);
                artist.setVisibility(GONE);
            } else {
                ratingBar.setVisibility(GONE);
            }

            if (isGrid) {
                try {
                    Bitmap bitmap = ((BitmapDrawable) albumArt.getDrawable()).getBitmap();
                    new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            Palette.Swatch swatch = palette.getVibrantSwatch();
                            if (swatch != null) {
                                int color = swatch.getRgb();
                                footer.setBackgroundColor(color);
                                int textColor = iSeaUtils.getBlackWhiteColor(swatch.getTitleTextColor());
                                title.setTextColor(textColor);
                                artist.setTextColor(textColor);
                            } else {
                                Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                                if (mutedSwatch != null) {
                                    int color = mutedSwatch.getRgb();
                                    footer.setBackgroundColor(color);
                                    int textColor = iSeaUtils.getBlackWhiteColor(mutedSwatch.getTitleTextColor());
                                    title.setTextColor(textColor);
                                    artist.setTextColor(textColor);
                                }
                            }
                        }
                    });
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}



