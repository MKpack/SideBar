package com.android.systemui.sidebar;

import com.android.systemui.sidebar.SideBarComponent.SideBarScope;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;
import com.android.systemui.statusbar.phone.BarTransitions;
import com.android.systemui.R;
import javax.inject.Inject;

@SideBarScope
public final class SideBarTransitions implements
        LightBarTransitionsController.DarkIntensityApplier {
    private static final String TAG = "SideBarTransitions";

    private final SideBarLineView mLineView;
    private final LightBarTransitionsController mLightBarTransitionsController;

    @Inject
    public SideBarTransitions(
            SideBarLineView view,
            LightBarTransitionsController.Factory lightBarTransitionsControllerFactory) {
        mLineView = view;
        mLightBarTransitionsController = lightBarTransitionsControllerFactory.create(this);
    }

    @Override
    public void applyDarkIntensity(float darkIntensity) {
        mLineView.setDarkIntensity(darkIntensity);
    }

    @Override
    public int getTintAnimationDuration() {
        return 0;
    }

    public LightBarTransitionsController getLightBarTransitionsController() {
        return mLightBarTransitionsController;
    }

}
