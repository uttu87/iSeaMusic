package com.iseasoft.iSeaMusic.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetUtil
{
	public static boolean hasNetworkConnection(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		return netinfo != null && netinfo.isConnected();
	}
}
