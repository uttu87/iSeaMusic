package com.iseasoft.iSeaMusic.fragments;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.List;

public class AdsFragment extends Fragment {
    protected List<Object> mDataSet;
    protected int spaceBetweenAds;
    protected void generateDataSet(final RecyclerView.Adapter adapter) {
        for (int i = 2; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
            final int position = i;
            AdLoader adLoader = new AdLoader.Builder(getActivity(), "/21617015150/407539/21858867742")
                    .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                        @Override
                        public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                            mDataSet.add(position, unifiedNativeAd);
                            adapter.notifyItemRangeChanged(position, spaceBetweenAds);
                        }
                    })
                    .build();

            adLoader.loadAd(new PublisherAdRequest.Builder()
                    .addTestDevice("FB536EF8C6F97686372A2C5A5AA24BC5").build());
        }
    }
}
