package com.iseasoft.iSeaMusic.fragments;

import android.support.v4.app.Fragment;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.iseasoft.iSeaMusic.adapters.AdsAdapter;

import java.util.List;

public class AdsFragment extends Fragment {
    protected final static int LIST_VIEW_ADS_COUNT = 10;
    protected final static int GRID_VIEW_ADS_COUNT = 5;
    protected int spaceBetweenAds;

    protected void generateDataSet(final AdsAdapter adapter) {
        AdLoader adLoader = new AdLoader.Builder(getActivity(), "/21617015150/407539/21858867742")
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        final List<Object> mDataSet = adapter.getDataSet();
                        for (int i = 2; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
                            adapter.getDataSet().add(i, unifiedNativeAd);
                            adapter.notifyItemRangeChanged(i, spaceBetweenAds);
                        }
                    }
                })
                .build();

        adLoader.loadAd(new PublisherAdRequest.Builder()
                .addTestDevice("FB536EF8C6F97686372A2C5A5AA24BC5").build());
    }
}
