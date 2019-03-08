package com.iseasoft.iSeaMusic.cloud.dropbox;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.iseasoft.iSeaMusic.cloud.CloudBaseActivity;

/**
 * Created by xuanhien091 on 10/20/17.
 */

public abstract class DropboxBaseActivity extends CloudBaseActivity {
    public static final String ACCOUNT_PREFS_NAME = "dropbox_prefs";
    public static final String ACCESS_TOKEN = "access-token";
    public static final String STORE_UID = "store-uid";

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString(ACCESS_TOKEN, null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString(ACCESS_TOKEN, accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }

        String uid = Auth.getUid();
        String storedUid = prefs.getString(STORE_UID, null);
        if (uid != null && !uid.equals(storedUid)) {
            prefs.edit().putString(STORE_UID, uid).apply();
        }
    }

    private void initAndLoadData(String accessToken) {
        DropboxClientFactory.init(accessToken);
        loadData();
    }

    protected abstract void loadData();

    protected boolean hasToken() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        String accessToken = prefs.getString(ACCESS_TOKEN, null);
        return accessToken != null;
    }

    protected void clearAccessToken(DropboxClientFactory.CallBack callback) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(ACCESS_TOKEN).commit();
        prefs.edit().remove(STORE_UID).commit();
        AuthActivity.result = null;
        DropboxClientFactory.revokeClient(callback);
    }
}
