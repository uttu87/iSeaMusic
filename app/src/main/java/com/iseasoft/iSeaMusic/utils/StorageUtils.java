package com.iseasoft.iSeaMusic.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;


public class StorageUtils
{

	public static final String ALBUM_ART = "album_art.jpg";

	public static boolean deleteMusicFile(String path)
	{
		File file = new File(path);

		if (file.exists() && file.delete())
		{
			deleteCleanDirectory(new File(path).getParentFile());

			return true;
		}
		else
			return false;

	}

	public static boolean deleteFile(String path)
	{
		File file = new File(path);

		if (file.exists() && file.delete())
		{
			return true;
		}
		else
			return false;

	}

	/**
	 * Deletes this directory if is empty or contains only album artwork. This method propagates it recursively to parent dir
	 *
	 * @param dir
	 *            directory to be cleaned and deleted
	 */
	private static void deleteCleanDirectory(File dir)
	{
		File parentDir = dir.getParentFile();

		File[] files = dir.listFiles();

		if (files.length > 1)
		{
			return;
		}
		else if (files.length == 1)
		{
			if (files[0].getName().equals(ALBUM_ART))
			{
				if (!files[0].delete())
					return;
			}
			else
			{
				return;
			}
		}

		if (dir.delete() && parentDir != null)
			deleteCleanDirectory(parentDir);

	}

	public static boolean mediaFileExists(String filename)
	{
		return fileExists(filename);
	}

	public static boolean fileExists(String filename)
	{
		if (filename == null)
			return false;
		else if (filename.startsWith("http://"))
			return true;
		else
		{
			File file = new File(filename);
			if (file.exists())
				return true;
			else
				return false;
		}
	}

	public static long getFileSize(String filename)
	{
		if (filename == null)
			return 0;
		else
		{
			File file = new File(filename);

			return file.length();
		}
	}

	public static String getCanonicalPath(String path)
	{
		try
		{
			if (path == null)
				return null;

			String canonical = new File(path).getCanonicalPath();

			if (Utils.isApiLevelAtLeast(Utils.API_17_JB))
			{
				return canonical.replace("/legacy", "/0");
			}

			return canonical;
		}
		catch (IOException e)
		{
			return path;
		}
	}

	public static void notifyFileChange(Context context, File file)
	{
		// if (context instanceof Activity || context instanceof Service)
//		{
//			MediaScannerConnection.scanFile(
//					context,
//					new String[] { file.toString() },
//					null,
//					new MediaScannerConnection.OnScanCompletedListener()
//					{
//						@Override
//						public void onScanCompleted(String path, Uri uri)
//						{
//							log.d("Scanned " + path + ", uri=" + uri);
//						}
//					});
//		}
//		else
		{
			Uri contentUri = Uri.fromFile(file);
			Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			mediaScanIntent.setData(contentUri);
			LocalBroadcastManager.getInstance(context).sendBroadcast(mediaScanIntent);
		}
	}

	public static void deleteAndNotifyFile(final Context context, final File file)
	{
//		context.getContentResolver().delete(MediaStore.Files.getContentUri("external"),
//				"lower(" + MediaStore.Files.FileColumns.DATA + ")=lower(?)",
//				new String[] { file.toString() });

		MediaScannerConnection.scanFile(
				context,
				new String[] { file.toString() },
				null,
				new MediaScannerConnection.OnScanCompletedListener()
				{
					@Override
					public void onScanCompleted(String path, Uri uri)
					{

						context.getContentResolver().delete(uri, null, null);
					}
				});
	}

	public static String repairPath(String path)
	{
		path = path.replace('\\', '/');
		while (path.contains("//"))
		{
			path = path.replace("//", "/");
		}

		return path;
	}

	public static String readFile(File file) throws IOException
	{
		FileInputStream stream = new FileInputStream(file);

//		byte[] bytes = new byte[200];
//		stream.read(bytes);
//
//		String s = new String(bytes, "UTF8");

		try
		{
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			// Instead of using default, pass in a decoder.
			return Charset.forName("UTF8").decode(bb).toString();
//			return Charset.defaultCharset().decode(bb).toString();
		}
		finally
		{
			stream.close();
		}
	}

	public static File renameOverwrite(Context context, File from, File to)
	{
		if (to.exists())
		{
			to.delete();
		}

		if (from.renameTo(to) && to.exists())
		{
			deleteAndNotifyFile(context, from);
			notifyFileChange(context, to);
			return to;
		}
		else
		{
			return null;
		}
	}

	public static void refreshMediaStore(Context c)
	{
		for (Storage s : Storage.getAllStorages(c))
		{
			LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + s.getRootDir())));
		}
	}

	public static String getMusicDirectory(){
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		if (!path.mkdir()) {
			path.mkdir();
		}
		return  path.toString();
	}
}
