package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import org.jetbrains.annotations.Nullable;

/** Added by mixin to vanilla objects that have no level reference but need to know the wrap. */
public interface SeamWrapHolder {

    @Nullable
    WrapSettings seamless$wrap();

    void seamless$setWrap(@Nullable WrapSettings wrap);
}
