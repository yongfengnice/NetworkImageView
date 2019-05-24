package com.suyf.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapCache implements ImageLoader.ImageCache {

  // 内存缓存
  private LruCache<String, Bitmap> mMemoryCache;
  private ImageFileCache mImageFileCache;

  public BitmapCache(Context context) {
    // Get the Max available memory
    int maxMemory = (int) (Runtime.getRuntime().maxMemory());
    int cacheSize = maxMemory / 8;
    mMemoryCache =
        new LruCache<String, Bitmap>(cacheSize) {
          @Override
          protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getRowBytes() * bitmap.getHeight();
          }
        };
    mImageFileCache = new ImageFileCache(context, "images");
  }

  @Override
  public Bitmap getBitmap(String url) {
    Bitmap bitmap = mMemoryCache.get(url);
    if (bitmap == null) {
      return getBitmapFromFile(url);
    }
    return bitmap;
  }

  @Override
  public void putBitmap(String url, Bitmap bitmap) {
    if (getBitmap(url) == null) {
      mMemoryCache.put(url, bitmap);
      File imageFile = mImageFileCache.getFile(url);
      try {
        //save bitmap
        bitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(imageFile));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public byte[] getFile(String url) {
    byte[] bytes = null;
    File imageFile = mImageFileCache.getFile("file_" + url);
    if (imageFile == null || !imageFile.exists()) {
      return null;
    }
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(imageFile);
      bytes = new byte[inputStream.available()];
      inputStream.read(bytes, 0, inputStream.available());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return bytes;
  }

  @Override
  public void putFile(String url, byte[] bytes) {
    if (bytes == null || bytes.length <= 0) {
      return;
    }
    File imageFile = mImageFileCache.getFile("file_" + url);
    FileOutputStream outputStream = null;
    try {
      imageFile.createNewFile();
      outputStream = new FileOutputStream(imageFile);
      outputStream.write(bytes, 0, bytes.length);
      outputStream.flush();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private Bitmap getBitmapFromFile(String url) {
    File imageFile = mImageFileCache.getFile(url);
    if (imageFile.exists()) {
      return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }
    return null;
  }

}
