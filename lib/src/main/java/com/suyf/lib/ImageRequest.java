/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.suyf.lib;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;
import com.suyf.lib.ImageLoader.ImageCache;
import okhttp3.Call;
import okhttp3.Response;
import pl.droidsonroids.gif.GifDrawable;

/**
 * A canned request for getting an image at a given URL and calling back with a decoded Bitmap.
 */
public class ImageRequest {

  private static final Object sDecodeLock = new Object();

  private final Config mDecodeConfig;
  private final int mMaxWidth;
  private final int mMaxHeight;
  private final ScaleType mScaleType;

  private String mUrl;
  private ResponseListener mResponseListener;

  /**
   * Creates a new image request, decoding to a maximum specified width and height. If both width
   * and height are zero, the image will be decoded to its natural size. If one of the two is
   * nonzero, that dimension will be clamped and the other one will be set to preserve the image's
   * aspect ratio. If both width and height are nonzero, the image will be decoded to be fit in the
   * rectangle of dimensions width x height while keeping its aspect ratio.
   *
   * @param url URL of the image
   * @param responseListener Listener to receive the decoded bitmap
   * @param maxWidth Maximum width to decode this bitmap to, or zero for none
   * @param maxHeight Maximum height to decode this bitmap to, or zero for none
   * @param scaleType The ImageViews ScaleType used to calculate the needed image size.
   * @param decodeConfig Format to decode the bitmap to
   */
  public ImageRequest(
      String url,
      ResponseListener responseListener,
      int maxWidth,
      int maxHeight,
      ScaleType scaleType,
      Config decodeConfig,
      final Call call,
      final ImageCache cache) {
    mUrl = url;
    mResponseListener = responseListener;
    mDecodeConfig = decodeConfig;
    mMaxWidth = maxWidth;
    mMaxHeight = maxHeight;
    mScaleType = scaleType;

    ConcurrentExecutor.get().execute(new Runnable() {
      @Override
      public void run() {
        Bitmap cacheBitmap = cache.getBitmap(mUrl);
        if (cacheBitmap != null) {
          mResponseListener.onBitmapResponse(cacheBitmap);
          return;
        }
        boolean support = supportDrawable();
        if (support) {
          try {
            byte[] cacheFile = cache.getFile(mUrl);
            if (cacheFile != null) {
              Drawable drawable = new GifDrawable(cacheFile);
              mResponseListener.onDrawableResponse(drawable);
              return;
            }
          } catch (Exception e) {
          }
        }
        Response response = null;
        try {
          response = call.execute();
          byte[] bytes = response.body().bytes();
          try {
            if (support) {
              Drawable drawable = new GifDrawable(bytes);
              cache.putFile(mUrl, bytes);
              mResponseListener.onDrawableResponse(drawable);
            } else {
              throw new Exception("not support");
            }
          } catch (Exception e) {
            Bitmap bitmap = parseNetworkResponse(bytes);
            if (bitmap != null) {
              mResponseListener.onBitmapResponse(bitmap);
            } else {
              mResponseListener.onErrorResponse(new Exception("parse bitmap fail"));
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          mResponseListener.onErrorResponse(e);
        } finally {
          try {
            response.close();
          } catch (Exception e) {
          }
        }
      }
    });
  }

  /**
   * check if class exit. cannot be confuse.
   */
  private boolean supportDrawable() {
    try {
      Class.forName("pl.droidsonroids.gif.GifDrawable");
      return true;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * For API compatibility with the pre-ScaleType variant of the constructor. Equivalent to the
   * normal constructor with {@code ScaleType.CENTER_INSIDE}.
   */
  @Deprecated
  public ImageRequest(
      String url,
      ResponseListener responseListener,
      int maxWidth,
      int maxHeight,
      Config decodeConfig,
      Call call,
      ImageCache cache) {
    this(url, responseListener, maxWidth, maxHeight, ScaleType.CENTER_INSIDE, decodeConfig, call,
        cache);
  }

  /**
   * Scales one side of a rectangle to fit aspect ratio.
   *
   * @param maxPrimary Maximum size of the primary dimension (i.e. width for max width), or zero to
   * maintain aspect ratio with secondary dimension
   * @param maxSecondary Maximum size of the secondary dimension, or zero to maintain aspect ratio
   * with primary dimension
   * @param actualPrimary Actual size of the primary dimension
   * @param actualSecondary Actual size of the secondary dimension
   * @param scaleType The ScaleType used to calculate the needed image size.
   */
  private static int getResizedDimension(
      int maxPrimary,
      int maxSecondary,
      int actualPrimary,
      int actualSecondary,
      ScaleType scaleType) {

    // If no dominant value at all, just return the actual.
    if ((maxPrimary == 0) && (maxSecondary == 0)) {
      return actualPrimary;
    }

    // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
    if (scaleType == ScaleType.FIT_XY) {
      if (maxPrimary == 0) {
        return actualPrimary;
      }
      return maxPrimary;
    }

    // If primary is unspecified, scale primary to match secondary's scaling ratio.
    if (maxPrimary == 0) {
      double ratio = (double) maxSecondary / (double) actualSecondary;
      return (int) (actualPrimary * ratio);
    }

    if (maxSecondary == 0) {
      return maxPrimary;
    }

    double ratio = (double) actualSecondary / (double) actualPrimary;
    int resized = maxPrimary;

    // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
    if (scaleType == ScaleType.CENTER_CROP) {
      if ((resized * ratio) < maxSecondary) {
        resized = (int) (maxSecondary / ratio);
      }
      return resized;
    }

    if ((resized * ratio) > maxSecondary) {
      resized = (int) (maxSecondary / ratio);
    }
    return resized;
  }

  /**
   * Returns the largest power-of-two divisor for use in downscaling a bitmap that will not result
   * in the scaling past the desired dimensions.
   *
   * @param actualWidth Actual width of the bitmap
   * @param actualHeight Actual height of the bitmap
   * @param desiredWidth Desired width of the bitmap
   * @param desiredHeight Desired height of the bitmap
   */
  static int findBestSampleSize(
      int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
    double wr = (double) actualWidth / desiredWidth;
    double hr = (double) actualHeight / desiredHeight;
    double ratio = Math.min(wr, hr);
    float n = 1.0f;
    while ((n * 2) <= ratio) {
      n *= 2;
    }
    return (int) n;
  }

  protected Bitmap parseNetworkResponse(byte[] response) {
    // Serialize all decode on a global lock to reduce concurrent heap usage.
    synchronized (sDecodeLock) {
      try {
        return doParse(response);
      } catch (OutOfMemoryError e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  /**
   * The real guts of parseNetworkResponse. Broken out for readability.
   */
  private Bitmap doParse(byte[] data) {
    BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
    Bitmap bitmap;
    if (mMaxWidth == 0 && mMaxHeight == 0) {
      decodeOptions.inPreferredConfig = mDecodeConfig;
      bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
    } else {
      // If we have to resize this image, first get the natural bounds.
      decodeOptions.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
      int actualWidth = decodeOptions.outWidth;
      int actualHeight = decodeOptions.outHeight;

      // Then compute the dimensions we would ideally like to decode to.
      int desiredWidth =
          getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight, mScaleType);
      int desiredHeight =
          getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth, mScaleType);

      // Decode to the nearest power of two scaling factor.
      decodeOptions.inJustDecodeBounds = false;
      // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
      // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
      decodeOptions.inSampleSize =
          findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
      Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

      // If necessary, scale down to the maximal acceptable size.
      if (tempBitmap != null
          && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
        bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
        tempBitmap.recycle();
      } else {
        bitmap = tempBitmap;
      }
    }
    return bitmap;
  }

}
