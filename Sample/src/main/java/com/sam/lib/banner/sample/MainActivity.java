package com.sam.lib.banner.sample;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.sam.lib.sample.Banner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private Banner<Bean> mBanner;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBanner = findViewById(R.id.banner);
        List<Bean> packs = new ArrayList<Bean>();
        packs.add(new Bean("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0D/01/ChMkJ1gq00WIXw_GAA47r_8gjqgAAXxJAH8qOMADjvH566.jpg"));
        packs.add(new Bean("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/0D/ChMkJ1e9jHqIWT4CAA2dKPU9Js8AAUsZgMf8mkADZ1A116.jpg"));
        packs.add(new Bean("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/0D/ChMkJle9jIGIMgtdAAYnBOEz3LAAAUsZwPgFgYABicc437.jpg"));
        packs.add(new Bean("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0F/0A/ChMkJleZ8-iIBbFBAAVrdxItOlQAAT76QAFx7oABWuP846.jpg"));
        packs.add(new Bean("http://desk.fd.zol-img.com.cn/t_s1024x768c5/g5/M00/0B/04/ChMkJ1bG5kyIcwkXAAsM0s9DJzoAAKsAwJB9ncACwzq207.jpg"));

        mBanner.setBannerData(packs);
        mBanner.setOnItemPickListener(new Banner.OnItemPickListener<Bean>() {
            @Override
            public void onItemPick(int position, Bean item) {

            }
        });
        mBanner.setOnItemBindListener(new Banner.OnItemBindListener<Bean>() {
            @Override
            public void onItemBind(int position, Bean item, ImageView view, Banner.BannerViewHolder holder) {
                Glide.with(view).load(item.url).into(view);
//                ((TextView) holder.getView(R.id.tv)).setText("Item " + position);
            }

        });

    }

    static class Bean {
        private String url;

        public Bean(String url) {
            this.url = url;
        }
    }
}
