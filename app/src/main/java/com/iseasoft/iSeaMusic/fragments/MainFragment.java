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

package com.iseasoft.iSeaMusic.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.iseasoft.iSeaMusic.R;
import com.iseasoft.iSeaMusic.utils.ATEUtils;
import com.iseasoft.iSeaMusic.utils.Helpers;
import com.iseasoft.iSeaMusic.utils.PreferencesUtility;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    private PreferencesUtility mPreferences;
    private ViewPager viewPager;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferencesUtility.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_main, container, false);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        final ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);


        viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        if (viewPager != null) {
            setupViewPager(viewPager);
            viewPager.setOffscreenPageLimit(2);
        }

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        return rootView;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("dark_theme", false)) {
            ATE.apply(this, "dark_theme");
        } else {
            ATE.apply(this, "light_theme");
        }
        viewPager.setCurrentItem(mPreferences.getStartPageIndex());
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getChildFragmentManager());
        adapter.addFragment(new DiscoverFragment(), "Discover");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_MUSIC_ID), "Music");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_CHILDREN_MUSIC_ID), "Children");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_CHRISTIAN_MUSIC_ID), "Christian Music");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_COUNTRY_MUSIC_ID), "Country");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_CLASSICAL_MUSIC_ID), "Classical");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_JAZZ_MUSIC_ID), "Jazz");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_ELECTRONIC_MUSIC_ID), "Electronic");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_HIPHOP_MUSIC_ID), "Hip Hop");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_INDEPENDENT_MUSIC_ID), "Independent");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_ASIA_MUSIC_ID), "Music of Asia");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_LATIN_MUSIC_ID), "Latin America");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_POP_MUSIC_ID), "POP");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_REGGAE_MUSIC_ID), "Reggae");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_RHYTHM_BLUES_MUSIC_ID), "Rhythm and Blues");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_ROCK_MUSIC_ID), "Rock");
//        adapter.addFragment(MusicFragment.newInstance(Constants.TOPIC_SOUL_MUSIC_ID), "Soul");
        adapter.addFragment(new SongsFragment(), this.getString(R.string.songs));
        adapter.addFragment(new AlbumFragment(), this.getString(R.string.albums));
        adapter.addFragment(new ArtistFragment(), this.getString(R.string.artists));
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPreferences.lastOpenedIsStartPagePreference()) {
            mPreferences.setStartPageIndex(viewPager.getCurrentItem());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String ateKey = Helpers.getATEKey(getActivity());
        ATEUtils.setStatusBarColor(getActivity(), ateKey, Config.primaryColor(getActivity(), ateKey));

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
