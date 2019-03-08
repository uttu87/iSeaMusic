package com.iseasoft.iSeaMusic.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class Storage
{

	public static final String APP_FILES_RELATIVE_PATH = "/MediaMonkey/files";
	private static final String STORAGE_INFO_XML_FILE = "/storageInfo.xml";
	private static final String STORAGE_INFO_MMW_XML_FILE = "/storageInfo.xml.mmw";
	private static final String STORAGE_INFO_VERSION = "1.0";

	private static final String STORAGE_GUID_ATTRIBUTE = "storageGuid";
	private static final String VERSION_ATTRIBUTE = "version";

	private static final String STORAGES_ELEMENT = "storages";

	private static final String INTERNAL_STORAGE_NAME = "Internal";
	private static final String INTERNAL_SD_CARD = "Internal Storage";

	private static List<Storage> sStorages;
	private static Object sMonitor = new Object();

	public static int NO_EXTERNEL_STORAGE = 0;
	public static int ONLY_INT_EXTERNEL_STORAGE = 1;
	public static int ONLY_EXT_EXTERNEL_STORAGE = 2;
	public static int BOTH_INT_EXT_EXTERNEL_STORAGE = 3;

	public static Storage getStorageByGuid(Context c, String guid)
	{
		List<Storage> storages = getAllStorages(c);

		for (Storage s : storages)
		{
			if (guid.equals(s.getGuid()))
			{
				return s;
			}
		}

		return null;
	}

	public static Storage getMainStorage(Context c)
	{
		synchronized (sMonitor)
		{
			getAllStorages(c);

			if (sStorages == null || sStorages.isEmpty())
			{
				return null;
			}
			else
				return sStorages.get(0);
		}
	}

	public static Storage getMainExternalStorage(Context c)
	{
		synchronized (sMonitor)
		{
			getAllStorages(c);

			if (sStorages == null || sStorages.isEmpty())
			{
				return null;
			}
			else
			{
				for (Storage s : sStorages)
				{
					if (!s.getName().equals(INTERNAL_STORAGE_NAME))
					{
						return s;
					}
				}
				return null;
			}
		}
	}

	public static Storage getSubExternelStorage(Context c)
	{
		synchronized (sMonitor)
		{
			getAllStorages(c);

			if (sStorages == null || sStorages.isEmpty())
			{
				return null;
			}
			else
			{
				for (Storage s : sStorages)
				{
					if (!s.getName().equals(INTERNAL_STORAGE_NAME) && !s.getName().equals(INTERNAL_SD_CARD))
					{
						return s;
					}
				}
				return null;
			}
		}

	}

	public static int getExternelStorageStatus(Context c)
	{
		refreshStorages(c);
		if (getMainExternalStorage(c) != null)
		{
			if (getSubExternelStorage(c) != null)
			{
				return BOTH_INT_EXT_EXTERNEL_STORAGE;
			}
			else
			{
				return ONLY_INT_EXTERNEL_STORAGE;
			}
		}
		else if (getSubExternelStorage(c) != null)
		{
			return ONLY_EXT_EXTERNEL_STORAGE;
		}
		else
		{
			return NO_EXTERNEL_STORAGE;
		}
	}

	public static List<Storage> refreshStorages(Context c)
	{
		synchronized (sMonitor)
		{
			sStorages = null;

			return getAllStorages(c);
		}
	}

	public static List<Storage> getAllStorages(Context c)
	{
		synchronized (sMonitor)
		{
			if (sStorages == null)
			{
				// SD card not mounted
				if (!Config.Storage.INTERNAL_MAIN_STORAGE && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				{
					//log.w("External storage not mounted");
					return new CopyOnWriteArrayList<Storage>();// Empty collcetion
				}

				List<Storage> storages = new ArrayList<Storage>();

				if (Config.Storage.INTERNAL_MAIN_STORAGE)
				{
					String root = Environment.getDataDirectory().toString();
					String files = c.getFilesDir().toString();
					storages.add(new Storage(INTERNAL_STORAGE_NAME, root, files, files));
				}

				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				{
					StorageScanner scanner = new StorageScanner();
					scanner.scan();
					storages.addAll(scanner.getValidStorages());
				}

				int i = 0;
				for (Storage s : storages)
				{
					s.setId(i++);
					if (s.configFilesDir == null)
					{
						s.configFilesDir = s.rootDir + APP_FILES_RELATIVE_PATH;
					}
					if (s.databaseDir == null)
					{
						s.databaseDir = s.rootDir + APP_FILES_RELATIVE_PATH;
					}
					s.parseGuid();
					if (s.guid == null)
					{
						s.generateGuid(c);
					}
				}

				writeStorageInfo(c, storages);

				sStorages = storages;
			}

			return sStorages;
		}
	}

	public static boolean isMainStorageAvailable(Context context)
	{
		if (!Config.Storage.INTERNAL_MAIN_STORAGE && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			return false;
		}
		else if (Config.Storage.INTERNAL_MAIN_STORAGE)
		{
			return true;
		}

		Storage s = getMainStorage(context);

		if (s == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public static void writeStorageInfo(Context c, List<Storage> storages)
	{
		if (storages == null)
			return;

		Iterator<Storage> i = storages.iterator();
		while (i.hasNext())
		{
			Storage s = i.next();

			File xmlFile = new File(s.configFilesDir + STORAGE_INFO_XML_FILE);
			try
			{
				// create directory
				if (!xmlFile.getParentFile().exists())
				{
					xmlFile.getParentFile().mkdirs();
				}

				// delete old file
				if (xmlFile.exists() && !xmlFile.delete())
				{
					continue;
				}

				// create new file
				if (!xmlFile.createNewFile())
					continue;
			}
			catch (IOException e)
			{
				i.remove();
				//log.e(e, false);
				continue;
			}

			FileOutputStream fileos = null;
			try
			{
				fileos = new FileOutputStream(xmlFile);

				fileos.write(s.generateStorageXml(storages).getBytes());

				// notify scanner about new file
				StorageUtils.notifyFileChange(c, xmlFile);
			}
			catch (FileNotFoundException e)
			{
				i.remove();
				//log.e(e, false);
				continue;
			}
			catch (IOException e)
			{
				i.remove();
				//log.e(e, false);
				continue;
			}
			finally
			{
				if (fileos != null)
				{
					try
					{
						fileos.close();
					}
					catch (IOException e)
					{
						//log.e(e, false);
					}
				}
			}
		}
	}

	private String name;
	private String rootDir; // path of this storage relative to main external storage
	private String configFilesDir; // absolute android path where xml should be stored
	private String databaseDir;
	private String guid;
	private int id;

	public Storage(String name, String relative, String config, String remoteDb)
	{
		this.name = name;
		this.rootDir = relative;
		this.configFilesDir = config;
		this.databaseDir = remoteDb;
	}

	private void generateGuid(Context context)
	{
		guid = new UuidFactory(context).getStorageUuid(this);
	}

	private void parseGuid()
	{
		parseGuidFromFile(new File(configFilesDir + STORAGE_INFO_XML_FILE));

		if (guid == null)
		{
			parseGuidFromFile((new File(configFilesDir + STORAGE_INFO_MMW_XML_FILE)));
		}
	}

	private void parseGuidFromFile(File xmlFile)
	{
		// check if file exists
		if (!xmlFile.exists())
		{
			guid = null;
			return;
		}

		XmlPullParser parser = Xml.newPullParser();
		try
		{
			// auto-detect the encoding from the stream
			parser.setInput(new FileInputStream(xmlFile), null);
			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT)
			{
				switch (eventType)
				{
					case XmlPullParser.START_TAG:
						if (STORAGES_ELEMENT.equals(parser.getName()))
						{
							guid = parser.getAttributeValue(null, STORAGE_GUID_ATTRIBUTE);
							return;
						}
					default:
						break;
				}
				eventType = parser.next();
			}
		}
		catch (Exception ex)
		{
			//log.e(Log.getStackTraceString(ex));
		}

		guid = null;
		return;
	}

	public String generateStorageXml(List<Storage> list)
	{
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try
		{
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", STORAGES_ELEMENT);
			serializer.attribute("", VERSION_ATTRIBUTE, STORAGE_INFO_VERSION);
			serializer.attribute("", STORAGE_GUID_ATTRIBUTE, this.guid);
			for (Storage s : list)
			{
				serializer.startTag("", "storage");
				serializer.startTag("", "title");
				serializer.text(s.name);
				serializer.endTag("", "title");
				serializer.startTag("", "path");
				serializer.text(s.rootDir);
				serializer.endTag("", "path");
				serializer.startTag("", "current");
				serializer.text(s.rootEquals(this) ? "1" : "0");
				serializer.endTag("", "current");
				serializer.endTag("", "storage");
			}
			serializer.endTag("", STORAGES_ELEMENT);
			serializer.endDocument();
			return writer.toString();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setId(int i)
	{
		id = i;
	}

	public int getId()
	{
		return id;
	}

	private boolean rootEquals(Storage storage)
	{
		return this.rootDir.equals(storage.rootDir);
	}

	public String getGuid()
	{
		return guid;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getRootDir()
	{
		return rootDir;
	}

	public void setRootDir(String rootDir)
	{
		this.rootDir = rootDir;
	}

	public String getConfigFilesDir()
	{
		return configFilesDir;
	}

	public void setConfigFilesDir(String configFilesDir)
	{
		this.configFilesDir = configFilesDir;
	}

	public String getDatabaseDir()
	{
		return databaseDir;
	}

	public void setDatabaseDir(String databaseDir)
	{
		this.databaseDir = databaseDir;
	}

	public void setGuid(String guid)
	{
		this.guid = guid;
	}

	public List<String> getAllRootDirs()
	{
		List<String> roots = new ArrayList<String>();

		roots.add(rootDir);

//		if(Utils.isApiLevelAtLeast(Utils.API_17_JB))
//		{
//			int numberIndex = rootDir.indexOf("sdcard") + 6;
//			roots.add(object)
//		}

		return roots;
	}

	public static Storage getNext(Context context, Storage storage)
	{
		List<Storage> storages = getAllStorages(context);

		int index = storages.indexOf(storage);

		if (index == -1 || index + 1 >= storages.size())
		{
			return null;
		}
		else
		{
			return storages.get(index + 1);
		}
	}
}
