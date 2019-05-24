package com.suyf.lib;

import android.content.Context;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageFileCache {

  private File mCacheFile;

  public ImageFileCache(Context context, String fileName) {
    mCacheFile = new File(context.getCacheDir(), "ImageFile");
    mCacheFile = new File(mCacheFile, fileName);
    if (!mCacheFile.exists()) {
      mCacheFile.mkdirs();
    }
  }

  public File getFile(String fileName) {
    return new File(mCacheFile, md5(fileName));
  }

  private static String md5(String key) {
    if (key == null) {
      return "";
    }
    String cacheKey;
    try {
      final MessageDigest mDigest = MessageDigest.getInstance("MD5");
      mDigest.update(key.getBytes());
      cacheKey = bytesToHexString(mDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      cacheKey = String.valueOf(key.hashCode());
    }
    return cacheKey;
  }

  private static String bytesToHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte _byte : bytes) {
      String hex = Integer.toHexString(0xFF & _byte);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString();
  }
}
