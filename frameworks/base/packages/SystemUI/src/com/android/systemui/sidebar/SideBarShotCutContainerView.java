package com.android.systemui.sidebar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.widget.RecyclerView;
import com.android.internal.widget.RecyclerView.State;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.android.internal.widget.LinearLayoutManager;
import android.view.Gravity;
import android.os.UserHandle;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;
import android.os.Process;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.graphics.drawable.LayerDrawable;

public class SideBarShotCutContainerView extends FrameLayout {
    private static final String TAG = "SideBarShotCutContainerView";
    private static final boolean DEBUG = false;
    private RecyclerView mRecyclerView;
    private final int INIT_WIDTH = 6;
    private final int INIT_HEIGHT = 70;
    private final int CONTAINER_WIDTH = 50;
    private final int CONTAINER_HEIGHT = 300;
    private final Context mContext;
    // private List<Drawable> mIcons;
    private List<LauncherActivityInfo> apps;
    private OnAppClickListener listener;

    public SideBarShotCutContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void initContainerHW() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dpToPx(10));
        if (DEBUG)
            drawable.setColor(Color.BLACK);

        setBackground(drawable);

        mRecyclerView = new RecyclerView(mContext);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false));
        if (DEBUG) {
            GradientDrawable drawable1 = new GradientDrawable();
            drawable1.setColor(Color.YELLOW);
            drawable1.setCornerRadius(dpToPx(35));
            mRecyclerView.setBackground(drawable1);
        } else {
            GradientDrawable drawableR = new GradientDrawable();
            drawableR.setCornerRadius(dpToPx(10));
            // drawableR.setColor(Color.YELLOW);
            mRecyclerView.setBackground(drawableR);
        }
        addView(mRecyclerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override

            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
                super.getItemOffsets(outRect, view, parent, state);
                RecyclerView.Adapter adapter = parent.getAdapter();
                if (adapter == null)
                    return;
                int position = parent.getChildAdapterPosition(view);
                int itemCount = adapter.getItemCount();

                if (position == RecyclerView.NO_POSITION)
                    return;
                // first icon 距离顶部多设置8dp
                if (position == 0) {
                    outRect.top = dpToPx(8);
                }
                // last icon 距离底部多设置8dp
                if (position == itemCount - 1) {
                    outRect.bottom = dpToPx(8);
                }
            }
        });
        loadsIcon();
    }

    private void loadsIcon() {
        // get apps information

        LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        apps = launcherApps.getActivityList(null,
                Process.myUserHandle());
        Log.d(TAG, "apps: " + apps.size());
        // mIcons = new ArrayList<>();
        // for (LauncherActivityInfo appInfo : apps) {
        // Log.d(TAG, "app: " + appInfo.getComponentName().getPackageName());
        // Drawable icon = appInfo.getBadgedIcon(0);
        // if (icon != null)
        // mIcons.add(wrapIconWithWhiteBackground(icon));
        // }
        // Log.d(TAG, "mIcons size: " + mIcons.size());
        mRecyclerView.setAdapter(new AppAdapter());
    }

    /**
     * 拿到只有icon，为没有颜色的icon添加白色底座
     * 
     * @param icon
     */
    private LayerDrawable wrapIconWithWhiteBackground(Drawable icon) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.WHITE);
        background.setCornerRadius(dpToPx(10));
        Drawable[] layers = { background, icon };
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        int inset = 0;
        layerDrawable.setLayerInset(1, inset, inset, inset, inset);

        return layerDrawable;
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
        private class Holder extends RecyclerView.ViewHolder {
            ImageView icon;

            Holder(FrameLayout rootItem, ImageView iconView) {
                super(rootItem);
                icon = iconView;
            }
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            // item中先设置一个layout当作item，里面内容可以做到水平居中
            FrameLayout itemRoot = new FrameLayout(getContext());
            RecyclerView.LayoutParams lRootp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            itemRoot.setLayoutParams(lRootp);
            ImageView imageView = new ImageView(parent.getContext());
            int size = dpToPx(36);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            int margin = dpToPx(8);
            lp.setMargins(0, margin, 0, margin);
            imageView.setLayoutParams(lp);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            itemRoot.addView(imageView);
            return new Holder(itemRoot, imageView);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            LauncherActivityInfo appInfo = apps.get(position);

            Drawable icon = appInfo.getBadgedIcon(0);
            holder.icon.setImageDrawable(wrapIconWithWhiteBackground(icon));
            holder.icon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.launchApp(appInfo);
                }
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * mContext.getResources().getDisplayMetrics().density +
                0.5f);
    }

    private void launchApp(LauncherActivityInfo appInfo) {
        try {
            LauncherApps launcherApps = (LauncherApps) mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            launcherApps.startMainActivity(appInfo.getComponentName(), appInfo.getUser(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "launch failed", e);
        }
    }

    public interface OnAppClickListener {
        public void launchApp(LauncherActivityInfo appInfo);
    }

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent: " + ev.getAction());
        return super.dispatchTouchEvent(ev);
    }

    public void recyclerViewScrollToPosition(int position) {
        mRecyclerView.scrollToPosition(position);
    }
}
