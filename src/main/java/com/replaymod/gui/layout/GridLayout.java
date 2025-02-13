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
package com.replaymod.gui.layout;

import com.google.common.base.Preconditions;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.element.GuiElement;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class GridLayout implements Layout {
    private static final Data DEFAULT_DATA = new Data();

    private int columns;

    private int spacingX, spacingY;

    private boolean cellsEqualSize = true;

    @Override
    public Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> layOut(GuiContainer<?> container, ReadableDimension size) {
        Preconditions.checkState(columns != 0, "Columns may not be 0.");
        int elements = container.getElements().size();
        int rows = (elements - 1 + columns) / columns;
        if (rows < 1) {
            return Collections.emptyMap();
        }
        int cellWidth = (size.getWidth() + spacingX) / columns - spacingX;
        int cellHeight = (size.getHeight() + spacingY) / rows - spacingY;

        Pair<int[], int[]> maxCellSize = null;

        if (!cellsEqualSize) {
            maxCellSize = calcNeededCellSize(container);
        }

        Map<GuiElement, Pair<ReadablePoint, ReadableDimension>> map = new LinkedHashMap<>();
        Iterator<Map.Entry<GuiElement, com.replaymod.gui.layout.LayoutData>> iter = container.getElements().entrySet().iterator();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (!iter.hasNext()) {
                    return map;
                }
                int x = j * (cellWidth + spacingX);
                int y = i * (cellHeight + spacingY);

                if (maxCellSize != null) {
                    cellWidth = maxCellSize.getLeft()[j];
                    cellHeight = maxCellSize.getRight()[i];

                    x = 0;
                    for (int x1 = 0; x1 < j; x1++) {
                        x += maxCellSize.getLeft()[x1];
                        x += spacingX;
                    }

                    y = 0;
                    for (int y1 = 0; y1 < i; y1++) {
                        y += maxCellSize.getRight()[y1];
                        y += spacingY;
                    }
                }

                Map.Entry<GuiElement, com.replaymod.gui.layout.LayoutData> entry = iter.next();
                GuiElement element = entry.getKey();
                Data data = entry.getValue() instanceof Data ? (Data) entry.getValue() : DEFAULT_DATA;
                Dimension elementSize = new Dimension(element.getMinSize());
                ReadableDimension elementMaxSize = element.getMaxSize();
                elementSize.setWidth(Math.min(cellWidth, elementMaxSize.getWidth()));
                elementSize.setHeight(Math.min(cellHeight, elementMaxSize.getHeight()));

                int remainingWidth = cellWidth - elementSize.getWidth();
                int remainingHeight = cellHeight - elementSize.getHeight();
                x += (int) (data.alignmentX * remainingWidth);
                y += (int) (data.alignmentY * remainingHeight);
                map.put(element, Pair.<ReadablePoint, ReadableDimension>of(new Point(x, y), elementSize));
            }
        }
        return map;
    }

    @Override
    public ReadableDimension calcMinSize(GuiContainer<?> container) {
        Preconditions.checkState(columns != 0, "Columns may not be 0.");
        int maxWidth = 0, maxHeight = 0;

        int elements = 0;
        for (Map.Entry<GuiElement, com.replaymod.gui.layout.LayoutData> entry : container.getElements().entrySet()) {
            GuiElement element = entry.getKey();
            ReadableDimension minSize = element.getMinSize();

            int width = minSize.getWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }

            int height = minSize.getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
            elements++;
        }
        int rows = (elements - 1 + columns) / columns;

        int totalWidth = maxWidth * columns;
        int totalHeight = maxHeight * rows;

        if (!cellsEqualSize) {
            Pair<int[], int[]> maxCellSize = calcNeededCellSize(container);

            totalWidth = 0;
            for (int w : maxCellSize.getLeft()) {
                totalWidth += w;
            }

            totalHeight = 0;
            for (int h : maxCellSize.getRight()) {
                totalHeight += h;
            }
        }

        if (elements > 0) {
            totalWidth += spacingX * (columns - 1);
        }
        if (elements > columns) {
            totalHeight += spacingY * (rows - 1);
        }
        return new Dimension(totalWidth, totalHeight);
    }

    private Pair<int[], int[]> calcNeededCellSize(GuiContainer<?> container) {
        int[] columnMaxWidth = new int[columns];
        int[] rowMaxHeight = new int[(container.getElements().size() - 1 + columns) / columns];

        int elements = 0;
        for (Map.Entry<GuiElement, com.replaymod.gui.layout.LayoutData> entry : container.getElements().entrySet()) {
            int column = elements % columns;
            int row = elements / columns;

            GuiElement element = entry.getKey();
            ReadableDimension minSize = element.getMinSize();

            int width = minSize.getWidth();
            if (width > columnMaxWidth[column]) {
                columnMaxWidth[column] = width;
            }

            int height = minSize.getHeight();
            if (height > rowMaxHeight[row]) {
                rowMaxHeight[row] = height;
            }

            elements++;
        }

        return Pair.of(columnMaxWidth, rowMaxHeight);
    }

    public int getColumns() {
        return this.columns;
    }

    public GridLayout setColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public int getSpacingX() {
        return this.spacingX;
    }

    public GridLayout setSpacingX(int spacingX) {
        this.spacingX = spacingX;
        return this;
    }

    public int getSpacingY() {
        return this.spacingY;
    }

    public GridLayout setSpacingY(int spacingY) {
        this.spacingY = spacingY;
        return this;
    }

    public boolean isCellsEqualSize() {
        return this.cellsEqualSize;
    }

    public GridLayout setCellsEqualSize(boolean cellsEqualSize) {
        this.cellsEqualSize = cellsEqualSize;
        return this;
    }

    public static class Data implements LayoutData {
        private double alignmentX, alignmentY;

        public Data() {
            this(0, 0);
        }

        public Data(double alignmentX, double alignmentY) {
            this.alignmentX = alignmentX;
            this.alignmentY = alignmentY;
        }

        public double getAlignmentX() {
            return this.alignmentX;
        }

        public void setAlignmentX(double alignmentX) {
            this.alignmentX = alignmentX;
        }

        public double getAlignmentY() {
            return this.alignmentY;
        }

        public void setAlignmentY(double alignmentY) {
            this.alignmentY = alignmentY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return Double.compare(data.alignmentX, alignmentX) == 0 &&
                    Double.compare(data.alignmentY, alignmentY) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(alignmentX, alignmentY);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "alignmentX=" + alignmentX +
                    ", alignmentY=" + alignmentY +
                    '}';
        }
    }
}
