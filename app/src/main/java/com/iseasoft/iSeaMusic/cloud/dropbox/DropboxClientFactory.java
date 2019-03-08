package com.iseasoft.iSeaMusic.cloud.dropbox;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import okhttp3.OkHttpClient;

/**
 * Created by xuanhien091 on 10/20/17.
 */

public class DropboxClientFactory {
    private static DbxClientV2 mDbxClient;
    private static OkHttpClient okHttpClient;

    public interface CallBack {
        void onRevoke();
    }

    public static void init(String accessToken) {
        if (mDbxClient == null) {
            okHttpClient = OkHttp3Requestor.defaultOkHttpClient();
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("tango")
                    .withHttpRequestor(new OkHttp3Requestor(okHttpClient))
                    .build();

            mDbxClient = new DbxClientV2(requestConfig, accessToken);
        }
    }

    public static DbxClientV2 getClient() {
        return mDbxClient;
    }

    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public static void revokeClient(final CallBack callBack) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mDbxClient.auth().tokenRevoke();
                } catch (DbxException e) {
                    Log.e("Dropbox", "Access Revoke Exception", e);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mDbxClient = null;
                if (callBack != null)
                    callBack.onRevoke();
            }
        }.execute();

    }
}
