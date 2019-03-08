package com.iseasoft.iSeaMusic.cloud.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.iseasoft.iSeaMusic.MusicService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ContentService extends BaseService
{
	//private final static Logger log = new Logger(ContentService.class.getSimpleName(), true);

	private static boolean sStarted = false;
	private static boolean sHidden = false;

	private final IBinder mBinder = new PlaybackServiceBinder();

	private boolean mSynchronizing = false;

	protected StorageObserverService mService;

	public static final String SYNC_STARTED_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_STARTED_ACTION";
	public static final String SYNC_STOPPED_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_STOPPED_ACTION";
	public static final String SYNC_TASK_STARTED_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_TASK_STARTED_ACTION";
	public static final String SYNC_TASK_STOPPED_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_TASK_STOPPED_ACTION";

	public static final String SYNC_MEDIASTORE_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_MEDIASTORE_ACTION";
	public static final String COMMIT_MEDIASTORE_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.COMMIT_MEDIASTORE_ACTION";
	public static final String SYNC_MDJ_LIBRARY_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.SYNC_MDJ_LIBRARY_ACTION";
	public static final String REFRESH_DB_INFO_ACTION = "com.iseasoft.iSeaMusic.cloud.services.ContentService.REFRESH_DB_INFO_ACTION";

	public static final String STARTED_SYNC_TYPE = "StartedSyncType";
	public static final String SYNC_RESULT_COUNT = "SyncResultCount";
	public static final int SYNC_TYPE_MEDIASTORE_NONE = 0;
	public static final int SYNC_TYPE_MEDIASTORE_SYNC = 1;
	public static final int SYNC_TYPE_MEDIASTORE_COMMIT = 2;
	public static final int SYNC_TYPE_MDJ_LIBRARY_SYNC = 3;
	public static final int SYNC_TYPE_DB_INFO_REFRESH = 4;

	public static Boolean isStarted(Context context)
	{
		if (sStarted)
		{
			//log.i("Service is running");
		}
		else
		{
			//log.i("Service is not running");
		}
		return sStarted;
	}

	public static boolean isHidden()
	{
		//log.d("isHidden:" + sHidden);
		return sHidden;
	}

	private static class SyncActionIntent
	{
		public SyncAction action;
		public Bundle extras;

		public SyncActionIntent(SyncAction action, Bundle extras)
		{
			this.action = action;
			this.extras = extras;
		}
	}

	private enum SyncAction
	{
		MEDIASTORE_SYNC(false),
		MEDIASTORE_COMMIT(true),
		MDJ_LIBRARY_SYNC(false),
		DB_INFO_REFRESH(true);

		private final boolean mHidden;

		private SyncAction(boolean hidden)
		{
			mHidden = hidden;
		}

		public boolean isHidden()
		{
			return mHidden;
		}
	}

	// 5 minutes timeout
	public static final long MAX_WAKELOCK_TIME = 300000;

	private final BlockingQueue<SyncActionIntent> mSyncQueue = new LinkedBlockingQueue<SyncActionIntent>();

	public class PlaybackServiceBinder extends Binder
	{
		public ContentService getService()
		{
			return ContentService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		sStarted = true;

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ContentService.SYNC_STARTED_ACTION));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		//log.i("onStartCommand");

		if (intent != null)
		{
			final String action = intent.getAction();

			try
			{
				if (action.equals(SYNC_MEDIASTORE_ACTION))
				{
					//log.d("Add sync MediaStore action");
					mSyncQueue.put(new SyncActionIntent(SyncAction.MEDIASTORE_SYNC, intent.getExtras()));
				}
				else if (action.equals(COMMIT_MEDIASTORE_ACTION))
				{
					//log.d("Add commit MediaStore action");
					mSyncQueue.put(new SyncActionIntent(SyncAction.MEDIASTORE_COMMIT, intent.getExtras()));
				}
				else if (action.equals(SYNC_MDJ_LIBRARY_ACTION))
				{
					//log.d("Add sync MDJ library action");
					mSyncQueue.put(new SyncActionIntent(SyncAction.MDJ_LIBRARY_SYNC, intent.getExtras()));
				}
				else if (action.equals(REFRESH_DB_INFO_ACTION))
				{
					//log.d("Add refresh DB info action");

					mSyncQueue.put(new SyncActionIntent(SyncAction.DB_INFO_REFRESH, intent.getExtras()));
				}
				else
				{
					return START_NOT_STICKY;
				}

				if (mService == null)
				{
					//log.d("binding");
					Intent service = new Intent(getApplicationContext(), StorageObserverService.class);
					service.putExtra(StorageObserverService.STOP, true);

					bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE);
				}
				else
				{
					mService.stopObservers();
					doNextActionSynchronized();
				}
			}
			catch (InterruptedException e)
			{
				//log.e(Log.getStackTraceString(e));
			}
		}
		else
		{
			//log.d("null intent");

			return START_NOT_STICKY;
		}
		return START_STICKY;
	}

	private void doNextActionSynchronized()
	{
		synchronized (this)
		{
			if (!mSynchronizing)
			{
				if (doNextAction())
					mSynchronizing = true;

			}
		}
	}

	private boolean doNextAction()
	{
		//log.i("doNextAction");

		SyncActionIntent actionInfo = mSyncQueue.poll();

		if (actionInfo == null)
		{
			//log.d("No task in queue");
			return false;
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(SYNC_TASK_STOPPED_ACTION);

		registerReceiverSave(mIntentReceiver, filter);

		sHidden = actionInfo.action.isHidden();
		Intent started_intent = new Intent(ContentService.SYNC_TASK_STARTED_ACTION);
		started_intent.putExtra(STARTED_SYNC_TYPE, syncActionToInt(actionInfo.action));
		LocalBroadcastManager.getInstance(this).sendBroadcast(started_intent);

		Intent intent;

		switch (actionInfo.action)
		{
			case MEDIASTORE_SYNC:
				//log.d("Starting MediaStoreSyncService");
				intent = new Intent(this, MusicService.class);
				break;
			default:
				return false;
		}

		Bundle extras = actionInfo.extras;
		if (extras != null)
			intent.putExtras(extras);

		startService(intent);

		return true;
	}

	private int syncActionToInt(SyncAction action)
	{
		switch (action)
		{
			case MEDIASTORE_SYNC:
				return SYNC_TYPE_MEDIASTORE_SYNC;
			case MEDIASTORE_COMMIT:
				return SYNC_TYPE_MEDIASTORE_COMMIT;
			case MDJ_LIBRARY_SYNC:
				return SYNC_TYPE_MDJ_LIBRARY_SYNC;
			case DB_INFO_REFRESH:
				return SYNC_TYPE_DB_INFO_REFRESH;
		}
		return SYNC_TYPE_MEDIASTORE_NONE;
	}

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (ContentService.SYNC_TASK_STOPPED_ACTION.equals(action))
			{

				//log.d("SYNC_TASK_STOPPED_ACTION received");

				//CacheHelper.clearCaches();
				// notify change of db file so it is copied to external storage
				//DbUtils.notifyDatabaseChange(context.getApplicationContext());

				synchronized (ContentService.this)
				{
					unregisterReceiverSave(mIntentReceiver);

					if (!doNextAction())
					{
						synchronized (this)
						{
							mSynchronizing = false;
						}
						stopSelf();
					}
				}
			}
			else
			{
				//log.e("Unknown action received");
			}
		}
	};

	@Override
	public void onDestroy()
	{
		//log.i("onDestroy");

		if (mService != null)
		{
			mService.startObservers();
			unbindService(mServiceConnection);
		}

		sStarted = false;

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ContentService.SYNC_STOPPED_ACTION));

		super.onDestroy();
	}

	public static void startSync(Context context, String action)
	{
		Intent contentService = new Intent(context.getApplicationContext(), ContentService.class);
		contentService.setAction(action);
		context.startService(contentService);
	}

	public static void startSync(Context context, String action, Bundle extras)
	{
		Intent contentService = new Intent(context.getApplicationContext(), ContentService.class);
		contentService.setAction(action);
		contentService.putExtras(extras);
		context.startService(contentService);
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mService = ((StorageObserverService.ServiceBinder) service).getService();

			if (mService != null)
			{
				mService.stopObservers();
			}

			doNextActionSynchronized();
		}
	};
}
