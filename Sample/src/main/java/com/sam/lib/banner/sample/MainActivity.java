package com.sam.lib.banner.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.sam.lib.banner.Banner;
import com.sam.lib.banner.Banner.BaseBannerAdapter;

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


//        mBanner.setBannerData(packs, (position, item, view, holder) -> Glide.with(view).load(item.url).into(view));

        mBanner.setOnItemPickListener(new Banner.OnItemPickListener<Bean>() {
            @Override
            public void onItemPick(int position, Bean item) {
                Log.e("sam", "onItemPick: " + position);
            }
        });

        mBanner.setOnItemClickListener(new Banner.OnItemClickListener<Bean>() {
            @Override
            public void onItemClick(int position, Bean item) {
                Log.e("sam", "onItemClick: " + position);
            }
        });

        mBanner.setBannerData(new MyAdapter(packs));


    }

    class MyAdapter implements BaseBannerAdapter<Bean> {

        List<Bean> mData;

        public MyAdapter(List<Bean> mData) {
            this.mData = mData;
        }

        @Override
        public Banner.BannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0)
                return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.banner_item, parent, false));
            return null;
        }

        @Override
        public int getItemCount() {
            return mData != null ? mData.size() : 0;
        }

        @Override
        public void onBindViewHolder(Banner.BannerViewHolder holder, int position) {
            if (holder instanceof MyViewHolder) {
                MyViewHolder myViewHolder = (MyViewHolder) holder;
                myViewHolder.mTextView.setText("Item " + position);
                Glide.with(myViewHolder.imageView).load(mData.get(position).url).into(myViewHolder.imageView);
            }else{
                Glide.with(holder.getImageView()).load(mData.get(position).url).into(holder.getImageView());
            }
        }

        @Override
        public Bean getItem(int position) {
            return mData.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return position < 2 ? 0 : 1;
        }
    }

    static class MyViewHolder extends Banner.BannerViewHolder {

        TextView mTextView;
        ImageView imageView;

        public MyViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.tv);
            imageView = itemView.findViewById(R.id.banner_image_view_id);
        }
    }

    static class Bean {
        private String url;

        public Bean(String url) {
            this.url = url;
        }
    }
}
