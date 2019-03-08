package com.iseasoft.iSeaMusic.cloud;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.tsutaya.musicplayer.R;
import com.tsutaya.musicplayer.cloud.drive.GoogleDriveActivity;
import com.tsutaya.musicplayer.cloud.dropbox.DropboxActivity;

import java.util.ArrayList;

import jp.co.mytrax.traxcore.preferences.ActionBarPreferenceActivity;
import jp.co.mytrax.traxcore.ui.UiNavigationHelper;
import jp.co.mytrax.util.NetUtil;

public class CloudActivity extends ActionBarPreferenceActivity
{
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		setTitle(getString(R.string.cloud_title));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cloud);

		ListView cloudListView = (ListView) findViewById(R.id.actual_list_view);
		ArrayList<String> arrCloud = new ArrayList<String>();
		arrCloud.add(getString(R.string.cloud_google_drive));
		arrCloud.add(getString(R.string.cloud_dropbox));

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.listitem_cloud, arrCloud);
		cloudListView.setAdapter(adapter);

		cloudListView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (!NetUtil.hasNetworkConnection(CloudActivity.this))
				{
					Toast.makeText(CloudActivity.this, getResources().getString(R.string.wrong_internet), Toast.LENGTH_SHORT).show();
				}
				else
				{
					if (position == 0)
					{
						startActivity(new Intent(CloudActivity.this, GoogleDriveActivity.class));
					}
					else
					{
						startActivity(new Intent(CloudActivity.this, DropboxActivity.class));
					}
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		UiNavigationHelper.setHomeUpAction(this, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return UiNavigationHelper.navigateUp(this, item) ? true : super.onOptionsItemSelected(item);
	}

}
