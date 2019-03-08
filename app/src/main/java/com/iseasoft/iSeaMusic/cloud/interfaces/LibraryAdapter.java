package com.iseasoft.iSeaMusic.cloud.interfaces;

import android.content.Intent;
import android.view.View;

import com.tsutaya.musicplayer.cloud.utils.Limiter;

public interface LibraryAdapter
{
	public int getMediaType();

	public void setLimiter(Limiter limiter);

	public Limiter getLimiter();

	public void comitQuery(Object data);

	public void clear();

	public Limiter buildLimiter(int type, long id, String preGeneratedName);

	public Intent createData(View row);

	public static final String DATA_GO_UP = "goUp";

	public static final String DATA_LINK_WITH_DROPBOX = "linkWithDropbox";

	public static final String DATA_TYPE = "type";

	public static final String DATA_FILE = "file";

	public static final String DATA_EXPANDABLE = "expandable";

	public static final String DATA_ID = "id";

	public static final String DATA_TITLE = "title";

	public static final long HEADER_ID = -1;

	public static final long INVALID_ID = -2;

	public static final int ID_LINK_TO_PARENT_DIR = -10;

	public static final String TAG_DELIMITER_ROOT = "delimiterRoot";

	public static final String[] TAG_IDS_OF_INTEREST = { "TXXX", "TIT2", "TRCK", "TPE1", "TALB" };

	public static final String PREFS_CLOUD_SONG_CACHE = "daocCloudsongNinja";

	public static final String PREFS_CLOUD_DIR_HASHES = "daocCloudsongDirNinja";
}
