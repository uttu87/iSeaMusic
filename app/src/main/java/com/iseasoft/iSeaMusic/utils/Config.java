package com.iseasoft.iSeaMusic.utils;


public class Config
{
	public static class Version
	{
		public final static int EULA_VERSION = 2;
		public final static boolean BETA_VERSION = true;
	}

	public static class Playback
	{
		public static final boolean GAPLESS_ENABLED = false;
	}

	public static class Storage
	{
		public static final long MINIMUM_FREE_SPACE = 20;

		/** Store db files etc in app's internal file storage area. */
		public static final boolean INTERNAL_MAIN_STORAGE = true;
	}

	public static class Sync
	{
		public static class Usb
		{
			public static final int WAIT_FOR_DB_TRIES = 6;
			public static final long WAIT_FOR_DB_TIMEOUT = 5000;
		}

		public static class Wifi
		{
			// length of batch of sync list
			public static final int SYNCLIST_DOWNLOAD_BATCH_SIZE = 200;
			public static final int MODIFICATIONS_UPLOAD_BATCH_SIZE = 100;
			public static final int METADATA_UPLOAD_BATCH_SIZE = 100;
			public final static boolean AVAILABLE_PRODUCT = true;
		}

		public static class MediaStore
		{
			public static class Observer
			{
				public static final long FORCE_DELAY = 20000;
				public static final long IDLE_DELAY = 5000;
				public static final long START_DELAY = 2000;
			}
		}
	}

	public static class Upnp
	{
		public static class Service
		{
			public static final String TOKEN_NAME = "MediaMonkeyAndroid";
			public static final String TOKEN_VERSION = "1.0";

			public static final int THREAD_CORE_POOL_SIZE = 16;
			public static final int THREAD_MAXIMUM_POOL_SIZE = 32;
			public static final int THREAD_KEEP_ALIVE_TIME = 5;
			public static final int THREAD_QUEUE_SIZE = 512;
		}

		public static class Query
		{
			// timeout of whole serialized upnp query - ms
			public static final long CONNECTION_TIMEOUT = 40000;
			// http timeouts - seconds
			public static final int HTTP_CONNECTION_TIMEOUT = 19;
			public static final int HTTP_DATAREADOUT_TIMEOUT = 19;
		}

		public static class Discovery
		{
			public static final long DISCOVERY_TIMEOUT = 10000;
			public static final long PROGRESS_TIMEOUT = 2000;
		}

		public static class DefaultDirectories
		{
			public static final String MUSIC = "/Music/";
			public static final String VIDEO = "/Video/";
			public static final String AUDIOBOOK = "/Music/Audiobooks/";
			public static final String PODCAST = "/Music/Podcasts/";
		}
	}

	public static class Http
	{
		public static final int GET_BUFFER_SIZE = 4096;
		public static final int GET_CONNECTION_TIMEOUT = 20000;
		public static final int GET_READ_TIMEOUT = 20000;
		public static final int POST_CONNECTION_TIMEOUT = 20000;
		public static final int POST_SO_TIMEOUT = 20000;
	}

}
