package com.suyf.networkimageview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.suyf.lib.ImageLoader;
import com.suyf.lib.NetworkImageView;
import com.suyf.networkimageview.http.NetworkClient;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    NetworkImageView imageView = findViewById(R.id.image);
    String url = "http://pic36.nipic.com/20131129/8821914_111419739001_2.jpg";
    imageView.setDefaultImageResId(R.mipmap.loading);
    imageView.setImageUrl(url, new ImageLoader(NetworkClient.get().getClient(),
        NetworkClient.get().getBitmapCache()));

    NetworkImageView imageView2 = findViewById(R.id.image2);
    String url2 = "http://img308.ph.126.net/gZv3PX_vBJ2VEKgwlT6PJQ==/3897584002512586676.gif";
    imageView.setDefaultImageResId(R.mipmap.loading);
    imageView2.setImageUrl(url2,
        new ImageLoader(NetworkClient.get().getClient(),
            NetworkClient.get().getBitmapCache()));
  }
}
