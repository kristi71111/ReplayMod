package com.replaymod.simplepathing.preview;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderWorldCallback;
import com.replaymod.core.versions.MCVer;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.gui.GuiPathing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.Optional;

import static com.replaymod.core.ReplayMod.TEXTURE;

public class PathPreviewRenderer extends EventRegistrations {
    private static final ResourceLocation CAMERA_HEAD = new ResourceLocation("replaymod", "camera_head.png");
    private static final Minecraft mc = MCVer.getMinecraft();

    private static final int SLOW_PATH_COLOR = 0xffcccc;
    private static final int FAST_PATH_COLOR = 0x660000;
    private static final double FASTEST_PATH_SPEED = 0.01;

    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;

    public PathPreviewRenderer(ReplayModSimplePathing mod, ReplayHandler replayHandler) {
        this.mod = mod;
        this.replayHandler = replayHandler;
    }

    {
        on(PostRenderWorldCallback.EVENT, this::renderCameraPath);
    }

    private void renderCameraPath(MatrixStack matrixStack) {
        if (!replayHandler.getReplaySender().isAsyncMode() || mc.gameSettings.hideGUI) return;

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        GuiPathing guiPathing = mod.getGuiPathing();
        if (guiPathing == null) return;
        EntityPositionTracker entityTracker = guiPathing.getEntityTracker();

        SPTimeline timeline = mod.getCurrentTimeline();
        if (timeline == null) return;
        Path path = timeline.getPositionPath();
        if (path.getKeyframes().isEmpty()) return;
        Path timePath = timeline.getTimePath();

        path.update();

        int renderDistance = mc.gameSettings.renderDistanceChunks * 16;
        int renderDistanceSquared = renderDistance * renderDistance;

        Triple<Double, Double, Double> viewPos = Triple.of(
                view.getPosX(),
                view.getPosY()
                ,
                view.getPosZ()
        );

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            RenderSystem.multMatrix(matrixStack.getLast().getMatrix());

            for (PathSegment segment : path.getSegments()) {
                Interpolator interpolator = segment.getInterpolator();
                Keyframe start = segment.getStartKeyframe();
                Keyframe end = segment.getEndKeyframe();
                long diff = (int) (end.getTime() - start.getTime());

                boolean spectator = interpolator.getKeyframeProperties().contains(SpectatorProperty.PROPERTY);
                if (spectator && entityTracker == null) {
                    continue; // Cannot render spectator positions when entity tracker is not yet loaded
                }
                // Spectator segments have 20 lines per second (at least 10) whereas normal segments have a fixed 100
                long steps = spectator ? Math.max(diff / 50, 10) : 100;
                Triple<Double, Double, Double> prevPos = null;
                for (int i = 0; i <= steps; i++) {
                    long time = start.getTime() + diff * i / steps;
                    if (spectator) {
                        Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
                        Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                        if (entityId.isPresent() && replayTime.isPresent()) {
                            Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                            if (loc != null) {
                                Triple<Double, Double, Double> pos = Triple.of(loc.getX(), loc.getY(), loc.getZ());
                                if (prevPos != null) {
                                    drawConnection(viewPos, prevPos, pos, 0x0000ff, renderDistanceSquared);
                                }
                                prevPos = pos;
                                continue;
                            }
                        }
                    } else {
                        Optional<Triple<Double, Double, Double>> optPos = path.getValue(CameraProperties.POSITION, time);
                        if (optPos.isPresent()) {
                            Triple<Double, Double, Double> pos = optPos.get();
                            if (prevPos != null) {
                                double distance = Math.sqrt(distanceSquared(prevPos, pos));
                                double speed = Math.min(distance / (diff / steps), FASTEST_PATH_SPEED);
                                double speedFraction = speed / FASTEST_PATH_SPEED;
                                int color = interpolateColor(SLOW_PATH_COLOR, FAST_PATH_COLOR, speedFraction);
                                drawConnection(viewPos, prevPos, pos, color, renderDistanceSquared);
                            }
                            prevPos = pos;
                            continue;
                        }
                    }
                    prevPos = null;
                }
            }

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            path.getKeyframes().stream()
                    .map(k -> Pair.of(k, k.getValue(CameraProperties.POSITION)))
                    .filter(p -> p.getRight().isPresent())
                    .map(p -> Pair.of(p.getLeft(), p.getRight().get()))
                    .filter(p -> distanceSquared(p.getRight(), viewPos) < renderDistanceSquared)
                    .sorted(new KeyframeComparator(viewPos)) // Need to render the furthest first
                    .forEachOrdered(p -> drawPoint(viewPos, p.getRight(), p.getLeft()));

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            int time = guiPathing.timeline.getCursorPosition();
            Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
            if (entityId.isPresent()) {
                // Spectating an entity
                if (entityTracker != null) {
                    Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                    if (replayTime.isPresent()) {
                        Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                        if (loc != null) {
                            drawCamera(viewPos,
                                    Triple.of(loc.getX(), loc.getY(), loc.getZ()),
                                    Triple.of(loc.getYaw(), loc.getPitch(), 0f));
                        }
                    }
                }
            } else {
                // Normal camera path
                Optional<Triple<Double, Double, Double>> cameraPos = path.getValue(CameraProperties.POSITION, time);
                Optional<Triple<Float, Float, Float>> cameraRot = path.getValue(CameraProperties.ROTATION, time);
                if (cameraPos.isPresent() && cameraRot.isPresent()) {
                    drawCamera(viewPos, cameraPos.get(), cameraRot.get());
                }
            }
        } finally {
            GL11.glPopMatrix();
            GlStateManager.popAttributes();
        }
    }

    private static int interpolateColor(int c1, int c2, double weight) {
        return (interpolateColorComponent((c1 >> 16) & 0xff, (c2 >> 16) & 0xff, weight) << 16)
                | (interpolateColorComponent((c1 >> 8) & 0xff, (c2 >> 8) & 0xff, weight) << 8)
                | interpolateColorComponent(c1 & 0xff, c2 & 0xff, weight);
    }

    private static int interpolateColorComponent(int c1, int c2, double weight) {
        return (int) (c1 + (1 - Math.pow(Math.E, -4 * weight)) * (c2 - c1)) & 0xff;
    }

    private static double distanceSquared(Triple<Double, Double, Double> p1, Triple<Double, Double, Double> p2) {
        double dx = p1.getLeft() - p2.getLeft();
        double dy = p1.getMiddle() - p2.getMiddle();
        double dz = p1.getRight() - p2.getRight();
        return dx * dx + dy * dy + dz * dz;
    }

    private void drawConnection(Triple<Double, Double, Double> view,
                                Triple<Double, Double, Double> pos1,
                                Triple<Double, Double, Double> pos2,
                                int color, int renderDistanceSquared) {
        if (distanceSquared(view, pos1) > renderDistanceSquared) return;
        if (distanceSquared(view, pos2) > renderDistanceSquared) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(
                pos1.getLeft() - view.getLeft(),
                pos1.getMiddle() - view.getMiddle(),
                pos1.getRight() - view.getRight()
        ).color(
                color >> 16 & 0xff,
                color >> 8 & 0xff,
                color & 0xff,
                255
        ).endVertex();
        buffer.pos(
                pos2.getLeft() - view.getLeft(),
                pos2.getMiddle() - view.getMiddle(),
                pos2.getRight() - view.getRight()
        ).color(
                color >> 16 & 0xff,
                color >> 8 & 0xff,
                color & 0xff,
                255
        ).endVertex();

        GL11.glLineWidth(3);
        tessellator.draw();
    }

    private void drawPoint(Triple<Double, Double, Double> view,
                           Triple<Double, Double, Double> pos,
                           Keyframe keyframe) {
        mc.getTextureManager().bindTexture(TEXTURE);

        float posX = 80f / ReplayMod.TEXTURE_SIZE;
        float posY = 0f;
        float size = 10f / ReplayMod.TEXTURE_SIZE;

        if (mod.isSelected(keyframe)) {
            posY += size;
        }

        if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
            posX += size;
        }

        float minX = -0.5f;
        float minY = -0.5f;
        float maxX = 0.5f;
        float maxY = 0.5f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(minX, minY, 0).tex(posX + size, posY + size).endVertex();
        buffer.pos(minX, maxY, 0).tex(posX + size, posY).endVertex();
        buffer.pos(maxX, maxY, 0).tex(posX, posY).endVertex();
        buffer.pos(maxX, minY, 0).tex(posX, posY + size).endVertex();

        GL11.glPushMatrix();

        GL11.glTranslated(
                pos.getLeft() - view.getLeft(),
                pos.getMiddle() - view.getMiddle(),
                pos.getRight() - view.getRight()
        );
        GL11.glNormal3f(0, 1, 0);
        GL11.glRotatef(-mc.getRenderManager().info.getYaw(), 0, 1, 0);
        GL11.glRotatef(mc.getRenderManager().info.getPitch(), 1, 0, 0);

        tessellator.draw();

        GL11.glPopMatrix();
    }

    private void drawCamera(Triple<Double, Double, Double> view,
                            Triple<Double, Double, Double> pos,
                            Triple<Float, Float, Float> rot) {

        mc.getTextureManager().bindTexture(CAMERA_HEAD);

        GL11.glPushMatrix();

        GL11.glTranslated(
                pos.getLeft() - view.getLeft(),
                pos.getMiddle() - view.getMiddle(),
                pos.getRight() - view.getRight()
        );
        GL11.glRotated(-rot.getLeft(), 0, 1, 0); // Yaw
        GL11.glRotated(rot.getMiddle(), 1, 0, 0); // Pitch
        GL11.glRotated(rot.getRight(), 0, 0, 1); // Roll
        GL11.glNormal3f(0, 1, 0);

        //draw the position line
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(0, 0, 0).color(0, 255, 0, 170).endVertex();
        buffer.pos(0, 0, 2).color(0, 255, 0, 170).endVertex();

        tessellator.draw();

        // draw camera cube
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        float cubeSize = 0.5f;

        double r = -cubeSize / 2;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        //back
        buffer.pos(r, r + cubeSize, r).tex(3 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r + cubeSize, r).tex(4 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r, r).tex(4 * 8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r).tex(3 * 8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();

        //front
        buffer.pos(r + cubeSize, r, r + cubeSize).tex(2 * 8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r + cubeSize, r + cubeSize).tex(2 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r + cubeSize, r + cubeSize).tex(8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r + cubeSize).tex(8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();

        //left
        buffer.pos(r + cubeSize, r + cubeSize, r).tex(0, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r + cubeSize, r + cubeSize).tex(8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r, r + cubeSize).tex(8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r, r).tex(0, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();

        //right
        buffer.pos(r, r + cubeSize, r + cubeSize).tex(2 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r + cubeSize, r).tex(3 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r).tex(3 * 8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r + cubeSize).tex(2 * 8 / 64f, 2 * 8 / 64f).color(255, 255, 255, 200).endVertex();

        //bottom
        buffer.pos(r + cubeSize, r, r).tex(3 * 8 / 64f, 0).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r, r + cubeSize).tex(3 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r + cubeSize).tex(2 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r, r).tex(2 * 8 / 64f, 0).color(255, 255, 255, 200).endVertex();

        //top
        buffer.pos(r, r + cubeSize, r).tex(8 / 64f, 0).color(255, 255, 255, 200).endVertex();
        buffer.pos(r, r + cubeSize, r + cubeSize).tex(8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r + cubeSize, r + cubeSize).tex(2 * 8 / 64f, 8 / 64f).color(255, 255, 255, 200).endVertex();
        buffer.pos(r + cubeSize, r + cubeSize, r).tex(2 * 8 / 64f, 0).color(255, 255, 255, 200).endVertex();

        tessellator.draw();

        GL11.glPopMatrix();
    }

    private class KeyframeComparator implements Comparator<Pair<Keyframe, Triple<Double, Double, Double>>> {
        private final Triple<Double, Double, Double> viewPos;

        public KeyframeComparator(Triple<Double, Double, Double> viewPos) {
            this.viewPos = viewPos;
        }

        @Override
        public int compare(Pair<Keyframe, Triple<Double, Double, Double>> o1,
                           Pair<Keyframe, Triple<Double, Double, Double>> o2) {
            return -Double.compare(distanceSquared(o1.getRight(), viewPos), distanceSquared(o2.getRight(), viewPos));
        }
    }
}
