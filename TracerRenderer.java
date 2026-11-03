package pl.playertracker;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TracerRenderer {

    private TracerRenderer() {}

    public static AbstractClientPlayerEntity find(MinecraftClient mc, String name) {
        if (mc.world == null) return null;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers())
            if (p.getGameProfile().getName().equalsIgnoreCase(name) && p != mc.player) return p;
        return null;
    }

    /* ---------------- linie w świecie ---------------- */

    public static void render(WorldRenderContext ctx) {
        TrackerState.Config c = TrackerState.C;
        if (!c.enabled || c.tracked.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        float td = ctx.tickCounter().getTickDelta(true);
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).normalize();
        Vec3d start = look.multiply(1.0);
        if (c.origin == 1) start = start.subtract(new Vec3d(camera.getVerticalPlane()).multiply(0.5));
        Matrix4f mat = ctx.matrixStack().peek().getPositionMatrix();

        int alpha = (int) (255 * (TrackerState.OPACITY[c.opacityIdx] / 100f));
        int limit = TrackerState.MAX_DIST[c.maxDistIdx];

        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean any = false;

        for (Map.Entry<String, TrackerState.Entry> en : c.tracked.entrySet()) {
            if (!en.getValue().visible) continue;
            AbstractClientPlayerEntity t = find(mc, en.getKey());
            if (t == null) continue;
            if (limit > 0 && t.distanceTo(mc.player) > limit) continue;

            int rgb = TrackerState.rgb(en.getKey());
            int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255;

            Vec3d lerped = t.getLerpedPos(td);
            Vec3d center = lerped.add(0, t.getHeight() / 2.0, 0).subtract(cam);

            if (c.style != 2) line(b, mat, start, center, r, g, bl, alpha);
            if (c.style != 1) {
                Vec3d off = lerped.subtract(t.getPos());
                Box box = t.getBoundingBox().offset(off).offset(-cam.x, -cam.y, -cam.z);
                box(b, mat, box, r, g, bl, alpha);
            }
            any = true;
        }
        if (!any) return;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(c.thickness);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (c.throughWalls) RenderSystem.disableDepthTest();
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1f);
    }

    private static void line(BufferBuilder b, Matrix4f m, Vec3d a, Vec3d c, int r, int g, int bl, int al) {
        b.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, al);
        b.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, al);
    }

    private static void box(BufferBuilder b, Matrix4f m, Box x, int r, int g, int bl, int al) {
        float x1 = (float) x.minX, y1 = (float) x.minY, z1 = (float) x.minZ;
        float x2 = (float) x.maxX, y2 = (float) x.maxY, z2 = (float) x.maxZ;
        float[][] e = {
                {x1,y1,z1,x2,y1,z1},{x2,y1,z1,x2,y1,z2},{x2,y1,z2,x1,y1,z2},{x1,y1,z2,x1,y1,z1},
                {x1,y2,z1,x2,y2,z1},{x2,y2,z1,x2,y2,z2},{x2,y2,z2,x1,y2,z2},{x1,y2,z2,x1,y2,z1},
                {x1,y1,z1,x1,y2,z1},{x2,y1,z1,x2,y2,z1},{x2,y1,z2,x2,y2,z2},{x1,y1,z2,x1,y2,z2}};
        for (float[] l : e) {
            b.vertex(m, l[0], l[1], l[2]).color(r, g, bl, al);
            b.vertex(m, l[3], l[4], l[5]).color(r, g, bl, al);
        }
    }

    /* ---------------- HUD ---------------- */

    public static void renderHud(DrawContext ctx, RenderTickCounter tick) {
        TrackerState.Config c = TrackerState.C;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.hud || c.tracked.isEmpty() || mc.player == null
                || mc.options.hudHidden || mc.currentScreen != null) return;

        TextRenderer tr = mc.textRenderer;
        List<Text> lines = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        lines.add(Text.literal("§b§lPLAYER TRACKER " + (c.enabled ? "§a●" : "§c●")));
        cols.add(0xFFFFFF);

        for (Map.Entry<String, TrackerState.Entry> en : c.tracked.entrySet()) {
            String name = en.getKey();
            AbstractClientPlayerEntity t = find(mc, name);
            StringBuilder sb = new StringBuilder("● " + name);
            if (!en.getValue().visible) sb.append(" §8(ukryty)");
            if (t != null) {
                if (c.hudDistance) sb.append("  §7").append((int) t.distanceTo(mc.player)).append("m");
                if (c.hudHealth) sb.append("  §c").append((int) Math.ceil(t.getHealth())).append("HP");
            } else if (TrackerState.findEntry(name) != null) sb.append("  §8poza zasięgiem");
            else sb.append("  §8offline");
            lines.add(Text.literal(sb.toString()));
            cols.add(TrackerState.rgb(name));
        }

        int w = 0;
        for (Text t : lines) w = Math.max(w, tr.getWidth(t));
        w += 12;
        int h = lines.size() * 10 + 6;
        boolean right = c.hudCorner == 1 || c.hudCorner == 3;
        boolean bottom = c.hudCorner >= 2;
        int x = right ? ctx.getScaledWindowWidth() - w - 4 : 4;
        int y = bottom ? ctx.getScaledWindowHeight() - h - 4 : 4;

        ctx.fill(x, y, x + w, y + h, 0x90000000);
        ctx.fill(x, y, x + 2, y + h, 0xFF00B7FF);
        for (int i = 0; i < lines.size(); i++)
            ctx.drawTextWithShadow(tr, lines.get(i), x + 7, y + 4 + i * 10, cols.get(i));
    }
}
