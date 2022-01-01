package com.won983212.schemimporter.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.schemimporter.client.gui.PanelScreen;

import java.util.List;

public class ScrollSelector extends AbstractComponent {
    private final List<?> elements;
    private final int viewCount;
    private int selectedIndex = 0;
    private int scrollIndex = 0;

    public ScrollSelector(int x, int y, int width, int height, List<?> elements) {
        super(x, y, width, height);
        this.elements = elements;
        this.viewCount = height / 18;
    }

    private void selectNext(int delta) {
        int max = elements.size();
        if (max == 0) {
            return;
        }

        selectedIndex -= delta;
        if (selectedIndex < 0) {
            selectedIndex += max;
        }
        if (selectedIndex >= max) {
            selectedIndex %= max;
        }
        if (scrollIndex > selectedIndex) {
            scrollIndex = selectedIndex;
        }
        if (scrollIndex + viewCount <= selectedIndex) {
            scrollIndex = selectedIndex - viewCount + 1;
        }
    }

    public int getSelectedIndex() {
        if (selectedIndex >= elements.size()) {
            selectedIndex = elements.size() - 1;
        }
        return selectedIndex;
    }

    @Override
    public void render(MatrixStack ms, int x, int y, float partialTime) {
        if (elements.isEmpty()) {
            drawString(ms, font, "아무것도 없음!", this.x + 5, this.y + (height - font.lineHeight) / 2 + 1, 0xffffffff);
            return;
        }
        for (int i = scrollIndex, j = 0; i < elements.size() && j < viewCount; i++, j++) {
            String str = elements.get(i).toString();
            str = (i + 1) + ": " + str;
            if (i == getSelectedIndex()) {
                str = "§6" + str;
            }
            str = PanelScreen.ellipsisText(font, str, width - 10);
            drawString(ms, font, str, this.x + 5, this.y + (18 - font.lineHeight) / 2 + 18 * j + 1, 0xffffffff);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        selectNext((int) delta);
        return true;
    }
}
