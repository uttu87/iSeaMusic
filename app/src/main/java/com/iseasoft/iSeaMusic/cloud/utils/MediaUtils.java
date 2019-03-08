package com.iseasoft.iSeaMusic.cloud.utils;

import android.content.Context;

import com.tsutaya.musicplayer.cloud.dropbox.DropboxContentHasher;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import jp.co.mytrax.traxcore.db.dao.MediaDao;
import jp.co.mytrax.traxcore.db.domain.Media;
import jp.co.mytrax.traxcore.db.store.MediaStore;

public class MediaUtils {
    public static final int TYPE_INVALID = -1;
    public static final int TYPE_UNIFIED = 1;
    public static final int TYPE_FILE = 0;
    public static final int TYPE_DROPBOX = 2;

    static String[] fileSizeUnits = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    public static String calculateProperFileSize(double bytes) {
        String sizeToReturn = "";
        int index = 0;
        for (index = 0; index < fileSizeUnits.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        sizeToReturn = String.format("%.2f", bytes) + " " + fileSizeUnits[index];
        return sizeToReturn;
    }

    public static boolean isExistSong(Context context, String md5, String title) {
        List<Media> medias = MediaDao.loadAllDownloadFiles(context, MediaStore.ItemType.MUSIC);
        if (medias != null && medias.size() > 0) {
            for (int i = 0; i < medias.size(); i++) {
                try {
                    if (title.equals(getFileNameInPath(medias.get(i).getData()))) {
                        if (md5.equals(getMD5Checksum(medias.get(i).getData()))) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    public static String getFileNameInPath(String path) {
        String[] segments = path.split("/");
        String fileName = segments[segments.length-1];
        return fileName;
    }

    public static boolean isExistSongInDropbox(Context context, String contextHash, String name) {
        List<Media> medias = MediaDao.loadAllDownloadFiles(context, MediaStore.ItemType.MUSIC);
        if (medias != null && medias.size() > 0) {
            for (int i = 0; i < medias.size(); i++) {
                if (name.equals(getFileNameInPath(medias.get(i).getData()))) {
                    try {
                        MessageDigest hasher = new DropboxContentHasher();
                        byte[] buf = new byte[1024];
                        InputStream in = new FileInputStream(medias.get(i).getData());
                        try {
                            while (true) {
                                int n = in.read(buf);
                                if (n < 0) break;  // EOF
                                hasher.update(buf, 0, n);
                            }
                        } finally {
                            in.close();
                        }
                        if (contextHash.equals(hex(hasher.digest()))) {
                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    static final char[] HEX_DIGITS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

    public static String hex(byte[] data) {
        char[] buf = new char[2 * data.length];
        int i = 0;
        for (byte b : data) {
            buf[i++] = HEX_DIGITS[(b & 0xf0) >>> 4];
            buf[i++] = HEX_DIGITS[b & 0x0f];
        }
        return new String(buf);
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
