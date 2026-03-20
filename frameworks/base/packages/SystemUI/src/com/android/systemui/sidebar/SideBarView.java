package com.android.systemui.sidebar;

import android.widget.FrameLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.graphics.Rect;
import com.android.systemui.R;

public class SideBarView extends FrameLayout {
    private static final String TAG = "SideBarView";
    private static final boolean DEBUG = false;

    public SideBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor("#d3239b00"));
            drawable.setCornerRadius(dpToPx(10));
            setBackground(drawable);
            Log.d(TAG, "color");
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density +
                0.5f);
    }

    // // 在 SideBarView.java 中
    // @Override
    // public boolean dispatchTouchEvent(MotionEvent ev) {
    // // 只有在非容器显示（即 Line 状态）时，才做区域穿透判定
    // if (!isShowContainer) { // 你需要通过某种方式获取这个状态
    // Rect rect = new Rect();
    // // 假设 mBarLine 是 SideBarView 的直接子 View
    // mBarLine.getHitRect(rect);

    // // 如果点击位置不在蓝色 Line 的矩形内，直接返回 false
    // // 这会告诉 WindowManager，这个事件请交给后面的窗口处理
    // if (!rect.contains((int) ev.getX(), (int) ev.getY())) {
    // return false;
    // }
    // }
    // return super.dispatchTouchEvent(ev);
    // }
}