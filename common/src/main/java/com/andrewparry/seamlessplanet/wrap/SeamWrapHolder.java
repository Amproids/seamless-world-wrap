package com.andrewparry.seamlessplanet.wrap;

import com.andrewparry.seamlessplanet.WrapSettings;
import org.jetbrains.annotations.Nullable;

/** Added by mixin to vanilla objects that have no level reference but need to know the wrap. */
public interface SeamWrapHolder {

    @Nullable
    WrapSettings seamless$wrap();

    void seamless$setWrap(@Nullable WrapSettings wrap);
}
