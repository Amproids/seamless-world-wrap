package com.andrewparry.seamlessworldwrap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** "World Wrap" tab on the Create World screen: Overworld settings on the left, the End on the right. */
public class WorldWrapTab extends GridLayoutTab {

    private static final int WIDGET_WIDTH = 150;
    /** World sizes offered, in chunks. */
    private static final int[] SIZES = {4, 8, 16, 32, 48, 64, 96, 128, 192, 256, 384, 512, 1024, 2048};

    private final List<AbstractWidget> endCustomWidgets = new ArrayList<>();

    public WorldWrapTab() {
        super(Component.translatable("seamless_world_wrap.tab.title"));
        WorldWrapDraft draft = WorldWrapDraft.current();
        Font font = Minecraft.getInstance().font;
        GridLayout.RowHelper rows = layout.columnSpacing(10).rowSpacing(6).createRowHelper(2);

        rows.addChild(new StringWidget(Component.translatable("seamless_world_wrap.overworld"), font));
        rows.addChild(new StringWidget(Component.translatable("seamless_world_wrap.end"), font));

        // Row: east-west | End mode
        rows.addChild(toggle("seamless_world_wrap.east_west", draft.overworld.wrapsX, v -> draft.overworld.wrapsX = v));
        rows.addChild(CycleButton.<WorldWrapDraft.EndMode>builder(WorldWrapDraft.EndMode::label)
                .withValues(WorldWrapDraft.EndMode.values())
                .withInitialValue(draft.endMode)
                .create(0, 0, WIDGET_WIDTH, 20, Component.translatable("seamless_world_wrap.end_mode"), (button, mode) -> {
                    draft.endMode = mode;
                    updateEndWidgets(draft);
                }));

        // Row: north-south | End east-west
        rows.addChild(toggle("seamless_world_wrap.north_south", draft.overworld.wrapsZ, v -> draft.overworld.wrapsZ = v));
        rows.addChild(endWidget(toggle("seamless_world_wrap.east_west", draft.end.wrapsX, v -> draft.end.wrapsX = v)));

        // Row: width | End north-south
        rows.addChild(new SizeSlider("seamless_world_wrap.width", draft.overworld.widthChunks, v -> draft.overworld.widthChunks = v));
        rows.addChild(endWidget(toggle("seamless_world_wrap.north_south", draft.end.wrapsZ, v -> draft.end.wrapsZ = v)));

        // Row: length | End width
        rows.addChild(new SizeSlider("seamless_world_wrap.length", draft.overworld.lengthChunks, v -> draft.overworld.lengthChunks = v));
        rows.addChild(endWidget(new SizeSlider("seamless_world_wrap.width", draft.end.widthChunks, v -> draft.end.widthChunks = v)));

        // Row: (empty) | End length
        rows.addChild(new StringWidget(Component.empty(), font));
        rows.addChild(endWidget(new SizeSlider("seamless_world_wrap.length", draft.end.lengthChunks, v -> draft.end.lengthChunks = v)));

        MultiLineTextWidget note = new MultiLineTextWidget(Component.translatable("seamless_world_wrap.note"), font);
        note.setMaxWidth(WIDGET_WIDTH * 2 + 10);
        note.setCentered(true);
        rows.addChild(note, 2);

        updateEndWidgets(draft);
    }

    private static CycleButton<Boolean> toggle(String key, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        return CycleButton.onOffBuilder(initial)
                .create(0, 0, WIDGET_WIDTH, 20, Component.translatable(key), (button, value) -> onChange.accept(value));
    }

    private AbstractWidget endWidget(AbstractWidget widget) {
        endCustomWidgets.add(widget);
        return widget;
    }

    /** End settings only matter (and can only be changed) when the End has its own settings. */
    private void updateEndWidgets(WorldWrapDraft draft) {
        boolean custom = draft.endMode == WorldWrapDraft.EndMode.CUSTOM;
        for (AbstractWidget widget : endCustomWidgets) {
            widget.active = custom;
        }
    }

    /** A slider that snaps to the offered world sizes and shows them in blocks. */
    private static final class SizeSlider extends AbstractSliderButton {
        private final String key;
        private final IntConsumer onChange;

        SizeSlider(String key, int initialChunks, IntConsumer onChange) {
            super(0, 0, WIDGET_WIDTH, 20, Component.empty(), indexOf(initialChunks) / (double) (SIZES.length - 1));
            this.key = key;
            this.onChange = onChange;
            updateMessage();
        }

        private static int indexOf(int chunks) {
            for (int i = 0; i < SIZES.length; i++) {
                if (SIZES[i] >= chunks) {
                    return i;
                }
            }
            return SIZES.length - 1;
        }

        private int chunks() {
            return SIZES[Mth.clamp((int) Math.round(value * (SIZES.length - 1)), 0, SIZES.length - 1)];
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key, String.format("%,d", chunks() * 16)));
        }

        @Override
        protected void applyValue() {
            onChange.accept(chunks());
        }
    }
}
