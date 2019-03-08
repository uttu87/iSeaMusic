package com.iseasoft.iSeaMusic.cloud.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public abstract class BaseService extends Service
{
	//private static Logger log = new Logger(BaseService.class.getSimpleName(), true);

	public void registerReceiverSave(BroadcastReceiver receiver, IntentFilter filter)
	{
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
	}

	protected void unregisterReceiverSave(BroadcastReceiver receiver)
	{
		try
		{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		}
		catch (Exception e)
		{
			//log.w("Unable to unregister receiver: " + e.getMessage());
		}
	}

	public void registerGlobalReceiverSave(BroadcastReceiver receiver, IntentFilter filter)
	{
		registerReceiver(receiver, filter);
	}

	protected void unregisterGlobalReceiverSave(BroadcastReceiver receiver)
	{
		try
		{
			unregisterReceiver(receiver);
		}
		catch (Exception e)
		{
			//log.w("Unable to unregister receiver: " + e.getMessage());
		}
	}
}
