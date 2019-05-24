package com.suyf.lib;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface ResponseListener {

  void onDrawableResponse(Drawable drawable);

  void onBitmapResponse(Bitmap response);

  void onErrorResponse(Exception error);
}
