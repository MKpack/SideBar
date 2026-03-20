package com.android.systemui.sidebar;

import android.graphics.drawable.Drawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Color;
import android.util.Log;

import com.android.internal.graphics.ColorUtils;

public class LineBackgroundDrawable extends Drawable {
    private static final String TAG = "LineBackgroundDrawable";

    private Context mContext;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean mGlassMode = false;

    private float mRadiusPx;
    private int mLightIconColor;
    private int mDarkIconColor;
    private RectF mRect = new RectF();
    // 记录上一刻状态
    private float lastDarkIntensity;

    public LineBackgroundDrawable(Context context, int lightIconColor, int darkIconColor) {
        mLightIconColor = lightIconColor;
        mDarkIconColor = darkIconColor;
        mContext = context;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mLightIconColor);
        mRadiusPx = dpToPx(20);
    }

    public static LineBackgroundDrawable createLineDrawable(Context context, int lightIconColor, int darkIconColor) {
        return new LineBackgroundDrawable(context, lightIconColor, darkIconColor);
    }

    public void setDarkIntensity(float darkIntensity) {
        lastDarkIntensity = darkIntensity;
        int color = ColorUtils.blendARGB(mLightIconColor, mDarkIconColor,
                darkIntensity);
        Log.d(TAG, "mLightIconColor" + mLightIconColor + ", mDarkIconColor" + mDarkIconColor);
        mPaint.setColor(color);
        mPaint.setAlpha((int) (255 * (1f - darkIntensity * 0.3f)));
        invalidateSelf();
    }

    public void setGlassMode(boolean glassMode) {
        mGlassMode = glassMode;
        if (!mGlassMode) {
            setDarkIntensity(lastDarkIntensity);
            return;
        }
        invalidateSelf();
    }

    public void setBackgroundColor(int color) {
        mPaint.setColor(color);
    }

    // set cornerRadius
    public void setCornerRadius(float radius) {
        this.mRadiusPx = (int) radius;
        invalidateSelf();
    }

    // draw
    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        // Log.d(TAG, "bounds: " + bounds);
        mRect.set(bounds);
        if (mGlassMode) {
            // mPaint.setColor(Color.DKGRAY);
            // canvas.drawRoundRect(mRect, mRadiusPx, mRadiusPx, mPaint);

            mPaint.setAlpha(255);
            // 白色半透明底
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0xCCFFFFFF); // 80% 白
            canvas.drawRoundRect(mRect, mRadiusPx, mRadiusPx, mPaint);

            // 边框高光
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(dpToPx(1));
            mPaint.setColor(0x4DFFFFFF);
            canvas.drawRoundRect(mRect, mRadiusPx, mRadiusPx, mPaint);

            // 恢复
            mPaint.setStyle(Paint.Style.FILL);
            return;
        }
        canvas.drawRoundRect(mRect, mRadiusPx, mRadiusPx, mPaint);
    }

    // 绘制透明度
    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    // 颜色滤镜
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    // 透明度类型
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }
}
