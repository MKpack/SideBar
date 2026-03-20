package com.android.systemui.sidebar;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import javax.inject.Scope;

import dagger.BindsInstance;
import android.content.Context;
import dagger.Subcomponent;
import com.android.systemui.sidebar.SideBarComponent.SideBarScope;

import androidx.annotation.Nullable;
import android.os.Bundle;

@Subcomponent(modules = { SideBarModule.class })
@SideBarComponent.SideBarScope
public interface SideBarComponent {

    @Subcomponent.Factory
    interface Factory {
        SideBarComponent create();
    }

    SideBar getSideBar();

    @Documented
    @Retention(RUNTIME)
    @Scope
    @interface SideBarScope {
    }
}
