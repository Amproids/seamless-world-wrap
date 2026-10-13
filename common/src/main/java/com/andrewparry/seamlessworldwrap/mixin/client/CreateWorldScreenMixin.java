package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.WorldWrapState;
import com.andrewparry.seamlessworldwrap.client.WorldWrapDraft;
import com.andrewparry.seamlessworldwrap.client.WorldWrapTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/** Adds the World Wrap tab, and hands its choices to the server when the world is created. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void seamless$freshDraft(CallbackInfo ci) {
        WorldWrapDraft.reset();
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"))
    private Tab[] seamless$addTab(Tab[] tabs) {
        Tab[] withWrap = Arrays.copyOf(tabs, tabs.length + 1);
        withWrap[tabs.length] = new WorldWrapTab();
        return withWrap;
    }

    @Inject(method = "onCreate", at = @At("HEAD"))
    private void seamless$useDraft(CallbackInfo ci) {
        WorldWrapState.setPendingNewWorld(WorldWrapDraft.current().toOptions());
    }
}
