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
import com.android.systemui.statusbar.policy.KeyguardStateController;

@SysUISingleton
public class SideBarController implements Callbacks {
    private String TAG = "SideBarController";

    private final Context mContext;
    private final SideBarComponent.Factory mSideBarComponentFactory;
    private final PanelExpansionStateManager mPanelExpansionStateManager;
    private final PanelExpansionListener mPanelExpansionListener;
    private final KeyguardStateController mKeyguardStateController;
    private SideBar mSideBar;
    private boolean isPanelExpanded = false;

    @Inject
    public SideBarController(Context context, SideBarComponent.Factory sideBarComponentFactory,
            PanelExpansionStateManager panelExpansionStateManager, KeyguardStateController keyguardStateController) {
        mContext = context;
        mSideBarComponentFactory = sideBarComponentFactory;
        mPanelExpansionStateManager = panelExpansionStateManager;
        mPanelExpansionListener = event -> {
            // event.getFraction() 是拉开的比例：0.0 是关闭，1.0 是全开
            // event.getExpanded() 是布尔值：是否处于展开状态
            isPanelExpanded = event.getFraction() > 0f;
            updateVisibility();
        };
        mKeyguardStateController = keyguardStateController;
    }

    public void createSideBar() {
        SideBarComponent component = mSideBarComponentFactory.create();
        mSideBar = component.getSideBar();
        mSideBar.init();
        mKeyguardStateController.addCallback(
                new KeyguardStateController.Callback() {
                    @Override
                    public void onKeyguardShowingChanged() {
                        updateVisibility();
                    }
                });
        mPanelExpansionStateManager.addExpansionListener(mPanelExpansionListener);
    }

    private void updateVisibility() {

        if (mKeyguardStateController.isShowing()
                || mKeyguardStateController.isOccluded()) {
            mSideBar.hide();
            return;
        }
        if (isPanelExpanded) {
            mSideBar.hide();
            return;
        }
        mSideBar.show();
    }
}