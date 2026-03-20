package com.android.systemui.sidebar;

import com.android.systemui.statusbar.CommandQueue.Callbacks;
import android.content.Context;
import android.content.ComponentName;
import com.android.systemui.dagger.SysUISingleton;

import java.util.List;

import javax.inject.Inject;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.shared.system.TaskStackChangeListeners;
import android.app.ActivityTaskManager;
import android.app.ActivityManager;
import android.util.Log;
import com.android.systemui.statusbar.phone.panelstate.PanelExpansionStateManager;
import com.android.systemui.statusbar.phone.panelstate.PanelExpansionListener;

@SysUISingleton
public class SideBarController implements Callbacks {
    private String TAG = "SideBarController";

    private final Context mContext;
    private final SideBarComponent.Factory mSideBarComponentFactory;
    private final PanelExpansionStateManager mPanelExpansionStateManager;
    private final PanelExpansionListener mPanelExpansionListener;
    private SideBar mSideBar;
    private boolean isPanelExpanded = false;

    @Inject
    public SideBarController(Context context, SideBarComponent.Factory sideBarComponentFactory,
            PanelExpansionStateManager panelExpansionStateManager) {
        mContext = context;
        mSideBarComponentFactory = sideBarComponentFactory;
        mPanelExpansionStateManager = panelExpansionStateManager;
        mPanelExpansionListener = event -> {
            // event.getFraction() 是拉开的比例：0.0 是关闭，1.0 是全开
            // event.getExpanded() 是布尔值：是否处于展开状态
            isPanelExpanded = event.getFraction() > 0f;
            updateVisibility();
        };
    }

    public void createSideBar() {
        SideBarComponent component = mSideBarComponentFactory.create();
        mSideBar = component.getSideBar();
        mSideBar.init();
        TaskStackChangeListeners.getInstance()
                .registerTaskStackListener(new TaskStackChangeListener() {
                    @Override
                    public void onTaskStackChanged() {
                        updateVisibility();
                    }

                    @Override
                    public void onTaskCreated(int taskId, ComponentName componentName) {
                        updateVisibility();
                    }

                    @Override
                    public void onTaskMovedToFront(int taskId) {
                        updateVisibility();
                    }
                });
        mPanelExpansionStateManager.addExpansionListener(mPanelExpansionListener);
        // updateVisibility();
    }

    private void updateVisibility() {
        if (isPanelExpanded) {
            mSideBar.hide();
            return;
        }
        ComponentName top = getTopActivity();
        if (isLauncher(top)) {
            mSideBar.show();
        } else {
            mSideBar.hide();
        }
    }

    private ComponentName getTopActivity() {
        ActivityTaskManager atm = mContext.getSystemService(ActivityTaskManager.class);
        List<ActivityManager.RunningTaskInfo> tasks = atm.getTasks(1);
        if (tasks != null && !tasks.isEmpty()) {
            return tasks.get(0).topActivity;
        }
        return null;
    }

    private boolean isLauncher(ComponentName cn) {
        if (cn == null)
            return false;
        return "com.android.launcher3".equals(cn.getPackageName());
    }
}