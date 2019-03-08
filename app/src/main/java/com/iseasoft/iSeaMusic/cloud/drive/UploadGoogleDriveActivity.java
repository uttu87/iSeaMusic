package com.iseasoft.iSeaMusic.cloud.drive;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.services.drive.Drive;
import com.tsutaya.musicplayer.R;
import com.tsutaya.musicplayer.cloud.drive.UploadFileToDrive.IUploadCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import jp.co.mytrax.traxcore.db.dao.MediaDao;
import jp.co.mytrax.traxcore.db.domain.Media;
import jp.co.mytrax.traxcore.db.store.MediaStore.ItemType;
import jp.co.mytrax.traxcore.mdj.MdjDecoratedMultiImageView;
import jp.co.mytrax.util.NetUtil;

public class UploadGoogleDriveActivity extends Activity
{

	private ListView mListView;
	private Hashtable<Media, Integer> mMediaProgress;
	private Hashtable<Media, UploadFileToDrive> mUploadingFile;
	private ArrayList<Media> mFilesToUpload;
	private ArrayList<Media> mFailUploadFiles;
	private ArrayMediaAdapter mediaAdapter;

	private TextView mLeftBarItem;
	private TextView mRightBarItem;

	// state of buttons on action bar
	private final int UPLOAD_CANCEL = 0;
	private final int UPLOAD_REUPLOAD = 1;
	private final int UPLOAD_NONE = 2;
	private final int UPLOAD_DONE = 3;
	private final int UPLOAD_PAUSE = 4;
	private final int UPLOAD_RETRY = 5;
	private int mButtonRightState = 0;
	private int mButtonLeftState = 0;

	private String mParentId;
	protected ProgressDialog mProgressDlg;
	public static Drive mService;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_google_drive);

		// get parent Id
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras().getBundle(GoogleDriveActivity.EXTRA_PARENTID_KEY);
		mParentId = bundle.getString(GoogleDriveActivity.BUNDLE_PARENTID_KEY);

		mButtonLeftState = UPLOAD_CANCEL;
		mButtonRightState = UPLOAD_PAUSE;

		mLeftBarItem = (TextView) findViewById(R.id.bar_left_item);
		mRightBarItem = (TextView) findViewById(R.id.bar_right_item);
		setOnBarItemsClickListener();

		mMediaProgress = new Hashtable<Media, Integer>();
		mUploadingFile = new Hashtable<Media, UploadFileToDrive>();
		mFilesToUpload = new ArrayList<Media>();
		mFailUploadFiles = new ArrayList<Media>();
		mListView = (ListView) findViewById(R.id.actual_list_view);

		loadSongsAsync();
	}

	/**
	 * do load songs async
	 */
	private void loadSongsAsync()
	{
		new AsyncTask<Object, List<Media>, List<Media>>()
		{
			@Override
			protected void onPreExecute()
			{
				// show progress bar
				mProgressDlg = new ProgressDialog(UploadGoogleDriveActivity.this);
				mProgressDlg.setCancelable(false);
				mProgressDlg.getWindow().setGravity(Gravity.CENTER);
				mProgressDlg.show();
			}

			@Override
			protected List<Media> doInBackground(Object... params)
			{
				List<Media> medias = new ArrayList<Media>();
				List<Media> songs = getSongs();
				if (songs != null && songs.size() > 0)
				{
					// create Upload Queue
					for (int i = 0; i < songs.size(); i++)
					{
						Media media = songs.get(i);
						if (isFileExist(media.getData()))
						{
							medias.add(media);
							createUploadAsync(media);
						}
					}

				}
				return medias;
			}

			@Override
			protected void onPostExecute(List<Media> medias)
			{
				if (mProgressDlg != null)
				{
					mProgressDlg.dismiss();
				}
				if (medias != null && medias.size() > 0)
				{
					mediaAdapter = new ArrayMediaAdapter(UploadGoogleDriveActivity.this, R.layout.listitem_media, createUploadData(medias));
					mListView.setAdapter(mediaAdapter);
					startUpload();
				}
				else
				{
					TextView txtSMS = (TextView) findViewById(R.id.txtSMS);
					txtSMS.setVisibility(View.VISIBLE);
					txtSMS.setText(getString(R.string.cloud_data_is_empty));
					txtSMS.setTextColor(Color.BLACK);
					mButtonLeftState = UPLOAD_DONE;
					mButtonRightState = UPLOAD_NONE;
					refreshActionBar();
				}
			};

		}.execute();
	}

	private List<UploadData> createUploadData(List<Media> medias)
	{
		List<UploadData> uploadList = new ArrayList<UploadData>();
		if (medias != null)
			for (int i = 0; i < medias.size(); i++)
				uploadList.add(new UploadData(medias.get(i), UploadData.UPLOAD_INQUEU));
		return uploadList;
	}

	private void runNotifyDataSetChanged()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				mediaAdapter.notifyDataSetChanged();
			}
		});
	}

	private void pauseUploadFile()
	{

		if (mFilesToUpload != null && mFilesToUpload.size() > 0)
		{
			Media media = mFilesToUpload.get(0);
			UploadFileToDrive currentUpload = mUploadingFile.get(media);
			if (!currentUpload.isCancelled())
			{
				currentUpload.pauseUpload();
				mediaAdapter.updateData(media, UploadData.UPLOAD_INQUEU);
				runNotifyDataSetChanged();
			}

		}

	}

	private void retryUploadFile()
	{
		mUploadingFile.clear();
		mFilesToUpload.clear();

		for (int i = 0; i < mFailUploadFiles.size(); i++)
		{
			Media media = mFailUploadFiles.get(i);
			// set normal state for fail upload files
			mediaAdapter.updateData(media, UploadData.UPLOAD_INQUEU);
			createUploadAsync(media);
		}
		mFailUploadFiles.clear();
		startUpload();
		runNotifyDataSetChanged();
	}

	private void restartUpload()
	{
		List<Media> mUplloadTempList = new ArrayList<Media>(mFilesToUpload);
		mUploadingFile.clear();
		mFilesToUpload.clear();
		for (int i = 0; i < mUplloadTempList.size(); i++)
		{
			Media media = mUplloadTempList.get(i);
			createUploadAsync(media);
		}
		startUpload();
	}

	private void cancelUploadingAllMediasConfirmDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(UploadGoogleDriveActivity.this);
		builder.setTitle(getString(R.string.confirm_cancel_uploading_all_items));
		builder.setNegativeButton(getString(R.string.cloud_confirm_cancel_all_coninue_upload), new DialogInterface.OnClickListener()

		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});

		builder.setPositiveButton(getString(R.string.cloud_confirm_cancel_all_cancel), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				stopUploadAllMedias(false);
				finish();
			}
		});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		return super.onContextItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		if (mButtonLeftState == UPLOAD_CANCEL)
			cancelUploadingAllMediasConfirmDialog();
		else
			finish();
	}

	/*
	 * stop a media media: is Media object
	 */
	private void stopUploadMedia(Media media)
	{

		UploadFileToDrive upload = mUploadingFile.get(media);
		if (upload != null)
		{
			upload.stop();
		}

		// store fail uploaded media
		mFailUploadFiles.add(media);
		mUploadingFile.remove(media);
		mFilesToUpload.remove(media);

		// update data state
		mediaAdapter.updateData(media, UploadData.UPLOAD_FAILED);
		runNotifyDataSetChanged();
		// upload a next file
		startUpload();
	}

	/**
	 * reasonFromError true => when network is error | false => user want to stop all items
	 * */
	private void stopUploadAllMedias(boolean reasonFromError)
	{
		int state = UploadData.UPLOAD_INQUEU;
		if (reasonFromError)
		{
			state = UploadData.UPLOAD_FAILED;
			mButtonRightState = UPLOAD_RETRY;
		}

		for (int i = 0; i < mFilesToUpload.size(); i++)
		{
			Media media = mFilesToUpload.get(i);
			UploadFileToDrive upload = mUploadingFile.get(media);
			if (upload != null)
			{
				upload.cancel(true);
				if (reasonFromError)
					mFailUploadFiles.add(media);
				// update data state
				mediaAdapter.updateData(media, state);
				runNotifyDataSetChanged();
			}
		}

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				refreshActionBar();
			}
		});
	}

	private void startUpload()
	{
		if (!NetUtil.hasNetworkConnection(UploadGoogleDriveActivity.this))
		{
			stopUploadAllMedias(true);
			return;
		}

		Media media = null;

		// get a existed media
		do
		{
			if (mUploadingFile != null && mUploadingFile.size() > 0 && mFilesToUpload != null && mFilesToUpload.size() > 0)
			{
				media = mFilesToUpload.get(0);
				if (!isFileExist(media.getData()))
				{
					mUploadingFile.remove(media);
					mFilesToUpload.remove(media);
					mFailUploadFiles.add(media);
					mediaAdapter.updateData(media, UploadData.UPLOAD_FAILED);
					runNotifyDataSetChanged();
					media = null;
				}
			}
			else
				break;
		} while (media == null);

		if (media != null)
		{
			UploadFileToDrive upload = mUploadingFile.get(media);
			if (upload != null && upload.getStatus() != AsyncTask.Status.RUNNING)
				upload.execute();
		}
		else
		{

			if (mFailUploadFiles != null && mFailUploadFiles.size() > 0)
			{
				mButtonRightState = UPLOAD_RETRY;
			}
			else
			{
				mButtonLeftState = UPLOAD_DONE;
				mButtonRightState = UPLOAD_NONE;
			}

			refreshActionBar();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		setTitle(getString(R.string.cloud_dropbox));
	}

	private List<Media> getSongs()
	{
		List<Media> medias = MediaDao.loadAllDownloadFiles(getApplicationContext(), ItemType.MUSIC);
		return medias;
	}

	public class ArrayMediaAdapter extends BaseAdapter
	{
		Context context;
		List<UploadData> mMedias;
		private final LayoutInflater mLayoutInflater;

		private ViewHolder mHolder;

		public ArrayMediaAdapter(Context context, int textViewResourceId, List<UploadData> medias)
		{
			this.context = context;
			mMedias = medias;
			mLayoutInflater = (LayoutInflater.from(context));
		}

		public void updateData(Media song, int status)
		{
			if (mMedias != null)
				for (UploadData item : mMedias)
					if (item.mMedia.equals(song))
					{
						item.mUploadResult = status;
						break;
					}
		}

		@Override
		public boolean isEnabled(int position)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getCount()
		{
			return mMedias != null ? mMedias.size() : 0;
		}

		@Override
		public Object getItem(int position)
		{
			return mMedias.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;

			if (rowView == null)
			{
				mHolder = new ViewHolder();
				rowView = mLayoutInflater.inflate(R.layout.listitem_media, parent, false);

				mHolder.mTtitle = (TextView) rowView.findViewById(R.id.title);
				mHolder.mArtist = (TextView) rowView.findViewById(R.id.details);
				mHolder.mIcon = (MdjDecoratedMultiImageView) rowView.findViewById(R.id.icon);

				mHolder.mProgress = (ProgressBar) rowView.findViewById(R.id.progressUpload);
				mHolder.mPercent = (TextView) rowView.findViewById(R.id.textProgressUpload);
				mHolder.mProgress.setVisibility(View.GONE);
				mHolder.mPercent.setVisibility(View.GONE);

				mHolder.mIVDone = (ImageView) rowView.findViewById(R.id.img_upload_done);
				mHolder.mIVFail = (ImageView) rowView.findViewById(R.id.img_upload_fail);
				rowView.setTag(mHolder);
			}
			else
			{
				mHolder = (ViewHolder) rowView.getTag();
			}

			Media media = mMedias.get(position).mMedia;
			mHolder.mTtitle.setText(media.getTitle());
			mHolder.mArtist.setText(media.getArtists());
			mHolder.mIcon.setImageDrawable(media.getAlbumArt());

			Integer status = mMediaProgress.get(media);
			if (mMedias.get(position).mUploadResult == UploadData.UPLOAD_INQUEU)
			{
				mHolder.mArtist.setVisibility(View.VISIBLE);
				mHolder.mIVDone.setVisibility(View.GONE);
				mHolder.mIVFail.setVisibility(View.GONE);
				mHolder.mPercent.setVisibility(View.GONE);
				mHolder.mProgress.setVisibility(View.GONE);
			}
			else if (mMedias.get(position).mUploadResult == UploadData.UPLOAD_FAILED)
			{ // upload is failed
				mHolder.mArtist.setVisibility(View.VISIBLE);
				mHolder.mIVDone.setVisibility(View.GONE);
				mHolder.mIVFail.setVisibility(View.VISIBLE);

				mHolder.mPercent.setVisibility(View.GONE);
				mHolder.mProgress.setVisibility(View.GONE);

			}
			else if (mMedias.get(position).mUploadResult == UploadData.UPLOAD_OK)
			{// upload is done
				mHolder.mArtist.setVisibility(View.VISIBLE);
				mHolder.mIVDone.setVisibility(View.VISIBLE);
				mHolder.mIVFail.setVisibility(View.GONE);

				mHolder.mPercent.setVisibility(View.GONE);
				mHolder.mProgress.setVisibility(View.GONE);
			}
			else
			{ // uploading
				mHolder.mArtist.setVisibility(View.GONE);
				mHolder.mIVDone.setVisibility(View.GONE);
				mHolder.mIVFail.setVisibility(View.GONE);

				mHolder.mPercent.setVisibility(View.VISIBLE);
				mHolder.mProgress.setVisibility(View.VISIBLE);
				mHolder.mProgress.setProgress(status);
				mHolder.mPercent.setText(status + "%");
			}
			return rowView;
		}

	}

	private class ViewHolder
	{
		private TextView mTtitle;
		private TextView mArtist;
		private MdjDecoratedMultiImageView mIcon;
		private ProgressBar mProgress;
		private TextView mPercent;
		public ImageView mIVDone;
		public ImageView mIVFail;
	}

	private void finishUpload(Media media)
	{
		mMediaProgress.put(media, 100);
		mUploadingFile.remove(media);
		mFilesToUpload.remove(media);
		mediaAdapter.updateData(media, UploadData.UPLOAD_OK);
		runNotifyDataSetChanged();
		startUpload();
	}

	private void createUploadAsync(Media currentMedia)
	{
		mMediaProgress.put(currentMedia, 0);
		UploadFileToDrive upload = new UploadFileToDrive(mService, currentMedia, mParentId);
		mUploadingFile.put(currentMedia, upload);
		mFilesToUpload.add(currentMedia);
		upload.setUploadCallback(new IUploadCallback()
		{

			@Override
			public void onUploadProgress(Media media, int percent)
			{
				if (percent >= 0 && percent <= 100)
				{
					mediaAdapter.updateData(media, UploadData.UPLOAD_INPROGRESS);
					mMediaProgress.put(media, percent);
					mediaAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onFinishUpload(Media media)
			{
				finishUpload(media);
			}

			@Override
			public void onErrorUpload(Media media, Exception e)
			{
				stopUploadMedia(media);
			}
		});
	}

	class UploadData
	{
		public Media mMedia;
		public int mUploadResult = 0; // 0: in queue, 2: failed, 1: ok
		public static final int UPLOAD_INQUEU = 0;
		public static final int UPLOAD_OK = 1;
		public static final int UPLOAD_FAILED = 2;
		public static final int UPLOAD_INPROGRESS = 3;

		public UploadData(Media media, int uploadResult)
		{
			mMedia = media;
			mUploadResult = uploadResult;
		}
	}

	private boolean isFileExist(String path)
	{
		File file = new File(path);
		return file.exists();
	}

	private void setOnBarItemsClickListener()
	{
		mLeftBarItem.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		mRightBarItem.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mButtonRightState == UPLOAD_PAUSE)
				{
					mButtonRightState = UPLOAD_REUPLOAD;
					pauseUploadFile();
				}
				else if (mButtonRightState == UPLOAD_REUPLOAD)
				{
					mButtonRightState = UPLOAD_PAUSE;
					refreshActionBar();
					restartUpload();
				}
				else if (mButtonRightState == UPLOAD_RETRY)
				{
					mButtonRightState = UPLOAD_PAUSE;
					retryUploadFile();
				}
				refreshActionBar();
			}
		});
	}

	private void refreshActionBar()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (mButtonLeftState == UPLOAD_CANCEL)
					mLeftBarItem.setText(R.string.cancel);
				else if (mButtonLeftState == UPLOAD_DONE)
					mLeftBarItem.setText(R.string.done);

				if (mButtonRightState == UPLOAD_PAUSE)
					mRightBarItem.setText(R.string.pause_upload);
				else if (mButtonRightState == UPLOAD_REUPLOAD)
					mRightBarItem.setText(R.string.resume);
				else if (mButtonRightState == UPLOAD_RETRY)
					mRightBarItem.setText(R.string.retry);
				else
					mRightBarItem.setVisibility(View.INVISIBLE);
			}
		});
	}
}
