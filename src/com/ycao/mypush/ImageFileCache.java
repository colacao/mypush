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
	/** è¿???¶é?3å¤?**/
	private static final long mTimeDiff = 10 * 24 * 60 * 60 * 1000;

	public ImageFileCache() {
		// æ¸????»¶ç¼??
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

	/*** ç¼??ç©ºé?å¤§å? ****/
	private static final int FREE_SD_SPACE_NEEDED_TO_CACHE = 10;

	public void saveBmpToSd(Bitmap bm, String url) {
		if (bm == null) {
			// ???ä¿?????ä¸?¸ªç©ºå?
			return;
		}
		// ?¤æ?sdcardä¸??ç©ºé?
		if (FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
			// SDç©ºé?ä¸?¶³
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

	// æ¸??ç¼??
	/**
	 * è®¡ç?å­?????ä¸????»¶å¤§å?ï¼?
	 * å½??ä»¶æ?å¤§å?å¤§ä?è§????ACHE_SIZE???sdcard?©ä?ç©ºé?å°??FREE_SD_SPACE_NEEDED_TO_CACHE???å®?
	 * ?£ä????40%???æ²¡æ?è¢?½¿?¨ç???»¶
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

			Log.i("ImageFileCache", "æ¸??ç¼????»¶");
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
	 * TODO ?¹æ???»¶?????¿®?¹æ??´è?è¡??åº?*
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
	 * ???è¿????»¶
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
	 * ä¿????»¶?????¿®?¹æ???è¿????????,???å°?½¿?¨ç??¾ç??¥æ??¹ä¸ºå½???¥æ?
	 * 
	 * @param path
	 */
	public void updateFileTime(String path) {
		File file = new File(path);
		long newModifiedTime = System.currentTimeMillis();
		file.setLastModified(newModifiedTime);
	}

	/**
	 * è®¡ç?sdcardä¸???©ä?ç©ºé?
	 * 
	 * @return
	 */
	private int MB = 1024 * 1024;

	private int freeSpaceOnSd() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat.getBlockSize()) / MB;
		return (int) sdFreeMB;
	}

	/** å°?rlè½????»¶??**/
	private String convertUrlToFileName(String url) {
		String[] strs = url.split("/");
		return strs[strs.length - 1] + WHOLESALE_CONV;
	}

	/** ?·å?ç¼????? **/
	private String getDirectory() {
		String dir = getSDPath() + "/" + CACHDIR;
		String substr = dir.substring(0, 4);
		if (substr.equals("/mnt")) {
			dir = dir.replace("/mnt", "");
		}
		return dir;
	}

	/**** ??D?¡è·¯å¾??å¸? ****/
	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // ?¤æ?sd?¡æ??????
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// ?·å?è·??å½?
		}
		if (sdDir != null) {
			return sdDir.toString();
		} else {
			return "";
		}
	}
}