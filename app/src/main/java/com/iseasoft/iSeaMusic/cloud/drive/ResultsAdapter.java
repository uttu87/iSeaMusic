package com.iseasoft.iSeaMusic.cloud.drive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.tsutaya.musicplayer.R;
import com.tsutaya.musicplayer.cloud.CloudBaseActivity;
import com.tsutaya.musicplayer.cloud.interfaces.LibraryAdapter;
import com.tsutaya.musicplayer.cloud.interfaces.ResultAdapterCallback;
import com.tsutaya.musicplayer.cloud.utils.Cloud;
import com.tsutaya.musicplayer.cloud.utils.Limiter;
import com.tsutaya.musicplayer.cloud.utils.MediaUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResultsAdapter extends BaseAdapter implements LibraryAdapter
{
	private final LayoutInflater mInflater;
	private Limiter mLimiter;
	boolean mLinkedWithDropbox;
	private List<Object> mCloudFiles;
	private Context mContext = null;
	private ResultAdapterCallback mCallback;
	private boolean mIsRoot = false;
	private Cloud mType;


	public ResultsAdapter(Context context, Limiter limiter, ResultAdapterCallback callback) {
		this.mLimiter = limiter;
		this.mContext = context;
		this.mCallback = callback;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		validateDropbox();
		mType = Cloud.DROPBOX;
	}

	public ResultsAdapter(Context context, ResultAdapterCallback callback) {
		mContext = context;
		mCallback = callback;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLinkedWithDropbox = true;
		mType = Cloud.GDRIVE;
	}

	public boolean getIsRoot() {
		return mIsRoot;
	}

	public void setIsRoot(boolean isRoot) {
		mIsRoot = isRoot;
	}

	public void setResultDate(List<Object> resultList) {
		mCloudFiles = resultList;
	}

	@Override
	public int getCount() {
		if (!mLinkedWithDropbox) {
			return 1;
		}
		if (mCloudFiles != null) {
			return mType == Cloud.GDRIVE? mCloudFiles.size() : mCloudFiles.size() + 1;
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		return mLinkedWithDropbox ? mCloudFiles.get(position) : null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("NewApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (!mLinkedWithDropbox) {
			View view = mInflater.inflate(R.layout.library_row_link_with_dropbox, null);
			TextView textView = (TextView) view.findViewById(R.id.text);
			textView.setText(mContext.getResources().getString(R.string.link_dropbox));
			return view;
		} else {
			if (mCloudFiles != null) {
				final ViewHolder holder;
				switch (mType) {
					case DROPBOX:
						View view;
						view = mInflater.inflate(R.layout.library_row_orchid, null);
						holder = new ViewHolder();
						holder.name = (TextView) view.findViewById(R.id.tvName);
						holder.size = (TextView) view.findViewById(R.id.tvSize);
						holder.icon = (ImageView) view.findViewById(R.id.imgIcon);
						holder.option = (ImageView) view.findViewById(R.id.imgOption);
						view.setTag(holder);
						if (position == 0) {
							holder.icon.setImageResource(R.drawable.ic_clould_folder);
							holder.name.setText("...");
							holder.size.setText("");
							holder.id = LibraryAdapter.ID_LINK_TO_PARENT_DIR;
							holder.option.setVisibility(View.INVISIBLE);
							view.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									mCallback.onRootClicked();
								}
							});
						} else {
							final Metadata item = (Metadata) mCloudFiles.get(position - 1);
							String fileName = item.getName();
							holder.id = position - 1;
							holder.name.setText(fileName);
							if (item instanceof FolderMetadata) {
								holder.icon.setImageResource(R.drawable.ic_clould_folder);
								holder.size.setText("");
								holder.option.setVisibility(View.INVISIBLE);
							} else if (item instanceof FileMetadata) {
								FileMetadata f = (FileMetadata) item;
								holder.icon.setImageResource(R.drawable.ic_clould_media);
								holder.size.setText(MediaUtils.calculateProperFileSize(f.getSize()));
								holder.option.setVisibility(View.VISIBLE);
							}

							holder.option.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									showPopupMenu(holder.option, item);
								}
							});
							view.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									if (item instanceof FolderMetadata) {
										mCallback.onFolderClicked(holder.id, (FolderMetadata) item);
									} else if (item instanceof FileMetadata) {
										mCallback.onFileClicked(holder.id, (FileMetadata) item);
									}
								}
							});
						}
						return view;
					case GDRIVE:
						final com.google.api.services.drive.model.File file;
						if (mCloudFiles != null && mCloudFiles.size() > position) {
							file = (com.google.api.services.drive.model.File) mCloudFiles.get(position);
						} else {
							file = null;
						}

						if (convertView == null) {
							holder = new ViewHolder();
							convertView = mInflater.inflate(R.layout.row_file, null);
							holder.name = (TextView) convertView.findViewById(R.id.tvName);
							holder.modify = (TextView) convertView.findViewById(R.id.tvModify);
							holder.size = (TextView) convertView.findViewById(R.id.tvSize);
							holder.icon = (ImageView) convertView.findViewById(R.id.imgIcon);
							holder.option = (ImageView) convertView.findViewById(R.id.imgOption);
							convertView.setTag(holder);
						} else {
							holder = (ViewHolder) convertView.getTag();
						}
						if (position == 0 && !getIsRoot()) {
							holder.icon.setImageResource(R.drawable.ic_clould_folder);
							holder.name.setText("...");
							holder.modify.setText("");
							holder.size.setText("");
							holder.option.setVisibility(View.INVISIBLE);
						} else {
							if (null != file) {
								holder.name.setAlpha(0.8f);
								holder.modify.setText("Modified: " + getDatePassMilliseconds(file.getModifiedDate().getValue()));
								holder.name.setText(file.getTitle());

								if (file.getFileExtension() == null) {
									holder.icon.setImageResource(R.drawable.ic_clould_folder);
									holder.size.setText("");
									holder.option.setVisibility(View.INVISIBLE);
								} else if (file.getFileExtension() != null && (file.getFileExtension().equals("mp3") || file.getFileExtension().equals("m4a"))) {
									holder.icon.setImageResource(R.drawable.ic_clould_media);
									String fileSize = getFileSize(file.getFileSize());
									holder.size.setText(fileSize);
									holder.option.setVisibility(View.VISIBLE);
								} else {
									holder.icon.setImageResource(R.drawable.ic_clould_media);
									String fileSize = getFileSize(file.getFileSize());
									holder.size.setText(fileSize);
									holder.option.setVisibility(View.VISIBLE);
								}
							}
						}

						holder.option.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								showPopupMenu(holder.option, file);
							}
						});

						return convertView;
				}
			}
		}
		return convertView;
	}

	private void showPopupMenu(final View v, final Object dbFile) {
		v.post(new Runnable() {

			@SuppressLint("NewApi")
			@Override
			public void run() {
				PopupMenu popup = new PopupMenu(mContext, v);
				int menuId = R.menu.popup_cloud;
				popup.getMenuInflater().inflate(menuId, popup.getMenu());
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						return onPopupMenuItemClick(menuItem, dbFile);
					}
				});
				popup.show();
			}
		});
	}

	public boolean onPopupMenuItemClick(MenuItem item, Object object) {

		final int id = item.getItemId();
		switch (id) {
			case R.id.menu_download_file:
				mCallback.onDownloadFileCallback(object);
				return true;
			default:
				return false;
		}
	}


	private static class ViewHolder {
		public int id;
		public TextView name;
		public ImageView icon;
		public TextView size;
		public ImageView option;
		public TextView modify;
	}

	@Override
	public int getMediaType() {
		return 0;
	}

	@Override
	public void setLimiter(Limiter limiter) {
		mLimiter = limiter;
	}

	@Override
	public Limiter getLimiter() {
		return mLimiter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void comitQuery(Object data) {
		if (data == null) {
			return;
		}
		mCloudFiles = (List<Object>) data;
		notifyDataSetInvalidated();

	}

	@Override
	public void clear() {

	}

	@Override
	public Intent createData(View row) {
		Intent intent = new Intent();
		intent.putExtra(LibraryAdapter.DATA_TYPE, MediaUtils.TYPE_DROPBOX);
		if (!mLinkedWithDropbox) {
			intent.putExtra(LibraryAdapter.DATA_LINK_WITH_DROPBOX, true);
			return intent;
		}
		return null;
	}

	private String getFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	private static String getDatePassMilliseconds(long milliseconds) {
		String dateFormatted = "";
		if (milliseconds != 0) {
			Date date = new Date(milliseconds);
			DateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");
			dateFormatted = dateFormat.format(date);
		}
		return dateFormatted;
	}

	@Override
	public Limiter buildLimiter(int type, long id, String preGeneratedName) {
		return null;
	}

	public void setFiles(ListFolderResult result) {
		if (mCloudFiles == null) {
			mCloudFiles = new ArrayList<>();
		}
		mCloudFiles.clear();
		mCloudFiles.addAll(result.getEntries());
		notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		validateDropbox();
	}

	private void validateDropbox() {
		SharedPreferences prefs = mContext.getSharedPreferences(CloudBaseActivity.ACCOUNT_PREFS_NAME, 0);
		String access_token = prefs.getString(CloudBaseActivity.ACCESS_TOKEN, null);
		if (access_token == null || access_token.length() == 0) {
			mLinkedWithDropbox = false;
		} else {
			mLinkedWithDropbox = true;
		}
	}
}
