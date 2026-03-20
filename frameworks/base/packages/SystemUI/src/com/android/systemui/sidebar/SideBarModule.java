package com.android.systemui.sidebar;

import com.android.systemui.sidebar.SideBarComponent.SideBarScope;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import com.android.systemui.dagger.qualifiers.DisplayId;
import com.android.systemui.R;
import android.view.View;
import android.view.LayoutInflater;

/** Module for {@link com.android.systemui.sidebar.SideBarComponent}. */
@Module
public interface SideBarModule {

    @Provides
    @SideBarScope
    static SideBarView providSideBarView(Context context) {
        return (SideBarView) LayoutInflater.from(context).inflate(R.layout.side_bar, null);
    }

    @Provides
    @SideBarScope
    static SideBarLineView provideBarLine(SideBarView sideBarView) {
        return sideBarView.findViewById(R.id.side_bar_line);
    }

    @Provides
    @SideBarScope
    static SideBarShotCutContainerView provideShotCutContainer(SideBarView sideBarView) {
        return sideBarView.findViewById(R.id.side_bar_short_cut_container);
    }
}
