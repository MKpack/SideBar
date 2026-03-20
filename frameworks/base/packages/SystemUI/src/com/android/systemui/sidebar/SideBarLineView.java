package com.android.systemui.sidebar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.android.systemui.R;
import com.android.settingslib.Utils;

import com.android.internal.graphics.ColorUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.animation.ValueAnimator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.view.ViewOutlineProvider;
import android.graphics.Outline;

public class SideBarLineView extends FrameLayout {
    private static final String TAG = "SideBarLineView";

    private final int SIDEBAR_WIDTH = 6;
    private final int SIDEBAR_EXPANDED_WIDTH = 20;
    private final int SIDEBAR_HEIGHT = 70;
    private final int rangeY = 40;

    private int mLightIconColor;
    private int mDarkIconColor;

    private float mDarkIntensity = 0f;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mRadius = 20;
    private final int expandRadius = 10;
    private RectF mRect = new RectF();
    private LineBackgroundDrawable mLineBackgroundDrawable;

    private LayoutParams mLineLayoutParams;

    private ValueAnimator currentAnimator;

    private SideBarShotCutContainerView mContainerView;

    private boolean isShowContainer = false;

    public SideBarLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // get the color
        mLightIconColor = context.getColor(R.color.side_bar_line_light_color);
        mDarkIconColor = context.getColor(R.color.side_bar_line_dark_color);
        Log.d(TAG, "mLightIconColor: " + mLightIconColor + ", mDarkIconColor: " + mDarkIconColor);
        mLineBackgroundDrawable = LineBackgroundDrawable.createLineDrawable(context, mLightIconColor, mDarkIconColor);
        setBackground(mLineBackgroundDrawable);

        // 继承framelayout他会不让drawable中的draw进行绘制
        setWillNotDraw(false);

        // 这里需要裁剪不在圆角范围外的范围，不然外部仍展示导致看起来四角有灰色角，这里找了好久bug才找到
        setClipToOutline(true);

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        dpToPx(20));
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContainerView = findViewById(R.id.side_bar_short_cut_container);
    }

    public void initLineWH() {
        mLineLayoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        mLineLayoutParams.width = dpToPx(SIDEBAR_WIDTH);
        mLineLayoutParams.height = dpToPx(SIDEBAR_HEIGHT);
        setLayoutParams(mLineLayoutParams);
        setTranslationY(dpToPx(rangeY));
        Log.d(TAG, "width: " + mLineLayoutParams.width + ", height: " + mLineLayoutParams.height);
        mContainerView.setAlpha(0f);
    }

    public void setBackgroundColor(int color) {
        mLineBackgroundDrawable.setBackgroundColor(color);
    }

    public void setDarkIntensity(float darkIntensity) {
        if (mDarkIntensity == darkIntensity) {
            return;
        }
        mDarkIntensity = darkIntensity;
        mLineBackgroundDrawable.setDarkIntensity(mDarkIntensity);
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density +
                0.5f);
    }

    public void setExpanded(boolean expanded) {
        mLineLayoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        mLineLayoutParams.width = expanded ? dpToPx(SIDEBAR_EXPANDED_WIDTH) : dpToPx(SIDEBAR_WIDTH);
        setLayoutParams(mLineLayoutParams);
    }

    public void setSwitchSide(boolean toLeft) {
        mLineLayoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        mLineLayoutParams.gravity = toLeft ? (Gravity.LEFT | Gravity.TOP) : (Gravity.RIGHT | Gravity.TOP);
        setLayoutParams(mLineLayoutParams);
    }

    public interface AnimationListener {
        public void onFinished();
    }

    /**
     * 将计算放入该方法，如果上一个动画没结束直接开始这个更加平滑
     * 这个方法是在动画中改变layoutparams的
     * 
     * @param windowHeight window height
     * @param expandWidth  横向扩展宽度 == window width
     */
    public void startExpandToWindow(int windowHeight, int expandWidth, AnimationListener listener) {
        if (currentAnimator != null) {
            Log.d(TAG, "shrinkToLine cancel");
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
            currentAnimator = null;
        }

        // get initialze value
        final int startWidth = getWidth();
        final int startHeight = getHeight();
        final float startTranslationY = getTranslationY();
        Log.d(TAG, "startTranslationY: " + startTranslationY);

        final int targetHeight = windowHeight;
        final int targetWidth = expandWidth;

        mLineBackgroundDrawable.setCornerRadius(expandRadius);
        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(400);
        currentAnimator.setInterpolator(new FastOutSlowInInterpolator());

        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
                if (listener != null)
                    listener.onFinished();
                isShowContainer = true;
            }
        });
        currentAnimator.addUpdateListener(animation -> {
            float f = (float) animation.getAnimatedFraction();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            lp.width = (int) (startWidth + (targetWidth - startWidth) * f);
            lp.height = (int) (startHeight + (targetHeight - startHeight) * f);
            setLayoutParams(lp);

            // 向上扩张：通过减去 translationY 实现
            setTranslationY(startTranslationY - (startTranslationY * f));
            // 设置圆角，mRadius->expandRaduis
            // float currentRaduis = dpToPx(mRadius) - ((dpToPx(mRadius) -
            // dpToPx(expandRadius)) * f);
            // mLineBackgroundDrawable.setCornerRadius(currentRaduis);
            // container的显示
            if (mContainerView != null) {
                float alpha = Math.max(0f, (f - 0.3f) / 0.7f);
                mContainerView.setAlpha(alpha);
            }
        });
        currentAnimator.start();
    }

    /**
     * 
     * @param lineY 距离window顶部的距离
     */
    public void startShrinkToLine(int lineY, AnimationListener listener) {
        // 防止两个动画进行抢占
        if (currentAnimator != null) {
            Log.d(TAG, "ExpandToWindow cancel");
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
            currentAnimator = null;
        }
        final int startWidth = getWidth();
        final int startHeight = getHeight();
        final float startTranslationY = getTranslationY();

        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(300);
        currentAnimator.setInterpolator(new FastOutSlowInInterpolator());
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
                if (listener != null)
                    listener.onFinished();
                isShowContainer = false;
            }
        });
        currentAnimator.addUpdateListener(animation -> {
            float f = (float) animation.getAnimatedFraction();

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            lp.width = (int) (startWidth + (dpToPx(SIDEBAR_WIDTH) - startWidth) * f);
            lp.height = (int) (startHeight + (dpToPx(SIDEBAR_HEIGHT) - startHeight) * f);
            setLayoutParams(lp);
            // Y 偏移
            setTranslationY(startTranslationY + (lineY - startTranslationY) * f);

            // 设置圆角，expandRadius->mRadius
            float currentRaduis = dpToPx(expandRadius) - ((dpToPx(expandRadius) - dpToPx(mRadius)) * f);
            // mLineBackgroundDrawable.setCornerRadius(currentRaduis);

            // container 的隐藏
            if (mContainerView != null) {
                mContainerView.setAlpha(1f - f);
            }
        });

        currentAnimator.start();
    }

    /**
     * 对于startExpandTowindow方法的一个优化，不在动画中使用setlayoutparams减少layout的重绘制
     */
    public void startExpandToWindowOptimization(int windowHeight, int expandWith, boolean isOnLeftSide,
            AnimationListener listener) {
        if (currentAnimator != null) {
            Log.d(TAG, "shrinkToLine cancel");
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
            currentAnimator = null;
        }

        int width = getWidth();
        int height = getHeight();
        float translationY = getTranslationY();
        float targetScaleX = (float) width / expandWith;
        float targetScaleY = (float) height / windowHeight;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.width = expandWith;
        lp.height = windowHeight;
        setLayoutParams(lp);

        if (isOnLeftSide) {
            setPivotX(0f); // 水平方向以左边缘为基准（向右生长）
        } else {
            setPivotX(expandWith); // 水平方向以右边缘为基准 (向左生长)
        }
        setPivotY(0f); // 垂直方向以顶部为基准（向下生长）

        setScaleX(targetScaleX);
        setScaleY(targetScaleY);

        mLineBackgroundDrawable.setGlassMode(true);
        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(400);
        currentAnimator.setInterpolator(new FastOutSlowInInterpolator());

        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
                if (listener != null)
                    listener.onFinished();
                isShowContainer = true;
            }
        });
        currentAnimator.addUpdateListener(animation -> {
            float f = (float) animation.getAnimatedFraction();
            setScaleX(targetScaleX + (1 - targetScaleX) * f);
            setScaleY(targetScaleY + (1 - targetScaleY) * f);

            setTranslationY(translationY - translationY * f);
            // 设置圆角，mRadius->expandRaduis
            float currentRaduis = dpToPx(mRadius) - ((dpToPx(mRadius) -
                    dpToPx(expandRadius)) * f);
            mLineBackgroundDrawable.setCornerRadius(currentRaduis);
            invalidateOutline();
            setAlpha(Math.max(0f, (f - 0.3f) / 0.7f));
            // container的显示
            if (mContainerView != null) {
                float alpha = Math.max(0f, (f - 0.3f) / 0.7f);
                mContainerView.setAlpha(alpha);
            }
        });
        currentAnimator.start();
    }

    /**
     * 对于startExpandTowindow方法的一个优化，不在动画中使用setlayoutparams减少layout的重绘制
     * 
     * @param lineY
     * @param isOnLeftSide
     * @param listener
     */
    public void startShrinkToLineOptimization(int lineY, boolean isOnLeftSide, AnimationListener listener) {
        if (currentAnimator != null) {
            Log.d(TAG, "ExpandToWindow cancel");
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
            currentAnimator = null;
        }
        final int startWidth = getWidth();
        final int startHeight = getHeight();
        final float startTranslationY = getTranslationY();

        float targetScaleX = (float) startWidth / dpToPx(SIDEBAR_WIDTH);
        float targetScaleY = (float) startHeight / dpToPx(SIDEBAR_HEIGHT);

        // 先设置大小
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.width = dpToPx(SIDEBAR_WIDTH);
        lp.height = dpToPx(SIDEBAR_HEIGHT);
        setLayoutParams(lp);

        if (isOnLeftSide) {
            setPivotX(0f); // 水平方向以左边缘为基准（向左缩小）
        } else {
            setPivotX(dpToPx(SIDEBAR_WIDTH)); // 水平方向以右边缘为基准 (向右缩小)
        }
        setPivotY(0f); // 垂直方向以顶部为基准（向上缩小）

        // container 的隐藏
        if (mContainerView != null) {
            mContainerView.setAlpha(0f);
        }
        // 然后立马放大
        setScaleX(targetScaleX);
        setScaleY(targetScaleY);
        Log.d(TAG, "targetScaleX: " + targetScaleX + ", targetScaleY: " + targetScaleY);

        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(300);
        currentAnimator.setInterpolator(new FastOutSlowInInterpolator());
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
                if (listener != null)
                    listener.onFinished();
                isShowContainer = false;
                mLineBackgroundDrawable.setGlassMode(false);
                mLineBackgroundDrawable.setCornerRadius(dpToPx(mRadius));
                mContainerView.recyclerViewScrollToPosition(0);
            }
        });
        currentAnimator.addUpdateListener(animation -> {
            float f = (float) animation.getAnimatedFraction();

            setScaleX(targetScaleX + (1 - targetScaleX) * f);
            setScaleY(targetScaleY + (1 - targetScaleY) * f);
            // Y 偏移
            setTranslationY(startTranslationY + (lineY - startTranslationY) * f);
            // // 设置圆角，expandRadius->mRadius
            // float currentRaduis = dpToPx(expandRadius) - ((dpToPx(expandRadius) -
            // dpToPx(mRadius)) * f);
            // mLineBackgroundDrawable.setCornerRadius(currentRaduis);
            // invalidateOutline();
        });

        currentAnimator.start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Log.d(TAG, "LineView dispatchTouchEvent: " + ev.getAction());
        return super.dispatchTouchEvent(ev);
    }

    // 判断当line时需要这个点击事件而展示container模式时不允许介入
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isShowContainer)
            return true;
        return false;
    }
}