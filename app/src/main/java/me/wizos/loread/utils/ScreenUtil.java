package me.wizos.loread.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.util.DisplayMetrics;

import me.wizos.loread.App;


/**
 * 屏幕工具
 * Created by Wizos on 2016/2/13.
 */
public class ScreenUtil {
    /**
     * 从 R.dimen 文件中获取到数值，再根据手机的分辨率转成为 px(像素)
     */
    public static int get2Px(Context context,@DimenRes int id) {
        final float scale = context.getResources().getDisplayMetrics().density;
        final float dpValue = (int)context.getResources().getDimension(id);
        return (int) (dpValue * scale + 0.5f);
    }
    public static int getDimen(Context context,@DimenRes int id) {
        return (int)context.getResources().getDimension(id);
    }
    public static int getColor(@ColorRes int id) {
        return App.i().getResources().getColor(id);
    }


    public static int resolveColor(Context context, @AttrRes int attr, int fallback) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, fallback);
        } finally {
            a.recycle();
        }
    }


    /**
     * 获取屏幕内容高度
     * @param activity
     * @return
     */
    public static int getScreenHeight(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int result = 0;
        int resourceId = activity.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return dm.heightPixels - result;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    public static int dpToPx(int dp) {
        DisplayMetrics displayMetrics = App.i().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }



}