package com.iseasoft.iSeaMusic.cloud.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.iseasoft.iSeaMusic.MusicService;
import com.iseasoft.iSeaMusic.activities.BaseActivity;
import com.iseasoft.iSeaMusic.utils.Storage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class StorageObserverService extends BaseService {
    //private static final Logger log = new Logger(StorageObserverService.class.getSimpleName(), true);

    public static final String STOP = "stop";

    private static final int MIN_DELAY = 1;
    private static final int MAX_DELAY = 2;

    private static final long IDLE_DELAY = 60000;
    private static final long DATABASE_COPY_MIN_DELAY = 10000;
    private static final long DATABASE_COPY_MAX_DELAY = 60000;

    public static final String PUBLISH_DATABASE_ACTION = "jp.co.mytrax.traxcore.storage.StorageObserverService.PUBLISH_DATABASE_ACTION";

    List<StorageObserver> mObservers = new ArrayList<StorageObserver>();
    private boolean mUsbSyncRunning = false;
    private boolean mIsUsbPlugged = false;

    private final IBinder mBinder = new ServiceBinder();

    private static Boolean sStarted = false;
    private int mServiceStartId = -1;
    private Handler mDelayedStopHandler;
    private Handler mDelayedDatabaseCopyHandler;

    private static class DelayedStopHandler extends Handler {
        private final WeakReference<StorageObserverService> mService;

        public DelayedStopHandler(StorageObserverService service) {
            mService = new WeakReference<StorageObserverService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mService == null || mService.get() == null)
                return;

            if (mService.get().mUsbSyncRunning) {
                //log.d("USB sync is running, postpone stopping");
                mService.get().stopServiceDelayed();
            } else {
                //log.d("Delayed stop of StorageObserverService");
                mService.get().stopSelf(mService.get().mServiceStartId);
            }
        }
    }

    ;

    private static class DelayedDatabaseCopyHandler extends Handler {
        private final WeakReference<StorageObserverService> mService;

        public DelayedDatabaseCopyHandler(StorageObserverService service) {
            mService = new WeakReference<StorageObserverService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mService == null || mService.get() == null)
                return;

            removeMessages(MIN_DELAY);
            removeMessages(MAX_DELAY);
        }
    }

    ;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //log.d("onStartCommand");

        if (Storage.isMainStorageAvailable(this)) {
            mServiceStartId = startId;
            clearObservers();

            if (createObservers()) {
                if (!ContentService.isStarted(this)) {
                    startObservers();
                }

                return START_STICKY;
            }
        } else {
            //log.d("Storages not available. Stop service.");
        }

        return START_NOT_STICKY;
    }

    public void stopServiceDelayed() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    private void cancelStopService() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
    }

    public static Boolean isStarted() {

        if (sStarted) {
            //log.i("Service is running");
        } else {
            //log.i("Service is not running");
        }
        return sStarted;
    }

    public void startObservers() {
        //log.d("Start observers");
        for (StorageObserver o : mObservers) {
            o.start();
        }
    }

    private boolean createObservers() {
        boolean res = false;

        //mObservers.add(new MusicService.MediaStoreObserver(mDelayedStopHandler));

        return res;
    }

    public void onUsbSyncStarted() {
        mUsbSyncRunning = true;

        for (StorageObserver o : mObservers) {
            o.onUsbSyncStarted();
        }
    }

    public void onUsbSyncFinished() {
        mUsbSyncRunning = false;

        for (StorageObserver o : mObservers) {
            o.onUsbSyncFinished();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //log.d("onCreate");
        sStarted = true;

        mDelayedStopHandler = new DelayedStopHandler(this);
        mDelayedDatabaseCopyHandler = new DelayedDatabaseCopyHandler(this);

        IntentFilter filter = new IntentFilter();

        //filter.addAction(PlaybackService.PLAYBACK_DESTROYED);
        filter.addAction(BaseActivity.APP_GO_TO_FOREGROUND);
        filter.addAction(BaseActivity.APP_GO_TO_BACKGROUND);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(StorageObserverService.PUBLISH_DATABASE_ACTION);

        registerReceiverSave(mIntentReceiver, filter);
    }

    @Override
    public void onDestroy() {
        //log.d("onDestroy");

        clearObservers();

        unregisterReceiverSave(mIntentReceiver);

        sStarted = false;
        super.onDestroy();
    }

    protected void clearObservers() {
        stopObservers();

        mObservers.clear();
    }

    public void stopObservers() {
        //log.d("Stop observers");

        for (StorageObserver o : mObservers) {
            o.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

//			if (PlaybackService.PLAYBACK_DESTROYED.equals(action))
//			{
//				log.d("PLAYBACK_DESTROYED received");
//
////				stopSelf();
//			}
//			else
            if (BaseActivity.APP_GO_TO_FOREGROUND.equals(action)) {
                //log.d("APP_GO_TO_FOREGROUND received");
                cancelStopService();
            } else if (BaseActivity.APP_GO_TO_BACKGROUND.equals(action)) {
                //log.d("APP_GO_TO_BACKGROUND received");
                stopServiceDelayed();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                // power plug extra is stored in intent
                if (plugged > 0) {
                    boolean isPlugged = (plugged & BatteryManager.BATTERY_PLUGGED_USB) > 0;

                    // USB plug state changed
                    if (isPlugged != mIsUsbPlugged) {
                        mIsUsbPlugged = isPlugged;
                    }
                }
            } else if (StorageObserverService.PUBLISH_DATABASE_ACTION.equals(action)) {
                if (mIsUsbPlugged) {
                    mDelayedDatabaseCopyHandler.removeMessages(MIN_DELAY);
                    mDelayedDatabaseCopyHandler.sendEmptyMessageDelayed(MIN_DELAY, DATABASE_COPY_MIN_DELAY);

                    if (!mDelayedDatabaseCopyHandler.hasMessages(MAX_DELAY)) {
                        mDelayedDatabaseCopyHandler.sendEmptyMessageDelayed(MAX_DELAY, DATABASE_COPY_MAX_DELAY);
                    }
                }
            } else {
                //log.e("Unknown action received");
            }
        }

    };

    public class ServiceBinder extends Binder {
        public StorageObserverService getService() {
            return StorageObserverService.this;
        }
    }

    public interface StorageObserver {
        public void start();

        public void onUsbSyncStarted();

        public void onUsbSyncFinished();

        public void stop();
    }
}
