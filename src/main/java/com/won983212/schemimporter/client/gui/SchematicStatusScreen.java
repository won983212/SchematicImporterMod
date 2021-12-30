package com.won983212.schemimporter.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.schemimporter.schematic.IProgressEntry;
import com.won983212.schemimporter.schematic.IProgressEntryProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

public class SchematicStatusScreen extends Screen {
    private final List<IProgressEntryProducer> progressProducers;

    public SchematicStatusScreen() {
        super(new StringTextComponent("Schematic Upload Status"));
        this.progressProducers = new ArrayList<>();
    }

    public void registerProgressProducer(IProgressEntryProducer producer) {
        progressProducers.add(producer);
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float tick) {
        int size = 0;
        for (IProgressEntryProducer producer : progressProducers) {
            size += producer.size();
        }
        if (size > 0) {
            renderProgressList(ms, mx, my, tick);
        }
    }

    private void renderProgressList(MatrixStack ms, int mx, int my, float tick) {
        FontRenderer font = Minecraft.getInstance().font;

        int x = 8;
        int y = 8;
        final int gap = 4;
        final int width = 140;

        for (IProgressEntryProducer producer : progressProducers) {
            for (IProgressEntry ent : producer.getProgressEntries()) {
                String title = ent.getTitle();
                String subtitle = ent.getSubtitle();
                float progressWidth = (float) (ent.getProgress() * (width - 2 * gap));

                title = PanelScreen.ellipsisText(font, title, width - 2 * gap);
                subtitle = PanelScreen.ellipsisText(font, subtitle, width - 2 * gap);

                fill(ms, x, y, x + width, y + gap * 4 + 20, 0xaa000000);
                font.drawShadow(ms, title, x + gap, y + gap, 0xffffffff);

                y += gap + 12;
                font.drawShadow(ms, subtitle, x + gap, y, 0xffaaaaaa);
                fill(ms, x + gap, y + 12, x + width - gap, y + 15, 0xff333333);
                PanelScreen.fillFloat(ms, x + gap, y + 12, x + gap + progressWidth, y + 15, 0xff396EB0);

                y += 22;
            }
        }
    }
}
