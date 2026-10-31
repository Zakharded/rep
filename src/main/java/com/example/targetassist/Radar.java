package com.example.targetassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class Radar {
    public static final int SIZE = 90;  // diameter in GUI pixels
    private static final int MARGIN = 6;

    public static void render(GuiGraphics g, Minecraft mc, int screenW) {
        if (!Settings.radarOn || mc.player == null || mc.level == null) return;
        Player me = mc.player;
        int r = SIZE / 2;
        int cx = screenW - MARGIN - r, cy = MARGIN + r;
        double range = Settings.radarRange;

        // background, faint cross, north marker
        circle(g, cx, cy, r, 0xFF3A424C);
        circle(g, cx, cy, r - 1, 0xB0101418);
        g.fill(cx - r + 3, cy, cx + r - 2, cy + 1, 0x30FFFFFF);
        g.fill(cx, cy - r + 3, cx + 1, cy + r - 2, 0x30FFFFFF);
        g.drawCenteredString(mc.font, "N", cx, cy - r + 3, 0xFFF0B429);
        g.drawString(mc.font, Math.round(range) + "m", cx - r + 6, cy + r - 12, 0xFF8B96A3, false);

        // dots (north is always up, east is right)
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == me || !(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator()) continue;
            int color;
            if (e instanceof Player) {
                if (!Settings.radarPlayers) continue;
                color = e.isInvisible() ? 0xFFFF55FF : 0xFFFF5555;  // invisible players are magenta
            } else if (e instanceof Enemy) {
                if (!Settings.radarHostile) continue;
                color = 0xFFFFAA00;
            } else if (e instanceof Mob) {
                if (!Settings.radarPassive) continue;
                color = 0xFF55FF55;
            } else {
                continue;
            }
            double dx = e.getX() - me.getX(), dz = e.getZ() - me.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > range) continue;
            int dotX = cx + (int) Math.round(dx / range * (r - 3));
            int dotY = cy + (int) Math.round(dz / range * (r - 3));
            int s = e instanceof Player ? 2 : 1;
            g.fill(dotX - s, dotY - s, dotX + s + 1, dotY + s + 1, color);
        }

        // you, plus a short line showing where you are facing
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        double yaw = Math.toRadians(me.getYRot());
        for (int i = 1; i <= 4; i++) {
            int fx = cx + (int) Math.round(-Math.sin(yaw) * i * 3);
            int fy = cy + (int) Math.round(Math.cos(yaw) * i * 3);
            g.fill(fx, fy, fx + 1, fy + 1, 0xFFFFFFFF);
        }
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
