package me.wizos.loread.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.List;

import me.wizos.loread.net.API;
import me.wizos.loread.App;
import me.wizos.loread.R;
import me.wizos.loread.bean.Article;
import me.wizos.loread.gson.itemContents.Origin;
import me.wizos.loread.utils.UTime;

/**
 * Created by Wizos on 2016/3/15.
 */
public class MainSlvAdapter extends ArrayAdapter<Article> {


    public MainSlvAdapter(Context context, List<Article> itemArray){
        super(context, 0 , itemArray);
        this.articleList = itemArray;
        this.context = context;
    }
//    public MainSlvAdapter(Context context, int textViewResourceId, List<Article> itemArray){
//        super(context, textViewResourceId, itemArray);
//        this.articleList = itemArray;
//        this.context = context;
//    }

    List<Article> articleList;
    Context context;

    @Override
    public int getCount() {
        return articleList.size();
    }
    @Override
    public Article getItem(int position) {
        return articleList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CustomViewHolder cvh;
        Article article = this.getItem(position);
        String articleFeedName = "";
        if (convertView == null) {
            cvh = new CustomViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.articleslv_item, null);
            cvh.articleTitle = (TextView) convertView.findViewById(R.id.articleslv_item_title);
            cvh.articleAuthor = (TextView) convertView.findViewById(R.id.articleslv_item_author);
            cvh.articleTime = (TextView) convertView.findViewById(R.id.articleslv_item_time);
            cvh.articleStar = (ImageView)convertView.findViewById(R.id.articleslv_item_star);
            cvh.articleReading = (ImageView)convertView.findViewById(R.id.articleslv_item_reading);
            convertView.setTag(cvh);
        } else {
            cvh = (CustomViewHolder) convertView.getTag();
        }

        cvh.articleTitle.setText(Html.fromHtml(article.getTitle()));

        Gson gson = new Gson();
        Origin origin = gson.fromJson(article.getOrigin(), Origin.class);
        cvh.articleAuthor.setText(Html.fromHtml(origin.getTitle()));
        cvh.articleTime.setText(UTime.formatDate(article.getCrawlTimeMsec()));
        if (article.getReadState().equals(API.ART_READ)) {
//            System.out.println("【1】" + article.getTitle());
            cvh.articleTitle.setAlpha(0.50f);
            cvh.articleTitle.setTextColor(App.getInstance().getResources().getColor(R.color.main_grey_light));
        } else {
//            System.out.println("【2】" + article.getTitle());
            cvh.articleTitle.setAlpha(0.90f);
            cvh.articleTitle.setTextColor(App.getInstance().getResources().getColor(R.color.main_grey_dark));
        }
        if( article.getReadState().equals(API.ART_READING)){
            cvh.articleReading.setVisibility(View.VISIBLE);
        }else {
            cvh.articleReading.setVisibility(View.GONE);
        }
        if (article.getStarState().equals(API.ART_STAR)) {
            cvh.articleStar.setVisibility(View.VISIBLE);
        }else {
            cvh.articleStar.setVisibility(View.GONE);
        }


//        System.out.println("【MainSlvAdapter】" + article.getTitle()  + article.getCategories());

        return convertView;
    }
    class CustomViewHolder {
        //        public ImageView imgIcon;
        public TextView articleTitle;
        public TextView articleAuthor;
        public TextView articleTime;
        public ImageView articleStar;
        public ImageView articleReading;
//        public TextView txtMoney;
    }
    
}
