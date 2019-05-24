package com.suyf.networkimageview.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import com.suyf.lib.BitmapCache;
import com.suyf.networkimageview.App;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkClient {

  static String TAG = "NetworkClient";
  private static NetworkClient sNetworkClient;
  private OkHttpClient client;
  private BitmapCache mBitmapCache;

  private NetworkClient() {
    mBitmapCache = new BitmapCache(App.get());

    File cacheFile = new File(App.get().getFilesDir().getAbsolutePath(), "okhttp_cache");
    int cacheSize = 20 * 1024 * 1024;
    Cache cache = new Cache(cacheFile, cacheSize);
    HttpX509Trust trustManager = new HttpX509Trust();
    OkHttpClient.Builder builder =
        new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(new LogInterceptor(
                new LogInterceptor.Logger() {
                  @Override
                  public void log(String message) {
                    if (!TextUtils.isEmpty(message)) {
                      Log.d(TAG, message);
                    }
                  }
                }))
            .addInterceptor(new Interceptor() {
              @Override
              public Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "User-Agent: Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Mobile Safari/537.36"
                    ).build();
                return chain.proceed(newRequest);
              }
            })
            .sslSocketFactory(new SSLSocketFact(trustManager), trustManager)
            .hostnameVerifier(
                new HostnameVerifier() {
                  @Override
                  public boolean verify(String hostname, SSLSession session) {
                    return true;
                  }
                })
            .cache(cache);

    client = builder.build();
    client.dispatcher().setMaxRequests(10);
    client.dispatcher().setMaxRequestsPerHost(3);
  }

  public static NetworkClient get() {
    if (sNetworkClient == null) {
      sNetworkClient = new NetworkClient();
    }
    return sNetworkClient;
  }

  public OkHttpClient getClient() {
    return client;
  }

  public BitmapCache getBitmapCache() {
    return mBitmapCache;
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivity =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivity == null) {
      return false;
    }
    NetworkInfo info = connectivity.getActiveNetworkInfo();
    return info != null && info.isConnected();
  }

}
