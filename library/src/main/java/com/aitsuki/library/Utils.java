package com.aitsuki.library;

import android.content.Context;

/**
 * Created by AItsuki on 2016/6/29.
 */
public class Utils {
    public static int dip2px(Context context, float dipValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    /**
     * y=1-2x+x^2
     * @return y
     */
    public static float calcDragRate(float x) {
        return (float) (1- 2 * x + Math.pow(x,2));
    }
}
