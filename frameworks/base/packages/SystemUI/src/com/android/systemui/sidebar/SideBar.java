package com.android.systemui.sidebar;

import com.android.systemui.util.ViewController;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.view.Display;
import android.view.ViewTreeObserver;

import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.android.systemui.R;
import com.android.systemui.shared.navigationbar.RegionSamplingHelper;
import com.android.systemui.shared.navigationbar.RegionSamplingHelper.SamplingCallback;
import android.graphics.Rect;
import com.android.systemui.sidebar.SideBarComponent.SideBarScope;
import com.android.systemui.sidebar.SideBarLineView.AnimationListener;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.Background;

import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;

import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Gravity;

import android.util.Log;
import android.graphics.Color;
import java.util.concurrent.Executor;

import javax.inject.Inject;

@SideBarScope
public class SideBar extends ViewController<SideBarView> {
    private static final String TAG = "SideBar";
    private static final boolean DEBUG = false;
    private final Context mContext;
    private WindowManager mWindowManager;

    private WindowManager.LayoutParams mLayoutParams;
    private FrameLayout.LayoutParams mBarlineLayoutParams;
    private SideBarLineView mBarLine;
    private SideBarShotCutContainerView mShotCutContainer;

    private RegionSamplingHelper mRegionSamplingHelper;
    private SideBarTransitions mSideBarTransitions;
    private ViewTreeObserver.OnComputeInternalInsetsListener mInsetsListener;

    private boolean isExpanded = false;
    private boolean islongPressed = false;
    private boolean isOnLeftSide = true;
    private boolean isLineOnTheTop = true;
    private boolean isShowContainer = false;
    private boolean isAnimating = false;

    private Runnable mLongPressRunable;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private final int maxWindowWidth = 60;
    private final int maxWindowHeight = 320;
    private final int minWindowWidth = 6;
    private final int minWindowHeight = 70;
    private final int mEdgeMarginlf;
    private final int mEdgeMargintb;
    private final int mScreenHeight;
    private final int mScreenWidth;
    // y轴移动范围, x移动范围为手指点到屏幕中心线的距离, 现在这个也对应了line在mview中位置范围
    private final int rangeY = 40;
    private final int SIDEBAR_LINE_HEIGHT = 70;

    // last down location
    private float mDownY;
    private float mDownX;

    // mview(window) location
    private int windowX;
    private int windowY;
    // line location
    private int lineX;
    private int lineY = 40;

    private final int mTouchSlop;

    @Inject
    SideBar(Context context,
            SideBarView sideBarView,
            SideBarLineView barlineView,
            @Main Executor mainExecutor,
            @Background Executor bgExecutor,
            SideBarShotCutContainerView shotCutContainerView,
            SideBarTransitions sideBarTransitions) {
        super(sideBarView);
        mBarLine = barlineView;
        mShotCutContainer = shotCutContainerView;
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mSideBarTransitions = sideBarTransitions;

        // 最小滑动像素
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        // assign values to the final variables
        Display display = mContext.getDisplay();
        Point size = new Point();
        display.getRealSize(size);
        mScreenHeight = size.y;
        mScreenWidth = size.x;
        mEdgeMarginlf = dpToPx(8);
        mEdgeMargintb = dpToPx(40);

        // bg亮度识别
        mRegionSamplingHelper = new RegionSamplingHelper(mView,
                new SamplingCallback() {
                    @Override
                    public void onRegionDarknessChanged(boolean isRegionDark) {
                        Log.d(TAG, "isRegionDark: " + isRegionDark);
                        mSideBarTransitions.getLightBarTransitionsController().setIconsDark(!isRegionDark, true);
                    }

                    @Override
                    public Rect getSampledRegion(View sampledView) {
                        return calculateSamplingRect();
                    }

                    @Override
                    public boolean isSamplingEnabled() {
                        return true;
                    }
                }, mainExecutor, bgExecutor);
        // 设置触控模式
        mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() {
            @Override
            public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo insets) {
                // 设置触控模式为区域模式
                insets.setTouchableInsets(ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_REGION);

                // 清空并设置新的区域
                insets.touchableRegion.setEmpty();

                int[] loc = new int[2];
                mBarLine.getLocationInWindow(loc);

                Rect touchRect = new Rect(loc[0], loc[1], loc[0] + mBarLine.getWidth(), loc[1] + mBarLine.getHeight());
                // 定义 line 的区域
                insets.touchableRegion.set(touchRect);
                Log.d("SideBar", "Width: " + mBarLine.getWidth() + " Height: " + mBarLine.getHeight());
                Log.d(TAG, "rect.top: " + touchRect.top + ", rect.bottom: " + touchRect.bottom + ", rect.left: "
                        + touchRect.left + ", rect.right: " + touchRect.right);
            }
        };
    }

    @Override
    protected void onInit() {
        // mView.setBackground(null);
        mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mWindowManager.addView(mView, createLayoutParams());
        Log.d(TAG, "First SideBar attached to window");
    }

    @Override
    protected void onViewAttached() {
        Log.d(TAG, "onViewAttached 被调用");
        mRegionSamplingHelper.setWindowVisible(true);
        mView.post(() -> {
            mRegionSamplingHelper.start(calculateSamplingRect());
            Log.d(TAG, "RegionSamplingHelper started");
        });
        mBarLine.initLineWH();
        lineY = dpToPx(rangeY);
        mShotCutContainer.initContainerHW();
        mShotCutContainer.setOnAppClickListener(appInfo -> {
            shrinkToLine();
            try {
                LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                launcherApps.startMainActivity(appInfo.getComponentName(), appInfo.getUser(), null, null);
            } catch (Exception e) {
                Log.e(TAG, "launch failed", e);
            }
        });
        // mShotCutContainer.setClickable(false);
        // mShotCutContainer.setEnabled(false);
        initTouch();
        // 注册区域触摸
        setupTouchableRegion();
    }

    @Override
    protected void onViewDetached() {
        mRegionSamplingHelper.setWindowVisible(false);
        mRegionSamplingHelper.stopAndDestroy();
        Log.d(TAG, "RegionSamplingHelper stopped and destroyed");
        mWindowManager.removeView(mView);
        removeTouchableRegion();
    }

    private WindowManager.LayoutParams createLayoutParams() {
        windowY = (int) mScreenHeight / 7;
        // lineY = windowY + dpToPx(rangeY);
        windowX = mEdgeMarginlf;
        mLayoutParams = new WindowManager.LayoutParams(
                dpToPx(maxWindowWidth),
                dpToPx(maxWindowHeight),
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // 在line下是无焦点，当在container状态下时即有焦点
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // 传递点击事件
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // 点击外部传递点击信息给该view
                PixelFormat.RGBA_8888);
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.x = mEdgeMarginlf;
        mLayoutParams.y = windowY;
        mLayoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        mLayoutParams.setTitle("SystemSideBar");
        return mLayoutParams;
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density + 0.5f);
    }

    private Rect calculateSamplingRect() {
        int margin = dpToPx(4);
        int[] pos = new int[2];
        mBarLine.getLocationOnScreen(pos);
        Rect rect = new Rect(pos[0] - margin, pos[1],
                pos[0] + mBarLine.getWidth() + margin,
                pos[1] + mBarLine.getHeight());
        // Log.d(TAG, "rect.left: " + rect.left + ", rect.top: " + rect.top + ",
        // rect.right: " + rect.right
        // + ", rect.bottom: " + rect.bottom);
        return rect;
    }

    private void initTouch() {
        mBarLine.setOnTouchListener((v, event) -> {
            // 处理触摸事件
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 按下按钮
                    mDownY = event.getRawY();
                    mDownX = event.getRawX();
                    mLongPressRunable = () -> {
                        if (isShowContainer)
                            return;
                        setExpanded(true);
                        islongPressed = true;
                    };
                    mHandler.postDelayed(mLongPressRunable, 1000); // 长按1000ms后执行扩展宽度
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDownX = event.getRawX();
                    float newDownY = event.getRawY();
                    if (!islongPressed) {
                        if (mLongPressRunable != null)
                            mHandler.removeCallbacks(mLongPressRunable);
                    }
                    // 滑动展开container
                    ifExpandToContainer(newDownX);
                    // 移动按钮
                    if (islongPressed) {
                        dragSideBar(newDownX, newDownY);// 如果没有展开，不处理移动事件
                    }

                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // judge show container
                    if (isShowContainer) {
                        // update locationX
                        updateXPosition(mEdgeMarginlf);
                        windowX = mEdgeMarginlf;
                    }
                    // show container是一个可以存在的状态，而expandAndDrag脱离了按键就恢复原貌
                    // judge the expand,then Drag status
                    // 松开按钮或取消,
                    if (mLongPressRunable != null) {
                        mHandler.removeCallbacks(mLongPressRunable); // 取消长按任务
                    }
                    if (islongPressed) {
                        islongPressed = false;
                        setExpanded(false);
                        updateXPosition(mEdgeMarginlf); // reset to the edge after releasing
                        windowX = mEdgeMarginlf;
                    }

                    break;
            }
            return true;
        });
        mView.setOnTouchListener((v, event) -> {
            // switch (event.getAction()) {
            // case MotionEvent.ACTION_OUTSIDE:
            // Log.d(TAG, "in action_outside");
            // if (isShowContainer) {
            // shrinkToLine();
            // Log.d(TAG, "shrinkToLine");
            // isShowContainer = false;
            // }
            // return true;
            // }
            // return false;
            if (isShowContainer) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 点击 container 外的区域
                    shrinkToLine();
                    Log.d(TAG, "shrinkToline");
                }
                return true; // 拦截所有点击
            }
            return false;
        });
    }

    private void ifExpandToContainer(float newDownX) {
        Log.d(TAG, "no filter isAnimating: " + isAnimating);
        if (isAnimating) {
            return;
        }
        Log.d(TAG, "first filter isAnimating: " + isAnimating);
        float s = newDownX - mDownX;
        // Log.d(TAG, "isExpanded: " + isExpanded + ", isLongPressed: " +
        // islongPressed);
        if (Math.abs(s) < mTouchSlop || isExpanded)
            return;
        if (!isShowContainer) {
            expandToWindow();
        } else {
            if (isOnLeftSide && s > 0 || !isOnLeftSide && s < 0) {
                // 更新太快卡顿，不使用
                // 左侧展开强制向右滑动,则给出像左侧移动的强制假象
                // windowX += (int) (s / 50);
                // updateXPosition(windowX);
            } else if (isOnLeftSide && s < 0 || !isOnLeftSide && s > 0) {
                // 向左滑动收起左侧框,这里指的是在还有焦点的时候左滑收起
                // 向右滑动收起右侧框,同上逻辑
                shrinkToLine();
            }
        }
        mDownX = newDownX;
    }

    private void expandToWindow() {
        // judge again
        if (isAnimating)
            return;
        Log.d(TAG, "second filter isAnimating: " + isAnimating);
        isAnimating = true;
        // 暂停采集
        mRegionSamplingHelper.stop();
        // mBarLine.setBackgroundColor(Color.TRANSPARENT);
        mBarLine.startExpandToWindowOptimization(dpToPx(maxWindowHeight), dpToPx(maxWindowWidth), isOnLeftSide,
                new AnimationListener() {
                    @Override
                    public void onFinished() {
                        Log.d(TAG, "onfinished set isAnimating as false");
                        isShowContainer = true;
                        isAnimating = false;
                        switchFlag(false);
                    }

                });
    }

    private void shrinkToLine() {
        // judge again
        if (isAnimating)
            return;
        Log.d(TAG, "second filter isAnimating: " + isAnimating);
        isAnimating = true;
        mBarLine.startShrinkToLineOptimization(lineY, isOnLeftSide, new AnimationListener() {
            @Override
            public void onFinished() {
                Log.d(TAG, "onfinished set isAnimating as false");
                isAnimating = false;
                isShowContainer = false;
                // 开始采集
                mRegionSamplingHelper.start(calculateSamplingRect());
                switchFlag(true);
            }

        });
    }

    // 拖动sidebarline逻辑
    private void dragSideBar(float newDownX, float newDownY) {
        if (newDownX >= mScreenWidth / 2 && isOnLeftSide || newDownX <= mScreenWidth / 2 && !isOnLeftSide) {
            // 如果跨过屏幕中心线，切换侧边
            isOnLeftSide = !isOnLeftSide;
            switchSide(isOnLeftSide);
            Log.d(TAG, "switch side: " + (isOnLeftSide ? "left" : "right"));
        } else if (newDownX >= mScreenWidth / 4 && newDownX <= mScreenWidth * 3 / 4) {
            // 在1/4-3/4的屏幕内小幅度移动，而超出的更大幅度移动
            if (isOnLeftSide) {
                windowX += (int) ((newDownX - mDownX) / 7);
                updateXPosition(windowX);
            } else {
                windowX += (int) ((mDownX - newDownX) / 7);
                updateXPosition(windowX);
            }
        } else {
            if (isOnLeftSide) {
                windowX += (int) ((newDownX - mDownX) / 2);
                if (windowX <= mEdgeMarginlf) {
                    windowX = mEdgeMarginlf;
                }
                updateXPosition(windowX);
            } else {
                windowX += (int) ((mDownX - newDownX) / 2);
                if (windowX <= mEdgeMarginlf) {
                    windowX = mEdgeMarginlf;
                }
                updateXPosition(windowX);
            }
        }
        mDownX = newDownX;

        int dy = (int) (newDownY - mDownY);
        mDownY = newDownY;
        // 向上拖
        if (dy < 0) {
            if (lineY > dpToPx(rangeY)) {
                // line 还没到顶部 → 先动 line
                lineY += dy;

                if (lineY < dpToPx(rangeY)) {
                    lineY = dpToPx(rangeY);
                }

                mBarLine.setTranslationY(lineY);

            } else {
                // line 已经在顶部 → 开始动 window
                windowY += dy;

                if (windowY < dpToPx(rangeY)) {
                    windowY = dpToPx(rangeY);
                }

                updateYPosition(windowY);
            }
        }
        // 向下拖
        else {
            // window 没到底
            if ((windowY + dpToPx(maxWindowHeight)) < mScreenHeight - dpToPx(10)) {
                // window 还能往下,先动 window
                windowY += dy;

                // 如果新windowy超出底部则设置为底部
                if ((windowY + dpToPx(maxWindowHeight)) > mScreenHeight - dpToPx(10)) {
                    windowY = mScreenHeight - dpToPx(10) - dpToPx(maxWindowHeight);
                }

                updateYPosition(windowY);

            } else {
                // window 到底了,开始动 line
                lineY += dy;

                // line的最底部
                int maxLineY = dpToPx(maxWindowHeight) - dpToPx(rangeY) - dpToPx(SIDEBAR_LINE_HEIGHT);

                if (lineY > maxLineY) {
                    lineY = maxLineY;
                }

                mBarLine.setTranslationY(lineY);
            }
        }
    }

    // 在初始化或者 onAttachedToWindow 时注册
    private void setupTouchableRegion() {
        mBarLine.getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsListener);
    }

    // 在 onDetachedFromWindow 时移除，防止内存泄漏
    private void removeTouchableRegion() {
        mBarLine.getViewTreeObserver().removeOnComputeInternalInsetsListener(mInsetsListener);
    }

    // set expanded, if isExpanded is true, set width to the bigger one, otherwise
    // set to the smaller one
    private void setExpanded(boolean expanded) {
        if (expanded == isExpanded) {
            return; // 如果状态没有改变，直接返回
        }
        isExpanded = expanded;
        mBarLine.setExpanded(isExpanded);
        updateSamplingRect();
        Log.d(TAG, "setExpanded: " + isExpanded + ", width: " + mBarLine.getLayoutParams().width);
    }

    private void switchFlag(boolean isFlag) {
        if (isFlag) {
            // 不抢焦点
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        } else {
            // 拿到焦点
            mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        }
        applyLayoutParams();
    }

    private void updateYPosition(int newY) {
        mLayoutParams.y = newY;
        applyLayoutParams();
    }

    private void updateXPosition(int newX) {
        mLayoutParams.x = newX;
        applyLayoutParams();
    }

    private void switchSide(boolean toLeft) {
        mLayoutParams.gravity = toLeft ? (Gravity.LEFT | Gravity.TOP) : (Gravity.RIGHT | Gravity.TOP);
        mBarLine.setSwitchSide(toLeft);
        applyLayoutParams();
    }

    private void applyLayoutParams() {
        mWindowManager.updateViewLayout(mView, mLayoutParams);
        mRegionSamplingHelper.updateSamplingRect();
    }

    private void updateSamplingRect() {
        mRegionSamplingHelper.updateSamplingRect();
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mView.setVisibility(View.GONE);
    }
}