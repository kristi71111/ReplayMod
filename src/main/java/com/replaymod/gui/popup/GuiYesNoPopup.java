/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.gui.popup;

import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiPanel;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.layout.VerticalLayout;
import com.replaymod.gui.utils.Colors;
import com.replaymod.gui.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

import java.util.function.Consumer;

public class GuiYesNoPopup extends AbstractGuiPopup<GuiYesNoPopup> implements Typeable {
    private final com.replaymod.gui.container.GuiPanel info = new com.replaymod.gui.container.GuiPanel().setMinSize(new Dimension(320, 50))
            .setLayout(new com.replaymod.gui.layout.VerticalLayout(com.replaymod.gui.layout.VerticalLayout.Alignment.TOP).setSpacing(2));
    private Consumer<Boolean> onClosed = (accepted) -> {
    };
    private Runnable onAccept = () -> {
    };
    private final com.replaymod.gui.element.GuiButton yesButton = new com.replaymod.gui.element.GuiButton().setSize(150, 20).onClick(new Runnable() {
        @Override
        public void run() {
            close();
            onAccept.run();
            onClosed.accept(true);
        }
    });
    private Runnable onReject = () -> {
    };
    private final com.replaymod.gui.element.GuiButton noButton = new com.replaymod.gui.element.GuiButton().setSize(150, 20).onClick(new Runnable() {
        @Override
        public void run() {
            close();
            onReject.run();
            onClosed.accept(false);
        }
    });
    private final com.replaymod.gui.container.GuiPanel buttons = new com.replaymod.gui.container.GuiPanel()
            .setLayout(new com.replaymod.gui.layout.HorizontalLayout(com.replaymod.gui.layout.HorizontalLayout.Alignment.CENTER).setSpacing(5))
            .addElements(new HorizontalLayout.Data(0.5), yesButton, noButton);
    private int layer;

    {
        popup.setLayout(new com.replaymod.gui.layout.VerticalLayout().setSpacing(10))
                .addElements(new VerticalLayout.Data(0.5), info, buttons);
    }

    public GuiYesNoPopup(GuiContainer container) {
        super(container);
    }

    public static GuiYesNoPopup open(com.replaymod.gui.container.GuiContainer container, GuiElement... info) {
        GuiYesNoPopup popup = new GuiYesNoPopup(container).setBackgroundColor(Colors.DARK_TRANSPARENT);
        popup.getInfo().addElements(new com.replaymod.gui.layout.VerticalLayout.Data(0.5), info);
        popup.open();
        return popup;
    }

    public GuiYesNoPopup setYesLabel(String label) {
        yesButton.setLabel(label);
        return this;
    }

    public GuiYesNoPopup setNoLabel(String label) {
        noButton.setLabel(label);
        return this;
    }

    public GuiYesNoPopup setYesI18nLabel(String label, Object... args) {
        yesButton.setI18nLabel(label, args);
        return this;
    }

    public GuiYesNoPopup setNoI18nLabel(String label, Object... args) {
        noButton.setI18nLabel(label, args);
        return this;
    }

    public GuiYesNoPopup onClosed(Consumer<Boolean> onClosed) {
        this.onClosed = onClosed;
        return this;
    }

    public GuiYesNoPopup onAccept(Runnable onAccept) {
        this.onAccept = onAccept;
        return this;
    }

    public GuiYesNoPopup onReject(Runnable onReject) {
        this.onReject = onReject;
        return this;
    }

    @Override
    protected GuiYesNoPopup getThis() {
        return this;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == MCVer.Keyboard.KEY_ESCAPE) {
            noButton.onClick();
            return true;
        }
        return false;
    }

    public com.replaymod.gui.element.GuiButton getYesButton() {
        return this.yesButton;
    }

    public GuiButton getNoButton() {
        return this.noButton;
    }

    public com.replaymod.gui.container.GuiPanel getInfo() {
        return this.info;
    }

    public GuiPanel getButtons() {
        return this.buttons;
    }

    public int getLayer() {
        return this.layer;
    }

    public GuiYesNoPopup setLayer(int layer) {
        this.layer = layer;
        return this;
    }
}
