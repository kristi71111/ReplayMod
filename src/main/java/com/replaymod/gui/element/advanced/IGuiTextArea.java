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
package com.replaymod.gui.element.advanced;

import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.function.Focusable;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;

public interface IGuiTextArea<T extends IGuiTextArea<T>> extends GuiElement<T>, Focusable<T> {
    String[] getText();

    T setText(String[] lines);

    String getText(int fromX, int fromY, int toX, int toY);

    int getSelectionFromX();

    int getSelectionToX();

    int getSelectionFromY();

    int getSelectionToY();

    String getSelectedText();

    void deleteSelectedText();

    String cutSelectedText();

    void writeText(String append);

    void writeChar(char c);

    T setCursorPosition(int x, int y);

    T setTextColor(ReadableColor textColor);

    T setTextColorDisabled(ReadableColor textColorDisabled);

    int getMaxTextWidth();

    T setMaxTextWidth(int maxTextWidth);

    int getMaxTextHeight();

    T setMaxTextHeight(int maxTextHeight);

    int getMaxCharCount();

    T setMaxCharCount(int maxCharCount);

    String[] getHint();

    T setHint(String... hint);

    T setI18nHint(String hint, Object... args);
}
