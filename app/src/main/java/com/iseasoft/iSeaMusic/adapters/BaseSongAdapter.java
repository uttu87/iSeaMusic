package com.iseasoft.iSeaMusic.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.gms.cast.framework.CastSession;
import com.iseasoft.iSeaMusic.MusicPlayer;
import com.iseasoft.iSeaMusic.activities.BaseActivity;
import com.iseasoft.iSeaMusic.cast.iSeaMusicCastHelper;
import com.iseasoft.iSeaMusic.models.Song;
import com.iseasoft.iSeaMusic.utils.NavigationUtils;
import com.iseasoft.iSeaMusic.utils.iSeaUtils;

import java.util.List;

/**
 * Created by naman on 7/12/17.
 */

public class BaseSongAdapter extends AdsAdapter {

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        public ItemHolder(View view) {
            super(view);
        }

    }

    public void playAll(final Activity context, final long[] list, int position,
                        final long sourceId, final iSeaUtils.IdType sourceType,
                        final boolean forceShuffle, final Song currentSong, boolean navigateNowPlaying) {

        if (context instanceof BaseActivity) {
            CastSession castSession = ((BaseActivity) context).getCastSession();
            if (castSession != null) {
                navigateNowPlaying = false;
                iSeaMusicCastHelper.startCasting(castSession, currentSong);
            } else {
                MusicPlayer.playAll(context, list, position, -1, iSeaUtils.IdType.NA, false);
            }
        } else {
            MusicPlayer.playAll(context, list, position, -1, iSeaUtils.IdType.NA, false);
        }

        if (navigateNowPlaying) {
            NavigationUtils.navigateToNowplaying(context, true);
        }


    }
    public void removeSongAt(int i){}
    public void updateDataSet(List<Song> arraylist) {}

}
