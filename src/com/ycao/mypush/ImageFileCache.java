package com.ycao.mypush;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class ImageFileCache {
	private static final String CACHDIR = "lualu";
	private static final String WHOLESALE_CONV = "";
	/** �???��?3�?**/
	private static final long mTimeDiff = 10 * 24 * 60 * 60 * 1000;

	public ImageFileCache() {
		// �????���??
		//removeCache(getDirectory());
	}

	public Bitmap getImage(final String url) {
		final String path = getDirectory() + "/" + convertUrlToFileName(url);
		File file = new File(path);
		if (file.exists()) {
			Bitmap bmp = BitmapFactory.decodeFile(path);
			if (bmp == null) {
				file.delete();
			} else {
				updateFileTime(path);
				return bmp;
			}
		}
		return null;
	}

	/*** �??空�?大�? ****/
	private static final int FREE_SD_SPACE_NEEDED_TO_CACHE = 10;

	public void saveBmpToSd(Bitmap bm, String url) {
		if (bm == null) {
			// ???�?????�?��空�?
			return;
		}
		// ?��?sdcard�??空�?
		if (FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
			// SD空�?�?��
			return;
		}
		String filename = convertUrlToFileName(url);
		String dir = getDirectory();
		File file = new File(dir + "/" + filename);
		try {
			file.createNewFile();
			OutputStream outStream = new FileOutputStream(file);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			outStream.flush();
			outStream.close();

		} catch (FileNotFoundException e) {
			Log.w("ImageFileCache", "FileNotFoundException");
		} catch (IOException e) {
			Log.w("ImageFileCache", "IOException");
		}
	}

	private static final int CACHE_SIZE = 10;

	// �??�??
	/**
	 * 计�?�?????�????��大�?�?
	 * �??件�?大�?大�?�????ACHE_SIZE???sdcard?��?空�?�??FREE_SD_SPACE_NEEDED_TO_CACHE???�?
	 * ?��????40%???没�?�?��?��???��
	 * 
	 * @param dirPath
	 * @param filename
	 */
	private boolean removeCache(String dirPath) {
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		if (files == null) {
			return true;
		}
		if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			return false;
		}

		int dirSize = 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().contains(WHOLESALE_CONV)) {
				dirSize += files[i].length();
			}
		}

		if (dirSize > CACHE_SIZE * MB || FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
			int removeFactor = (int) ((0.4 * files.length) + 1);

			Arrays.sort(files, new FileLastModifSort());

			Log.i("ImageFileCache", "�??�????��");
			for (int i = 0; i < removeFactor; i++) {

				if (files[i].getName().contains(WHOLESALE_CONV)) {
					files[i].delete();
				}

			}

		}

		if (freeSpaceOnSd() <= CACHE_SIZE) {
			return false;
		}
		return true;
	}

	/**
	 * TODO ?��???��?????��?��??��?�??�?*
	 */
	private class FileLastModifSort implements Comparator<File> {
		public int compare(File arg0, File arg1) {
			if (arg0.lastModified() > arg1.lastModified()) {
				return 1;
			} else if (arg0.lastModified() == arg1.lastModified()) {
				return 0;
			} else {
				return -1;
			}
		}
	}

	/**
	 * ???�????��
	 * 
	 * @param dirPath
	 * @param filename
	 */
	public void removeExpiredCache(String dirPath, String filename) {

		File file = new File(dirPath, filename);

		if (System.currentTimeMillis() - file.lastModified() > mTimeDiff) {

			Log.i("ImageFileCache", "Clear some expiredcache files ");

			file.delete();
		}

	}

	/**
	 * �????��?????��?��???�????????,???�?��?��??��??��??�为�???��?
	 * 
	 * @param path
	 */
	public void updateFileTime(String path) {
		File file = new File(path);
		long newModifiedTime = System.currentTimeMillis();
		file.setLastModified(newModifiedTime);
	}

	/**
	 * 计�?sdcard�???��?空�?
	 * 
	 * @return
	 */
	private int MB = 1024 * 1024;

	private int freeSpaceOnSd() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat.getBlockSize()) / MB;
		return (int) sdFreeMB;
	}

	/** �?rl�????��??**/
	private String convertUrlToFileName(String url) {
		String[] strs = url.split("/");
		return strs[strs.length - 1] + WHOLESALE_CONV;
	}

	/** ?��?�????? **/
	private String getDirectory() {
		String dir = getSDPath() + "/" + CACHDIR;
		String substr = dir.substring(0, 4);
		if (substr.equals("/mnt")) {
			dir = dir.replace("/mnt", "");
		}
		return dir;
	}

	/**** ??D?�路�??�? ****/
	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // ?��?sd?��??????
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// ?��?�??�?
		}
		if (sdDir != null) {
			return sdDir.toString();
		} else {
			return "";
		}
	}
}