package com.iseasoft.iSeaMusic.utils;

import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class StorageScanner
{
	private static String sDefaultPath = null;

	private static final String JB_DEFAULT_PATH = "/storage/sdcard0";
	private static final String GB_DEFAULT_PATH = "/mnt/sdcard";
	private static final String VOLD_PATH = "/system/etc/vold.fstab";
	private static final String MOUNTS_PATH = "/proc/mounts";

	private static final String EXT_SD = "External SD Card";
	private static final String EXT_SD1 = EXT_SD + " 1";
	private static final String EXT_SD2 = EXT_SD + " 2";

	private static final String INT = "Internal Storage";

	private final ArrayList<String> mMounts = new ArrayList<String>();
	private final ArrayList<String> mVold = new ArrayList<String>();

	private final ArrayList<Storage> mStorages = new ArrayList<Storage>();

	public StorageScanner()
	{

	}

	public ArrayList<String> getVolds()
	{
		return mVold;
	}

	public ArrayList<String> getMounts()
	{
		return mMounts;
	}

	public List<Storage> getValidStorages()
	{
		return mStorages;
	}

	public void scan()
	{
		mMounts.clear();
		mVold.clear();
		mStorages.clear();

		readMountsFile();

		readVoldFile();

		ArrayList<String> res = compareMountsWithVold();

		res = testPaths(res);

		setProperties(res);
	}

	private void readMountsFile()
	{
		/*
		 * Scan the /proc/mounts file and look for lines like this: /dev/block/vold/179:1 /mnt/sdcard vfat rw,dirsync,nosuid,nodev,noexec, relatime,uid=1000,gid=1015,fmask=0602,dmask
		 * =0602,allow_utime=0020,codepage =cp437,iocharset=iso8859-1,shortname=mixed,utf8,errors=remount-ro 0 0
		 * 
		 * When one is found, split it into its elements and then pull out the path to the that mount point and add it to the arraylist
		 */

		// some mount files don't list the default
		// path first, so we add it here to
		// ensure that it is first in our list
		mMounts.add(getDefaultPath());

		try
		{
			Scanner scanner = new Scanner(new File(MOUNTS_PATH));
			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				if (line.startsWith("/dev/block/vold/"))
				{
					String[] lineElements = line.split(" ");
					String element = lineElements[1];

					// don't add the default mount path
					// it's already in the list.
					if (!element.equals(getDefaultPath()))
						mMounts.add(element);
				}
			}
		}
		catch (Exception e)
		{
			//log.e(e);
		}
	}

	private static String getDefaultPath()
	{
		if (sDefaultPath == null)
		{
			if (Utils.isApiLevelAtLeast(Utils.API_16_JB))
			{
				sDefaultPath = JB_DEFAULT_PATH;
			}
			else
			{
				sDefaultPath = GB_DEFAULT_PATH;
			}
		}

		return sDefaultPath;
	}

	private void readVoldFile()
	{
		/*
		 * Scan the /system/etc/vold.fstab file and look for lines like this: dev_mount sdcard /mnt/sdcard 1 /devices/platform/s3c-sdhci.0/mmc_host/mmc0
		 * 
		 * When one is found, split it into its elements and then pull out the path to the that mount point and add it to the arraylist
		 */

		// some devices are missing the vold file entirely
		// so we add a path here to make sure the list always
		// includes the path to the first sdcard, whether real
		// or emulated.
		mVold.add(getDefaultPath());

		try
		{
			Scanner scanner = new Scanner(new File(VOLD_PATH));
			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				if (line.startsWith("dev_mount"))
				{
					String[] lineElements = line.split(" ");
					String element = lineElements[2];

					if (element.contains(":"))
						element = element.substring(0, element.indexOf(":"));

					// don't add the default vold path
					// it's already in the list.
					if (!element.equals(getDefaultPath()))
						mVold.add(element);
				}
			}
		}
		catch (Exception e)
		{
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ArrayList<String> compareMountsWithVold()
	{
		/*
		 * Sometimes the two lists of mount points will be different. We only want those mount points that are in both list.
		 * 
		 * Compare the two lists together and remove items that are not in both lists.
		 */

		ArrayList<String> intersection = new ArrayList<String>();

		for (String mount : mMounts)
		{
			if (mVold.contains(mount))
				intersection.add(mount);
		}

		return intersection;

	}

	private static ArrayList<String> testPaths(ArrayList<String> paths)
	{
		/*
		 * Now that we have a cleaned list of mount paths Test each one to make sure it's a valid and available path. If it is not, remove it from the list.
		 */

		ArrayList<String> tested = new ArrayList<String>();

		for (String p : paths)
		{
			if (p != null)
			{
				try
				{
					File root = new File(StorageUtils.getCanonicalPath(p));
					// fix bug can't detect "/storage/sdcard0" path on SONY (Android 4.1.2) device
					if (p.equals(JB_DEFAULT_PATH) && !root.exists())
					{
						root = new File(StorageUtils.getCanonicalPath(GB_DEFAULT_PATH));
					}

					if (root.exists() && root.isDirectory() && root.canWrite())
						tested.add(root.getAbsolutePath());
				}
				catch (Exception e)
				{
					//Logger.error("StorageScanner", e);
				}
			}
		}

		return tested;
	}

	private void setProperties(ArrayList<String> paths)
	{
		/*
		 * At this point all the paths in the list should be valid. Build the public properties.
		 */

		if (paths.size() > 0)
		{
			boolean hasInternal;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
			{
				hasInternal = true;
//				mStorages.add(new Storage("Auto", paths.get(0), null, null));
			}
			else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			{
				hasInternal = !Environment.isExternalStorageRemovable();
			}
			else
			{
				hasInternal = !Environment.isExternalStorageRemovable() || Environment.isExternalStorageEmulated();
			}

			addFirstTwoStorages(paths, hasInternal);

			if (paths.size() > 2)
			{
				for (int i = 2; i < paths.size(); i++)
				{
					mStorages.add(new Storage(EXT_SD + " " + (i + (hasInternal ? 0 : 1)), paths.get(i), null, null));
				}
			}
		}
	}

	private void addFirstTwoStorages(ArrayList<String> paths, boolean hasInternal)
	{
		if (hasInternal)
		{
			mStorages.add(new Storage(INT, paths.get(0), null, null));
			if (paths.size() > 1)
			{
				mStorages.add(new Storage((paths.size() > 2) ? EXT_SD1 : EXT_SD, paths.get(1), null, null));
			}
		}
		else
		{
			if (paths.size() > 1)
			{
				mStorages.add(new Storage(EXT_SD1, paths.get(0), null, null));
				mStorages.add(new Storage(EXT_SD2, paths.get(1), null, null));
			}
			else
			{
				mStorages.add(new Storage(EXT_SD, paths.get(0), null, null));
			}
		}
	}
}